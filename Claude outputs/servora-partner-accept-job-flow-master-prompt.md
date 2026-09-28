# MASTER PROMPT — Partner Home Shows Job Info + Green "Accept Job", Then a Dedicated Job Screen for All Task Updates (Servora / ServiceAssist)

Paste everything below the line into your coding agent. Work in the repo root `ServiceAssist/`. These are already applied and must keep working:

- dark mode (`ServoraTheme.colors`, `// theme-invariant`)
- per-booking chat with the partner's job-card message icon
- payment collection

---

## 0. ROLE AND WORKING RULES

You are a staff-level engineer with 10+ years of experience in Android (Kotlin, Jetpack Compose, Room), Supabase (Postgres, Deno Edge Functions) and marketplace UX.

1. Read every file named in section 1 first. If the code differs from what I describe, trust the code, say so in one line, and adapt.
2. Post a short plan, then do every phase in order without stopping. Report once at the end.
3. Don't ask questions unless you are truly blocked. State assumptions in one line.
4. **Move UI, don't rewrite logic.** Status transitions, OTP verification, payment collection, chat, call and map must behave exactly as they do today. They just live on a new screen.
5. Every new color comes from `MaterialTheme.colorScheme` / `ServoraTheme.colors`. `Color.White` is only allowed on brand-green surfaces, with `// theme-invariant`. Check both light and dark.
6. **No tests.** Don't write or run unit, UI, SQL or Deno tests. The only checks are `./gradlew assembleDebug` after each Android phase and `deno check` on the new function (if Deno is installed).
7. Never log customer phone numbers, addresses or OTPs.

## 1. CURRENT STATE — VERIFIED IN THE CODE

- **`ui/screens/PartnerJobsScreen.kt`** is the partner home, the "Duty & Jobs" tab (`PartnerNavTab.DUTY_JOBS`).
  - `activeJobs = bookings.filter { status != COMPLETED && status != CANCELLED }`, rendered as `items(activeJobs) { PartnerJobCard(...) }` (~line 467).
  - It also owns the **OTP verification dialog** state (`showOtpDialogForBooking`, `enteredOtp`, `otpError`, ~lines 150-160 and 492-600) and the `statusUpdateErrorFlow` snackbar.
- **`internal fun PartnerJobCard(...)`** (~line 614) does *everything* on the home page. Its sections:
  1. header: booking code (copy), status chip, amount, PAID
  2. service thumbnail, name, package and schedule
  3. customer and location, with the **Call** and **Message** icons (Message icon with unread badge, ~line 936) and a **Map** button
  4. completed-job details (payout breakdown)
  5. **task updates**: the 5-step tracker (Assigned → On Way → Arrived → Started → Payment), the "Ask for OTP" banner, and the primary action button (`testTag("btn_partner_advance_job")`)
- **The primary button** does this:
  - For `STARTED` / `AWAITING_PAYMENT`, it calls `onCollectPayment(id)`.
  - Otherwise it calls `onAdvanceStatus(id, status)`.
  - Labels by status: `ASSIGNED` "Start Travel to Customer", `ON_THE_WAY` "I Have Arrived at Doorstep", `ARRIVED` "Verify Customer OTP & Start" (goes through the OTP dialog), `STARTED` "Complete Duty • ₹x", `AWAITING_PAYMENT` "Proceed to Payment • ₹x".
- **`ServoraViewModel.advanceBookingStatus(id, currentStatus, onResult)`** (~line 656) maps the next status and calls `repository.updateBookingStatus`, which updates Room (`pendingSync`) and then `syncManager.updateBookingStatus(code, status)` → `booking-update-status`. It also posts customer and partner notifications.
- **`MainActivity.kt`:**
  - `PartnerNavTab.DUTY_JOBS -> PartnerJobsScreen(...)` (~line 505) wires `onAdvanceStatus`, `onAdvanceStatusWithCallback`, `statusUpdateErrorFlow`, `onOpenChat` (→ `openThreadForBooking` → `AppScreen.ChatThread`), `onCollectPayment` (→ `AppScreen.PaymentCollection`), `onViewAllBookings` (→ `PartnerBookings`) and `onNotificationsClick`.
  - `AppScreen` is a hand-rolled sealed interface. A `when` (~line 263-300) decides top-bar visibility per screen. `BackHandler` returns to `MainTabs`.
  - Partner notifications that have a `bookingId` currently go to `AppScreen.PartnerBookings`.
- **Data:**
  - `Booking` (Room, `ServoraDatabase` **version = 6**, explicit migrations up to `MIGRATION_5_6`) has **no acceptance field**. Bookings are created with `status = ASSIGNED` and `professionalId` already set.
  - Supabase `bookings` (`supabase_schema.sql`) has no `accepted_at` either.
  - `booking-update-status/index.ts` checks `booking.professional_id === identity.profileId` when `identity.isProd`.
  - `SupabaseSyncManager` pulls remote bookings into Room.
- **`PartnerBookingsScreen.kt`** (history and all bookings) has its own card, advance button and OTP dialog. **It is out of scope. Leave it working as is.**

## 2. WHAT I WANT (requirements)

1. **Partner home = job information only.** For each new job, the home page shows a clean card with the job's information and **one green "Accept Job" button**. The home page has no tracker, no status buttons, no OTP and no payment actions.
2. **Tapping "Accept Job"** accepts the job and then **opens a dedicated Job screen** for that booking.
3. **The Job screen** is where the partner does **all the task updates that used to be on the home page**: the step tracker, Start Travel → Arrived → Verify OTP & Start → Complete Duty → Proceed to Payment, plus Call, Message (chat), Map, and the payout breakdown. It must behave exactly as the home card does today.
4. **Accepted jobs that aren't finished** stay reachable from home, so the partner can return after leaving the app: a compact card with a green outlined **"Continue Job"** button that opens the same Job screen.
5. Acceptance is **saved in Supabase** and survives restarts, logouts and other devices. It's never shown as accepted when the server didn't record it.
6. **Cancel after accepting.** The Job screen has a **red "Cancel Job" button**. The partner must pick a reason (plus an optional note, required for "Other"). The cancellation and the partner's reason are **saved in Supabase**, and the customer is informed.

## 3. LOCKED DESIGN DECISIONS

### 3.1 What counts as "accepted"
- A new nullable column `bookings.accepted_at TIMESTAMPTZ`. The booking **status doesn't change** on accept: it stays `ASSIGNED`. That way the status machine, the customer screens, chat and payments are untouched.
- Room gets a matching column `acceptedAt: Long? = null`.
- **Backfill:** every existing booking whose status is past `ASSIGNED` (`ON_THE_WAY`, `ARRIVED`, `STARTED`, `AWAITING_PAYMENT`, `COMPLETED`) counts as accepted: `accepted_at = COALESCE(accepted_at, created_at)`. This keeps in-flight jobs from falling back to "Accept Job".
- On the client, "needs acceptance" means `status == ASSIGNED && acceptedAt == null`. Everything else that's active counts as accepted.

### 3.2 Accept is a server-confirmed action
The partner must be online, and the UI waits for the server:
- New Edge Function **`booking-accept`** (POST `{ booking_id }`), using `_shared/identity.ts` and `_shared/http.ts`, following the style of `booking-update-status`.
  - Load the booking. Reject with 403 unless `professional_id === identity.profileId` (same rule and `isProd` handling as `booking-update-status`).
  - Reject with 409 if the status is `CANCELLED`/`COMPLETED`, returning `"This job is no longer available"`.
  - **Idempotent:** if `accepted_at` is already set, return 200 with the existing value.
  - Otherwise, `update bookings set accepted_at = now() where id = … and accepted_at is null` and return `{ success: true, booking_id, accepted_at }`.
  - Every error is a readable sentence (`jsonError`).
- Android: `BookingApiService.acceptBooking(...)` → `ServoraRepository.acceptBooking(id): Result<Long>` → on success, write `acceptedAt` to Room → `ServoraViewModel.acceptJob(id, onResult)`. Also post a customer notification, "Your professional accepted the job ✅", with the same `AppNotification` pattern as `advanceBookingStatus`.
- On failure, show a snackbar with the server's sentence, keep the card as is and re-enable the button. **No optimistic local accept.**

### 3.3 Home page layout (`PartnerJobsScreen`)
Keep the existing brand header (status/online toggle, Sync) and the empty state. Split the list into two sections:

- **"New Jobs (n)"**: jobs that need acceptance, newest first. Each one uses the new `PartnerJobSummaryCard`:
  - Booking code and a **"NEW"** chip.
  - Amount (₹), and a payment-mode chip ("Cash after service" / "UPI" / "PAID").
  - Service thumbnail (`getServiceImageDrawable`), service name and package.
  - Date • time.
  - Customer **first name** and **locality, city**, special notes (if any), and the service duration/package details already on the model.
  - **Privacy:** don't show the full street address or phone number, and no Call/Map/Message, until the job is accepted. They appear on the Job screen.
  - **"Accept Job"** button:
    - full width, 52 dp tall, rounded 14 dp
    - brand green `colorScheme.primary`, white bold text with a `CheckCircle` icon (`// theme-invariant` on green)
    - `testTag("btn_partner_accept_job_${booking.id}")`
    - while the request is in flight, show a 18 dp spinner and ignore taps (no double accept)
    - on success, navigate to `AppScreen.PartnerJobDetail(booking.id)`
- **"Ongoing Jobs (n)"**: accepted but not completed or cancelled, ordered by schedule. Same summary card, but:
  - the chip shows the current status label (the existing `statusLabel` mapping)
  - the button is a green **outlined "Continue Job"** (`testTag("btn_partner_continue_job_${booking.id}")`)
  - tapping the card also opens the Job screen
  - the message icon with its unread badge may appear here, small and top-right, since the job is accepted
- The "View all bookings" entry point to `PartnerBookings` stays as it is.

### 3.4 The Job screen
New `AppScreen.PartnerJobDetail(val bookingId: Long)` and `ui/screens/PartnerJobDetailScreen.kt`:
- **Top bar:** back arrow, "Job #<code>" and the status chip. Hide the bottom nav, and add the case to `MainActivity`'s top-bar/bottom-bar visibility `when`s. `BackHandler` goes to `MainTabs` with `currentPartnerTab = DUTY_JOBS`.
- **Content**, reusing the existing home-card sections (move them, don't re-implement):
  1. amount/paid header
  2. service info
  3. full customer and location section, with Call, the **Message icon (unread badge, same `onOpenChat` path)** and Map
  4. the **5-step tracker**
  5. the "Ask customer for OTP" banner when `ARRIVED`
  6. the **primary action button**, with identical labels, colors and click logic, including the OTP dialog for `ARRIVED` and `onCollectPayment` for `STARTED`/`AWAITING_PAYMENT`
  7. the payout breakdown when completed
- **Snackbar** for `statusUpdateErrorFlow`.
- **Move the OTP dialog state and UI** from `PartnerJobsScreen` into this screen, unchanged: same `startOtp` check, same messages, same `onAdvanceStatusWithCallback`.
- The booking comes from the existing bookings flow (`displayedBookings.find { it.id == bookingId }`), so it updates live after each status change.
  - If the booking disappears or becomes `CANCELLED`, show a small "This job was cancelled" state with a "Back to Duties" button.
  - When it becomes `COMPLETED`, show the completed state (payout breakdown) and a "Back to Duties" button.
- **Guard:** if someone opens it for an unaccepted job (from a deep link or notification), show the summary and the Accept button in place of the task section instead of allowing task updates.
- **PaymentCollection:** when opened from the Job screen, its back/done must return to `PartnerJobDetail(bookingId)`, not the home tab. Pass the origin, or keep a simple `previousScreen`.

### 3.5 Code structure
- Split `PartnerJobCard` into small internal composables in a new `ui/components/PartnerJobSections.kt`:
  - `JobHeaderSection`
  - `JobServiceSection`
  - `JobCustomerSection`, with an `isAccepted` flag that hides the phone, full address and actions when false
  - `JobTrackerSection`
  - `JobOtpBanner`
  - `JobPrimaryActionButton`
  - `JobPayoutBreakdown`
- `PartnerJobSummaryCard` and `PartnerJobDetailScreen` compose these. Delete `PartnerJobCard` once nothing references it.

### 3.6 Notifications
Partner notifications with a `bookingId` now open `PartnerJobDetail(bookingId)` instead of `PartnerBookings`. Customer routing is unchanged.

### 3.7 Partner "Cancel Job" (Job screen only)

**When it's allowed**

Only when the job is accepted and the status is `ASSIGNED`, `ON_THE_WAY` or `ARRIVED`, meaning work hasn't started. For `STARTED`, `AWAITING_PAYMENT`, `COMPLETED` or `CANCELLED`, hide the button. For `STARTED` / `AWAITING_PAYMENT`, show a small muted line instead: "Work has started. Contact support to cancel." The server enforces the same rule.

**The button**

- Full width, below the primary action button, 48 dp tall, rounded 14 dp.
- Outlined red: border and text `colorScheme.error` (dark mode: `ServoraTheme.colors.danger`), with a `Cancel` icon, text "Cancel Job".
- `testTag("btn_partner_cancel_job")`.
- It must look clearly secondary to the green primary action, so it can't be mis-tapped.

**The reason sheet**

Tapping the button opens a `PartnerCancelJobBottomSheet`. Model it on the existing `ui/components/CancelBookingBottomSheet.kt`, with the same look, theme tokens and dark-mode support, but with partner reasons. Each reason has a stable code stored in the DB plus a label:

| Code | Label |
|---|---|
| `EMERGENCY` | Personal / family emergency |
| `VEHICLE_ISSUE` | Vehicle breakdown or travel problem |
| `RUNNING_LATE` | Can't reach on time for this slot |
| `LOCATION_ISSUE` | Location too far or unreachable |
| `CUSTOMER_UNREACHABLE` | Customer not reachable / not responding |
| `CUSTOMER_REQUEST` | Customer asked me to cancel |
| `TOOLS_UNAVAILABLE` | Required tools or parts not available |
| `SAFETY_CONCERN` | Safety concern at the location |
| `OTHER` | Other reason |

- A note field: optional, but **required (min 10 chars) for `OTHER`**, max 300 chars, with a counter.
- A red **"Confirm Cancellation"** button (filled `colorScheme.error`, white text `// theme-invariant`). It stays disabled until a reason is selected, then shows a spinner and blocks taps while in flight.
- A secondary "Keep Job" button to dismiss.

**Server-confirmed, like Accept**

New Edge Function **`booking-partner-cancel`** (POST `{ booking_id, reason_code, reason_note }`), using `_shared/identity.ts`, `_shared/http.ts` and `recomputeBookingChatStatus`:
1. Load the booking. Return 403 unless `professional_id === identity.profileId` (same `isProd` handling as `booking-update-status`).
2. Validate `reason_code` against the list above. `OTHER` requires a note of at least 10 chars. Trim the note, cap it at 300 chars, and scrub phone numbers/emails with `_shared/scrubber.ts`.
3. Return 409 `"This job can't be cancelled because work has already started"` unless the status is `ASSIGNED`/`ON_THE_WAY`/`ARRIVED` and `accepted_at` is not null.
4. **Idempotent:** if the booking is already `CANCELLED` with `cancelled_by = 'PARTNER'`, return 200.
5. In one statement, update the booking:
   - `status = 'CANCELLED'`
   - `cancelled_by = 'PARTNER'`
   - `cancelled_at = now()`
   - `cancellation_reason = <label>`
   - `cancellation_feedback = <note>`
   - `partner_cancel_reason_code = <code>`

   Then **insert an audit row** into `partner_job_cancellations`.
6. Call `recomputeBookingChatStatus` (the chat goes READ_ONLY, as for any cancel). Return `{ success: true, booking_id, status: "CANCELLED" }`.
7. Every error is a readable sentence.

**Android**

- `BookingApiService.partnerCancelBooking(...)` → `ServoraRepository.partnerCancelJob(id, code, note): Result<Unit>` → on success, update Room (`status = CANCELLED`, `cancellationReason`, `cancellationFeedback`, `cancelledAt`, new `cancelledBy = "PARTNER"`) → `ServoraViewModel.partnerCancelJob(id, code, note, onResult)`.
- No optimistic local cancel. On failure, show the server's sentence in a snackbar and keep the sheet open.
- Post a **customer notification**: "Your professional had to cancel <service> #<code>. We're sorry — please rebook or contact support." Use the existing `AppNotification` pattern, `targetRole = CUSTOMER`, with `bookingId`.
- After success: close the sheet, show a snackbar "Job cancelled", and return to `MainTabs` → Duty & Jobs. The job disappears from New/Ongoing because the home filter already excludes `CANCELLED`. It still shows in Partner Bookings history as "Booking Cancelled" with the reason (that screen already renders `cancellationReason`).
- Customer side: the booking shows as Cancelled with the existing UI. Where the customer sees the cancel reason (tracking / My Bookings), prefix it with "Cancelled by professional:" when `cancelledBy == "PARTNER"`. That's the only customer-screen change.

### 3.8 Out of scope
- No "Decline" button before acceptance. Cancel exists only after accepting.
- No automatic reassignment to another partner. Admin can see partner cancellations in the Supabase audit table.
- No change to customer screens beyond the one notification.
- No change to `PartnerBookingsScreen`.
- No change to the status machine.

## 4. THE WORK

### Phase 1 — Supabase
1. `supabase_booking_accept_migration.sql` (idempotent):
   ```sql
   ALTER TABLE public.bookings ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMPTZ;
   UPDATE public.bookings SET accepted_at = COALESCE(accepted_at, created_at)
    WHERE status IN ('ON_THE_WAY','ARRIVED','STARTED','AWAITING_PAYMENT','COMPLETED') AND accepted_at IS NULL;
   CREATE INDEX IF NOT EXISTS idx_bookings_pro_accept ON public.bookings (professional_id, accepted_at);
   ```
2. `supabase/functions/booking-accept/index.ts` as described in 3.2. Run `deno check`.
3. Confirm the realtime publication/`select=*` sync picks up the new column automatically, with no RLS change needed.
4. Give me the exact commands: run the SQL, then `supabase functions deploy booking-accept`.

### Phase 2 — Android data layer
1. `Booking` gets `acceptedAt: Long? = null`. `ServoraDatabase` goes to `version = 7` with an explicit `MIGRATION_6_7` (`ALTER TABLE bookings ADD COLUMN acceptedAt INTEGER`), registered in `addMigrations`. Don't rely on destructive fallback.
   - Local backfill in the same migration: `UPDATE bookings SET acceptedAt = createdAt WHERE status IN ('ON_THE_WAY','ARRIVED','STARTED','AWAITING_PAYMENT','COMPLETED') AND acceptedAt IS NULL`. Check how `status` is stored (enum name via TypeConverter).
2. `SupabaseModels` booking DTO: add `@Json(name = "accepted_at") val acceptedAt: String? = null`. Map it both ways (ISO ↔ epoch millis, via the existing time helpers).
3. `SupabaseSyncManager`:
   - The remote → local pull sets `acceptedAt` from the server (the server wins when it's non-null).
   - The local → remote push **never sends `accepted_at: null`** over an existing value. Omit the field when it's null locally.
4. `BookingApiService.acceptBooking`, `ServoraRepository.acceptBooking(id)`, `ServoraViewModel.acceptJob(id, onResult: (Result<Unit>) -> Unit)`, plus the customer notification.
5. Build.

### Phase 3 — Home page
Implement 3.3 in `PartnerJobsScreen`:
- new sections and `PartnerJobSummaryCard`
- remove the tracker, action button, OTP dialog and task UI from home
- new params: `onAcceptJob: (Booking, (Result<Unit>) -> Unit) -> Unit` and `onOpenJob: (Long) -> Unit`
- wire them in `MainActivity`
- build

### Phase 4 — Job screen
Implement 3.4, 3.5 and 3.6: `AppScreen.PartnerJobDetail`, `PartnerJobDetailScreen`, the moved OTP dialog, the section composables, `MainActivity` routing (visibility `when`s, `BackHandler`, PaymentCollection return, notification routing). Build.

### Phase 5 — Final pass
- `grep -rn "PartnerJobCard(" app/src` → nothing (or only the new sections).
- `grep -rn "btn_partner_advance_job" app/src` → only in `PartnerJobDetailScreen` / sections.
- Check that no new `Color(0x…)` appears outside `ui/theme/`.
- Check light and dark mode on both new UIs.
- `./gradlew assembleDebug`.

## 5. MANUAL TEST CHECKLIST (I run this myself — don't write tests)
1. Log in as a partner with a fresh assigned job. Home shows it under **New Jobs** with the job info, locality only (no phone, no full address) and a green **Accept Job** button. There's no tracker and no status buttons.
2. Tap Accept Job. A spinner shows, then the Job screen opens. Supabase `bookings.accepted_at` is set, and the customer gets the "accepted" notification.
3. On the Job screen: Start Travel → Arrived → Verify OTP & Start (wrong OTP shows an error, the right OTP starts the job) → Complete Duty → Proceed to Payment → PaymentCollection. After payment, you're back on the Job screen in the completed state. The customer app shows every status as before.
4. Back from the Job screen mid-job: home shows the job under **Ongoing Jobs** with **Continue Job**. Kill the app and reopen it: still Ongoing (not New). Log in on another device: same.
5. Airplane mode, then tap Accept Job: a readable error, and the job stays under New Jobs.
6. Double-tap Accept quickly: only one request, one navigation.
7. Jobs that were already in progress before this update show as Ongoing, not New.
8. Message icon on the Job screen: opens that booking's chat, and the unread badge works. Call and Map work.
9. Partner notification with a booking: opens that job's Job screen.
10. Everything above in dark mode: every text, icon and button is readable. The Partner Bookings screen still works as before.

## 6. ACCEPTANCE CRITERIA
1. The partner home shows job info plus a green Accept Job button for new jobs, and Continue Job for ongoing ones. All task updates happen only on the Job screen.
2. Acceptance is stored in Supabase (`accepted_at`), is idempotent, and only the assigned partner can do it. Existing in-flight jobs are backfilled as accepted.
3. Status transitions, OTP, payment, chat, call and map behave exactly as before.
4. The Room 6→7 migration is explicit and non-destructive. `assembleDebug` passes, and `deno check` passes (or say Deno isn't available). Every changed or added file is listed.

## 7. OUTPUT FORMAT
One report at the end, 15 lines max:
- files changed or added
- migration and deploy commands, in order (SQL → `supabase functions deploy booking-accept` → install the app)
- assumptions you made
- anything I must do by hand
