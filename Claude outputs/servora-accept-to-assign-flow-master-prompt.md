# MASTER PROMPT 15: Booking is "Confirmed" first, and becomes "Professional Assigned" only when the partner accepts (Servora / ServiceAssist)

Paste everything below the line into your coding agent. Work in the repo root `ServiceAssist/`. These are applied and must keep working:
- the partner "Accept Job" → Job screen → "Cancel Job" flow (`servora-partner-accept-job-flow-master-prompt.md`)
- chat v9, payments, red-dot badges, fixed header

---

## 0. ROLE AND WORKING RULES

You are a staff-level engineer (Android Kotlin/Compose/Room + Supabase Postgres/Deno Edge Functions) with marketplace experience.

1. Read every file named in section 2 first. If the code differs, trust the code, say so in one line, and adapt.
2. Post an 8-line plan, then do every phase in order without stopping. Report once at the end.
3. Don't ask questions unless you are blocked.
4. **The server is the source of truth for the booking lifecycle.** No client may downgrade a status or erase `accepted_at`.
5. Every new color comes from `ServoraTheme.colors` / `MaterialTheme.colorScheme`. Check light and dark.
6. **No tests.** The only checks are `./gradlew assembleDebug` after each Android phase, `deno check` on changed functions, and the manual checklist.
7. Never log phone numbers, addresses or OTPs.

## 1. THE NEW FLOW (what I want)

| Step | Who | Booking status | Customer sees | Partner sees |
|---|---|---|---|---|
| 1. Customer books | customer | **`CONFIRMED`**, `accepted_at = null`, `professional_id` = the offered partner | "Booking Confirmed ✅" notification. Tracker: *Booking Placed ✔ → Waiting for a professional to accept…*. **No professional card, no Call/Message.** | "New job request" notification + the job under **New Jobs** with a green **Accept Job** button |
| 2. Partner taps Accept Job | partner | **`ASSIGNED`** + `accepted_at = now()` (one atomic server update) | **"Professional Assigned 👨‍🔧 — <Name> will handle your <service>"** notification. The professional card, Call/Message and the chat unlock. Tracker: *Professional Assigned ✔* | The Job screen opens (existing) |
| 3+. On the way → Arrived → OTP → Started → Payment → Completed | partner | unchanged | unchanged (a notification per step) | unchanged |
| Customer cancels before acceptance | customer | `CANCELLED` | existing cancel UI | the job disappears from New Jobs |

Out of scope: auto-reassignment or timeouts when nobody accepts, and a "Decline" button. The customer can still cancel for free while it's waiting.

## 2. CURRENT STATE (verified in the repo) and the problems

1. **The customer is told "Professional Assigned" immediately.**
   - `ServoraViewModel.createBooking` (~line 613) creates `status = BookingStatus.ASSIGNED`.
   - `booking-create` defaults to `status: body.status || "ASSIGNED"`.
   - `Booking.status` defaults to `ASSIGNED` (`Models.kt:67`), and `SupabaseModels` falls back to `ASSIGNED` for unknown statuses (~line 129).
   - `booking-accept` only sets `accepted_at` and **keeps `status = ASSIGNED`**, so the customer never sees a change when the partner accepts.
2. **Notifications are local and in-memory only.**
   - `ServoraViewModel.addNotification` prepends to `_notifications` (a `MutableStateFlow`).
   - The customer's "accepted" notification is posted **on the partner's device** (`acceptJob`, ~line 829). Every "On the way / Arrived / Started" notification in `advanceBookingStatus` is too.
   - The partner's "New Duty Allocated" is posted **on the customer's device** (`createBooking`).
   - On two real devices, **the customer never gets "partner assigned"** and the partner never gets "new job".
3. **Clients can overwrite the lifecycle.**
   - `booking-create` does `upsert(..., onConflict: "booking_code")` with the client's `status`.
   - `SupabaseSyncManager` re-pushes `pendingSync` bookings through `createBooking`, with `insertBooking` REST as a fallback.
   - A stale customer device can push `CONFIRMED`/`ASSIGNED` back over an accepted or started booking.
4. **The partner can skip acceptance.**
   - `ServoraViewModel.advanceBookingStatus` maps `CONFIRMED → ASSIGNED` and `ASSIGNED → ON_THE_WAY`.
   - `booking-update-status` doesn't check `accepted_at`.
   - `PartnerBookingsScreen` has its own advance button.
5. **Chat opens before acceptance.** `chat-open` only blocks `status === "PENDING"`.
6. **Partner "needs acceptance" logic only knows `ASSIGNED && acceptedAt == null`** (`PartnerJobsScreen.kt:106/114`, `PartnerJobDetailScreen.kt:222/422`). A `CONFIRMED` job would wrongly show as "Ongoing".
7. **Customer tracking polls every 12 s only while the tracking screen is open** (`MainActivity.kt:~905`). The global sync runs every 60 s (`ServoraViewModel.startLiveSyncLoop`).

## 3. DESIGN RULES (locked)

- **Awaiting acceptance** ⇔ `status == CONFIRMED`. As a legacy fallback, `ASSIGNED && acceptedAt == null` counts the same. Add a single helper in `Models.kt`, and use it **everywhere**; no ad-hoc checks:
  ```kotlin
  val Booking.isAwaitingPartnerAcceptance: Boolean
      get() = status == BookingStatus.CONFIRMED || (status == BookingStatus.ASSIGNED && acceptedAt == null)
  val Booking.isPartnerAssigned: Boolean
      get() = !isAwaitingPartnerAcceptance && status != BookingStatus.CANCELLED && status != BookingStatus.PENDING
  ```
- Status order for "no downgrade": `PENDING 0 < CONFIRMED 1 < ASSIGNED 2 < ON_THE_WAY 3 < ARRIVED 4 < STARTED 5 < AWAITING_PAYMENT 6 < COMPLETED 7`. `CANCELLED` is terminal and reachable from any non-terminal status. `COMPLETED` is terminal.
- **Only `booking-accept` can move `CONFIRMED → ASSIGNED` for a partner.** Admin may force it through `booking-update-status`, which then also sets `accepted_at`.

## 4. THE WORK

### Phase 1: Supabase migration `supabase_accept_to_assign_migration.sql` (idempotent, one transaction)
1. **Backfill** the old model: `UPDATE public.bookings SET status = 'CONFIRMED' WHERE status = 'ASSIGNED' AND accepted_at IS NULL;`
2. `ALTER TABLE public.bookings ALTER COLUMN status SET DEFAULT 'CONFIRMED';`
3. **Lifecycle guard trigger** (protects against every client path, including the anon REST fallback):
   ```sql
   CREATE OR REPLACE FUNCTION public.booking_status_rank(s TEXT) RETURNS INT LANGUAGE sql IMMUTABLE AS $$
     SELECT CASE s WHEN 'PENDING' THEN 0 WHEN 'CONFIRMED' THEN 1 WHEN 'ASSIGNED' THEN 2 WHEN 'ON_THE_WAY' THEN 3
                   WHEN 'ARRIVED' THEN 4 WHEN 'STARTED' THEN 5 WHEN 'AWAITING_PAYMENT' THEN 6 WHEN 'COMPLETED' THEN 7
                   WHEN 'CANCELLED' THEN 99 ELSE -1 END $$;

   CREATE OR REPLACE FUNCTION public.trg_bookings_guard_lifecycle() RETURNS TRIGGER
   LANGUAGE plpgsql SET search_path = public AS $f$
   BEGIN
     -- never erase acceptance
     IF OLD.accepted_at IS NOT NULL AND NEW.accepted_at IS NULL THEN NEW.accepted_at := OLD.accepted_at; END IF;
     -- terminal states stay terminal
     IF OLD.status IN ('COMPLETED','CANCELLED') AND NEW.status IS DISTINCT FROM OLD.status THEN
       NEW.status := OLD.status;
     -- no downgrades (cancel is always allowed from non-terminal)
     ELSIF NEW.status <> 'CANCELLED' AND booking_status_rank(NEW.status) < booking_status_rank(OLD.status) THEN
       NEW.status := OLD.status;
     END IF;
     -- work can't start before acceptance
     IF NEW.accepted_at IS NULL AND booking_status_rank(NEW.status) BETWEEN 3 AND 7 THEN
       RAISE EXCEPTION 'Job must be accepted by the professional first' USING ERRCODE = 'P0001';
     END IF;
     -- ASSIGNED always carries accepted_at
     IF NEW.status = 'ASSIGNED' AND NEW.accepted_at IS NULL THEN NEW.accepted_at := now(); END IF;
     RETURN NEW;
   END $f$;

   DROP TRIGGER IF EXISTS trg_bookings_guard_lifecycle ON public.bookings;
   CREATE TRIGGER trg_bookings_guard_lifecycle BEFORE UPDATE ON public.bookings
   FOR EACH ROW EXECUTE FUNCTION public.trg_bookings_guard_lifecycle();
   ```
   - Name it so it fires **before** `enforce_payment_before_completion` if ordering matters. Postgres fires same-timing triggers alphabetically; state the order you get.
   - Keep the per-booking chat trigger from v9 as is.
4. Report `select status, count(*), count(accepted_at) from bookings group by 1;` before and after.

### Phase 2: Edge Functions
1. **`booking-create`:**
   - For a **new** booking, force `status = 'CONFIRMED'` and `accepted_at = null`, and ignore the client's `status` / `accepted_at` / `cancelled_*`.
   - Replace the blind `upsert` with: `insert`; on `23505` (the `booking_code` exists), return the **existing row unchanged**. Also allow updating only harmless fields (`special_notes`) if you must. **Never** overwrite `status`, `professional_id`, `accepted_at`, `is_paid` or the cancel fields from this endpoint.
2. **`booking-accept`:** make acceptance change the status atomically:
   ```ts
   const nowIso = new Date().toISOString();
   const { data: rows, error } = await supabaseClient.from("bookings")
     .update({ status: "ASSIGNED", accepted_at: nowIso })
     .eq("id", booking.id)
     .in("status", ["CONFIRMED", "ASSIGNED"])
     .is("accepted_at", null)
     .select("id, status, accepted_at, professional_id");
   ```
   - 1 row → 200 `{ success, booking_id, status: "ASSIGNED", accepted_at }`.
   - 0 rows → re-read the booking:
     - already accepted (`accepted_at` set, status not cancelled) → **200 idempotent**, with the existing values
     - `CANCELLED` → 409 `"The customer cancelled this booking"`
     - `COMPLETED` → 409 `"This job is no longer available"`
   - Keep the role and `professional_id` checks. Tighten them: when `identity.isProd`, the partner **must** equal `professional_id`.
   - Call `recomputeBookingChatStatus` after success.
3. **`booking-update-status`:**
   - If the caller is a PARTNER and the target is `ASSIGNED`, return 409 `"Use Accept Job to accept this booking"`.
   - If the booking is awaiting acceptance (`status = CONFIRMED`, or `accepted_at` null) and the target is `ON_THE_WAY`..`COMPLETED`, return 409 `"Accept the job first"`.
   - Map the trigger's `P0001` to the same 409 sentence in `jsonError`, or locally.
   - ADMIN may set `ASSIGNED` (the trigger fills `accepted_at`).
4. **`chat-open`:** in addition to `PENDING`, reject when `status === "CONFIRMED"` or `accepted_at` is null with 409 `"Chat opens once a professional accepts your booking."`. Add `accepted_at` to its `select`.
5. `deno check` all four. Deploy: `supabase functions deploy booking-create booking-accept booking-update-status chat-open`.

### Phase 3: Android data layer
1. `Models.kt`:
   - `Booking.status` defaults to `BookingStatus.CONFIRMED`.
   - Add the two extension properties from section 3.
   - Change `BookingStatus.CONFIRMED.label` to `"Booking Confirmed"` (already) and `ASSIGNED.label` to `"Professional Assigned"` (already). No change is needed; just confirm.
2. `SupabaseModels`: fall back to `CONFIRMED` (not `ASSIGNED`) for unknown statuses.
3. `ServoraDatabase`: go to `version = 8` with an explicit `MIGRATION_7_8`:
   `UPDATE bookings SET status = 'CONFIRMED' WHERE status = 'ASSIGNED' AND acceptedAt IS NULL`
   - Check how `status` is stored (enum name) first.
   - Register it in `addMigrations`. No destructive fallback.
4. `ServoraViewModel.createBooking`: `status = BookingStatus.CONFIRMED`.
5. `ServoraRepository.acceptBooking`: on success, write **both** `status = ASSIGNED` and `acceptedAt` to Room, with the values returned by the server. Add `BookingDao.markAccepted(id, acceptedAt)` that updates both columns.
6. `SupabaseSyncManager`:
   - On pull, the server status wins unless the local row is `pendingSync`. Even then, **never keep a local status whose rank is lower than the server's**, and never clear `acceptedAt`.
   - On push, `toSupabaseDto()` for a re-push must not send a lower status. The server now ignores it anyway.
7. `advanceBookingStatus`:
   - Remove the `PENDING → CONFIRMED` and `CONFIRMED → ASSIGNED` branches (return early with no-op).
   - If `booking.isAwaitingPartnerAcceptance`, emit `"Accept the job first"` to `_statusUpdateError` and return.
8. Build.

### Phase 4: Notifications that reach the right person (`ServoraViewModel`)
Replace "post the other party's notification from the actor's device" with **observer-based notifications generated on the receiving side**:
1. Add a `BookingEventNotifier` (a private section of `ServoraViewModel` is fine):
   - It keeps `lastSeen: MutableMap<Long, Pair<BookingStatus, Long?>>` (status, acceptedAt), seeded from the first emission **without** notifying.
   - It collects `allBookings` (Room flow) and diffs against `lastSeen` for bookings relevant to the current user:
     - **Customer** (`booking.customerId == currentUser.id`):
       - `isAwaitingPartnerAcceptance` → `isPartnerAssigned`: **"Professional Assigned 👨‍🔧"**, "<Pro name> accepted your <service> booking #<code>. You can now chat with them." `type = STATUS_UPDATE`, `targetRole = CUSTOMER`, with `bookingId`.
       - → `ON_THE_WAY` / `ARRIVED` (include the OTP as today) / `STARTED` / `AWAITING_PAYMENT` / `COMPLETED`: move the existing customer texts here.
       - → `CANCELLED` with `cancelledBy == "PARTNER"`: the existing partner-cancel text.
     - **Partner** (`booking.professionalId == currentUser.id`):
       - a new booking id appears with `isAwaitingPartnerAcceptance`: **"New job request #<code>"**, "<service> at <locality> on <date>, <time>. Tap to accept." `type = NEW_BOOKING`, `targetRole = PROFESSIONAL`.
       - → `CANCELLED` by the customer: "Customer cancelled #<code>".
   - **Dedupe key** `"${bookingId}_${status}"` (and `"_assigned"` for acceptance). Keep a `Set<String>` of emitted keys, and give notifications deterministic ids from the key, so profile switching or re-sync never duplicates.
   - Reset `lastSeen` and the emitted keys on session/profile change (re-seed silently).
2. **Delete the cross-party `addNotification` calls:**
   - the partner notification in `createBooking`
   - the customer notifications in `acceptJob`, `advanceBookingStatus` and `partnerCancelJob`

   **Keep** the actor's own confirmation, e.g. the customer's "Booking Confirmed #code — waiting for a professional to accept" right after booking. Change its text to that.
3. **Faster pickup of acceptance:** while the logged-in customer has any booking with `isAwaitingPartnerAcceptance` and the app is in the foreground, run `refreshBookingStatus(id)` for those bookings **every 10 s**. Stop when none are waiting. The 12 s tracking-screen poll stays as it is.
   - For the partner: while on the Duty & Jobs tab, trigger `runGuardedSync()` every 20 s, so new requests show up quickly.
   - Use `repeatOnLifecycle(STARTED)` from `MainActivity`.
4. Tapping a notification: the customer goes to the booking tracking screen; the partner goes to `PartnerJobDetail` (existing routing).

### Phase 5: Customer UI
1. **`BookingConfirmationScreen`** (tracking):
   - Header/status for `isAwaitingPartnerAcceptance`: **"Booking Confirmed"**, with the subtitle "Waiting for a professional to accept your request…" and a small pulsing dot or `CircularProgressIndicator(strokeWidth = 2.dp)`.
   - Tracker: `CONFIRMED → "Booking Confirmed"` done, `ASSIGNED → "Professional Assigned"` pending (step 2 highlighted as "in progress" while waiting).
   - **Hide the professional card** (name, rating, Call, **Message Professional**) until `isPartnerAssigned`. In its place, show a neutral card: "We've sent your request to a verified professional. You'll get a notification as soon as they accept." Don't show the hard-coded `Rajesh Sharma` fallback (`~line 616`) while waiting.
   - The Start OTP card can stay visible; if it's shown before assignment, add "Share this only when the professional arrives".
   - The Cancel option stays available (free) while waiting.
2. **`BookingsListScreen`** card: the status chip uses `booking.status.label`. Hide the **Message** button while `isAwaitingPartnerAcceptance`, and show "Awaiting professional" in muted text instead.
3. `grep -rn "professionalId\|professionals.find\|Message Professional" app/src/main/java/com/example/ui` and apply the same rule to any other customer-facing place that shows the pro or opens chat (Home active-booking banner and so on).
4. Build.

### Phase 6: Partner UI
1. `PartnerJobsScreen`: New Jobs = `isAwaitingPartnerAcceptance`. Ongoing = active && `!isAwaitingPartnerAcceptance`. Replace lines ~106/114.
2. `PartnerJobDetailScreen`: `isAccepted = !booking.isAwaitingPartnerAcceptance` (~222/422). `canCancel` stays for `ASSIGNED`/`ON_THE_WAY`/`ARRIVED`.
3. `PartnerBookingsScreen`: for awaiting jobs, replace its advance button with the same green **Accept Job** → `viewModel.acceptJob` → open `PartnerJobDetail`. Its status label for `CONFIRMED` is "New request".
4. `PartnerJobSections.statusLabel`: `CONFIRMED → "New request"`.
5. `AdminDashboardScreen` (~1312): admin "advance" from `PENDING`/`CONFIRMED` → `ASSIGNED` stays allowed (the server fills `accepted_at`). Label it "Force assign".
6. Build.

## 5. MANUAL CHECKLIST (I run it; don't write tests). Use two phones, or one phone and switch profiles.
1. The customer books. They get "Booking Confirmed — waiting…". Tracking shows Booking Confirmed ✔ and "Waiting for a professional". There's **no** pro card and no Message button. Supabase shows `status = CONFIRMED`, `accepted_at = null`.
2. Within about 20 s the partner sees a "New job request" notification and the job under New Jobs with Accept Job.
3. The partner accepts. Supabase shows `status = ASSIGNED` and `accepted_at` set. The Job screen opens.
4. Within about 10 s the customer gets **"Professional Assigned"** (exactly once). The pro card and Message appear, the tracker shows Professional Assigned ✔, and the chat opens.
5. The partner goes through On the way → Arrived → OTP → Started → Payment. The customer gets each notification once, on their own device.
6. Before acceptance, the partner can't advance the job from Partner Bookings, and a direct `booking-update-status` to `ON_THE_WAY` returns "Accept the job first". `chat-open` returns "Chat opens once a professional accepts…".
7. The customer cancels while waiting. The partner gets "Customer cancelled", and the job leaves New Jobs. Accepting afterwards returns "The customer cancelled this booking".
8. Stale-device test: after acceptance, force a customer re-push (edit a pending booking or re-sync offline data). Supabase status stays `ASSIGNED` or later, and `accepted_at` stays set.
9. Old data: bookings that were `ASSIGNED` without `accepted_at` now show as Confirmed/New Jobs. In-flight jobs are unchanged.
10. Double-tap Accept, and switch profiles back and forth: no duplicate notifications.
11. Everything above in dark mode.

## 6. ACCEPTANCE CRITERIA
- A new booking is `CONFIRMED`. It becomes `ASSIGNED` only through the partner's Accept, atomically with `accepted_at`, idempotently.
- The customer sees and is notified of "Professional Assigned" only after acceptance, on their own device. Pro details and chat are hidden until then.
- The partner is notified of new requests on their own device. No cross-device notification is generated by the other party's client.
- The server blocks downgrades, erased acceptance, and work before acceptance (trigger + functions).
- Room 7→8 migration is explicit. `assembleDebug` passes, and `deno check` passes (or note Deno is unavailable).

## 7. OUTPUT FORMAT
One report at the end, 20 lines max:
- files changed
- SQL before/after counts
- deploy commands in order (SQL → functions → app)
- the trigger order observed
- assumptions
- anything I must do by hand
