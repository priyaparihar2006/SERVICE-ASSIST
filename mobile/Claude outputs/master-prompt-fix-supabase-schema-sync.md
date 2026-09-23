# Master Prompt: Fix the Supabase schema vs. app mismatch (ServiceAssist)

How to use: open your coding agent (Claude Code, Cursor, AI Studio, etc.) at the project root, then paste everything below the line as one message.

---

## ROLE

You are a principal Android/Kotlin and Postgres/Supabase engineer with 10+ years of production experience. Your standard: the smallest safe change that fully fixes the root cause, backward compatible, additive, idempotent, reversible, and verified with evidence. You do not refactor, restyle, or "improve" anything outside this task. If an assumption below turns out to be false, stop and report instead of improvising.

## PROJECT CONTEXT

- Android app "Service Assist" (codename Servora): Kotlin, Jetpack Compose, Room (local DB), Supabase PostgREST via Retrofit + Moshi (no Supabase SDK). Package `com.example`.
- Local-first: Room is the source of truth for the UI. `SupabaseSyncManager` pulls remote rows into Room and pushes local rows to Supabase.
- Key files:
  - `supabase_schema.sql` (project root): the Supabase schema and seed data.
  - `app/src/main/java/com/example/data/remote/supabase/SupabaseModels.kt`: DTOs and their JSON column names.
  - `.../supabase/SupabaseApiService.kt`, `SupabaseSyncManager.kt`, `SupabaseClient.kt`, `SupabaseConfig.kt`.
  - `.../data/model/Models.kt` (Room entities), `.../ui/viewmodel/ServoraViewModel.kt`.
- Secrets live in `.env` (`SUPABASE_URL`, `SUPABASE_ANON_KEY`, `GEMINI_API_KEY`). Never print, log, or commit their values.

## THE BUG

The DTOs send/expect columns that do not exist in `supabase_schema.sql`:

| Table | Missing column | DTO field | DTO default |
|---|---|---|---|
| `bookings` | `customer_id` | `SupabaseBookingDto.customerId` | `"user_priya_1"` |
| `bookings` | `customer_name` | `SupabaseBookingDto.customerName` | `"Priya Sharma"` |
| `bookings` | `customer_phone` | `SupabaseBookingDto.customerPhone` | `"+91 98765 43210"` |
| `saved_addresses` | `user_id` | `SupabaseSavedAddressDto.userId` | `"user_priya_1"` |

Consequences:
1. PostgREST rejects every booking and address INSERT with a 4xx (unknown column, code `PGRST204`/`42703`).
2. The sync code does not check HTTP status (`api.insertBooking(...)` etc. return `Response<T>`, which does not throw on 4xx), so the failure is silent and the app looks fine.
3. On pull, remote bookings would be rebuilt using DTO defaults, so every remote booking would appear to belong to Priya. `ServoraViewModel.displayedBookings` filters by `customerId` / `customerName` / `customerPhone`, and `displayedAddresses` filters by `userId`, so identity must round-trip through the database.

A second, hidden bug will break inserts even after the columns are added: the seed data inserts explicit ids (bookings 1-5, saved_addresses 1-4, reviews 1-6) into `BIGSERIAL` columns. Explicit ids do not advance the sequence, so the app's next insert (no id) gets `nextval = 1` and fails with a duplicate primary key (409). The migration must resync the sequences.

I audited the other DTOs (`user_profiles`, `reviews`, `service_categories`) against the schema by reading code, and they match. Re-verify this yourself in Phase 0.

## HARD RULES (do not break the working app)

1. Additive only. Never DROP, TRUNCATE, DELETE, or rename anything in the database. Never change existing column types.
2. Do not touch Room: no entity changes in `Models.kt`, no `@Database` version bump. `ServoraDatabase` uses `fallbackToDestructiveMigration()`, so a version bump would wipe users' local data.
3. Do not rename any `@Json(name = ...)` value, DTO property, or Retrofit endpoint.
4. Do not change UI, navigation, ViewModel filtering logic, RLS policies, auth, or `.env`.
5. Keep old app builds working: new columns must be `NOT NULL DEFAULT <same value the DTO defaults to>`. That way older clients that do not send the keys still succeed, and Moshi's non-null `String` fields never receive `null` (which would throw and break the whole list parse).
6. Every SQL statement must be idempotent (safe to run twice).
7. Never write test rows to the live database (there is no DELETE policy on `bookings`, so you could not clean up). If you must test a write, do it inside `BEGIN; ... ROLLBACK;`.
8. The git working tree has many uncommitted changes. Do NOT run `git checkout`, `restore`, `reset`, `clean`, or `stash`, and do NOT commit unless I ask. Use `GIT_OPTIONAL_LOCKS=0 git status` so you never leave a `.git/index.lock` behind.
9. Never print `SUPABASE_ANON_KEY`. Load it into an environment variable without echoing it.

## PLAN (execute in order; report after each phase)

### Phase 0: Establish ground truth (read-only)
Do not assume the live database matches the file. Get the real state with the Supabase MCP tools (`list_tables`, `execute_sql`) if connected. Otherwise give me the queries below and wait for the results.

```sql
select table_name, column_name, data_type, is_nullable, column_default
from information_schema.columns
where table_schema = 'public'
  and table_name in ('bookings','saved_addresses','user_profiles','reviews','service_categories')
order by table_name, ordinal_position;

select (select count(*) from public.bookings)        as bookings,
       (select count(*) from public.saved_addresses) as addresses,
       (select count(*) from public.reviews)         as reviews;

select last_value, is_called from public.bookings_id_seq;
select last_value, is_called from public.saved_addresses_id_seq;
select last_value, is_called from public.reviews_id_seq;
```

Also confirm the bug empirically with one read-only REST call (key from `.env`, not echoed):
`GET $SUPABASE_URL/rest/v1/bookings?select=customer_id&limit=1` with headers `apikey` and `Authorization: Bearer <key>`. Expected before the fix: HTTP 400 "column bookings.customer_id does not exist".

Then diff every DTO field against the real columns and list any mismatch beyond the four above. If the live database already has the columns, stop and tell me: the fix then reduces to the sequence resync and the schema file update.

### Phase 1: SQL migration (additive, idempotent)

```sql
-- 1) Customer identity on bookings (defaults mirror the DTO defaults)
ALTER TABLE public.bookings
  ADD COLUMN IF NOT EXISTS customer_id    TEXT NOT NULL DEFAULT 'user_priya_1',
  ADD COLUMN IF NOT EXISTS customer_name  TEXT NOT NULL DEFAULT 'Priya Sharma',
  ADD COLUMN IF NOT EXISTS customer_phone TEXT NOT NULL DEFAULT '+91 98765 43210';

-- 2) Owner on saved addresses
ALTER TABLE public.saved_addresses
  ADD COLUMN IF NOT EXISTS user_id TEXT NOT NULL DEFAULT 'user_priya_1';

-- 3) Indexes for the per-user filters
CREATE INDEX IF NOT EXISTS idx_bookings_customer_id      ON public.bookings (customer_id);
CREATE INDEX IF NOT EXISTS idx_saved_addresses_user_id   ON public.saved_addresses (user_id);

-- 4) Resync sequences after explicit-id seeding (prevents duplicate-key on next insert)
SELECT setval(pg_get_serial_sequence('public.bookings','id'),
              COALESCE((SELECT MAX(id) FROM public.bookings), 1),
              (SELECT COUNT(*) > 0 FROM public.bookings));
SELECT setval(pg_get_serial_sequence('public.saved_addresses','id'),
              COALESCE((SELECT MAX(id) FROM public.saved_addresses), 1),
              (SELECT COUNT(*) > 0 FROM public.saved_addresses));
SELECT setval(pg_get_serial_sequence('public.reviews','id'),
              COALESCE((SELECT MAX(id) FROM public.reviews), 1),
              (SELECT COUNT(*) > 0 FROM public.reviews));

-- 5) Make PostgREST see the new columns immediately
NOTIFY pgrst, 'reload schema';
```

Apply it with the Supabase `apply_migration` tool (name it `align_schema_with_app_dtos`) if available. Otherwise hand me the exact block to run in Dashboard, SQL Editor, and wait for my confirmation. Existing rows are backfilled with the defaults, which is correct for the seeded demo rows (all Priya's). If Phase 0 showed rows from other users, report them before applying.

### Phase 2: Keep `supabase_schema.sql` as the single source of truth
- Add the four columns (with the same defaults) inside the `CREATE TABLE IF NOT EXISTS` blocks for `bookings` and `saved_addresses`, so fresh projects get the right schema.
- Append the Phase 1 ALTER/INDEX/setval/NOTIFY statements as a clearly labelled "MIGRATION (safe to re-run)" section after the seed data, because `CREATE TABLE IF NOT EXISTS` will not alter an existing table.
- Do not change the existing RLS policies or seed rows. Keep the file re-runnable end to end.

### Phase 3: Make sync failures visible (tiny Kotlin change, no behavior change)
In `SupabaseSyncManager.kt`, add a private helper and use it on the results of the push and update calls (`insertBooking`, `insertAddress`, `insertReview`, `upsertUserProfile`, `updateBookingStatusByCode`, `deleteAddress`), including the async ones:

```kotlin
private fun <T> retrofit2.Response<T>.logIfFailed(op: String) {
    if (!isSuccessful) Log.w(TAG, "$op failed: HTTP ${code()} ${errorBody()?.string()?.take(300)}")
}
```

It must never throw, never change control flow or return values, and never log the API key. Do not touch the OkHttp `Level.BODY` logging (separate task).

### Phase 4: Verify with evidence
1. Re-run the schema query from Phase 0 and show the four new columns with their defaults.
2. Repeat the REST probe. `select=customer_id` on `bookings` and `select=user_id` on `saved_addresses` must now return HTTP 200.
3. Check the sequences: `select last_value, is_called` for the three sequences must be at or above the current max id.
4. Optional write test only inside `BEGIN; INSERT ...; ROLLBACK;`: insert a booking without an id and confirm no duplicate-key error.
5. Build: run `./gradlew :app:compileDebugKotlin` (Windows: `gradlew.bat`). It must succeed. Note: `GreetingScreenshotTest` is already broken (it uses a removed `onRoleToggleClick` parameter and Roborazzi is commented out), so `testDebugUnitTest` will fail for that pre-existing reason. Do not fix it here; just report it. If you cannot run Gradle in your environment, say so plainly and give me the exact command. Do not claim the build passes without running it.
6. Give me this manual device checklist:
   - Log in as the demo customer, create a booking, and confirm in Supabase Table Editor that the row has `customer_id = user_priya_1` and the right name and phone.
   - Add an address and confirm the row has `user_id` set.
   - Clear app data, reopen, and confirm My Bookings and the addresses come back after sync.
   - Check Logcat for tag `SupabaseSyncManager`: there should be no `failed: HTTP 4xx` lines.

### Phase 5: Report
Give me: (a) files changed with a short diff summary, (b) the SQL you ran or handed me, (c) the evidence outputs from Phase 4 with secrets redacted, (d) rollback SQL, (e) anything you found but deliberately did not change.

Rollback (only if truly needed; it is additive so leaving it is harmless):
```sql
ALTER TABLE public.bookings
  DROP COLUMN IF EXISTS customer_id, DROP COLUMN IF EXISTS customer_name, DROP COLUMN IF EXISTS customer_phone;
ALTER TABLE public.saved_addresses DROP COLUMN IF EXISTS user_id;
DROP INDEX IF EXISTS public.idx_bookings_customer_id, public.idx_saved_addresses_user_id;
```

## ACCEPTANCE CRITERIA
- Booking and address inserts from the app succeed against Supabase, and identity fields round-trip (pull rebuilds the correct `customerId` / `userId`).
- New inserts without an id no longer collide with seeded ids.
- Older app builds keep working (defaults in place). Room schema and version unchanged. No UI or logic changes.
- `compileDebugKotlin` passes. `supabase_schema.sql` is re-runnable and matches the live database.
- Failures in push calls now show up in Logcat.

## OUT OF SCOPE (report only, do not fix here)
- RLS is fully open (`USING (true)` for anon) and there is no real authentication.
- Local Room ids differ from remote `BIGSERIAL` ids, and pushes do not write the remote id back. `deleteAddressAsync(id)` may therefore delete the wrong remote row, and pulling remote addresses with `OnConflictStrategy.REPLACE` may overwrite a different local address with the same id. This is suspected from reading the code: verify and report, do not change.
- Sync pulls every user's bookings and addresses (no per-user filter).
- `booking_code` uses a random 5-digit number, so a collision would make the push silently fail on the unique constraint.
- OkHttp `Level.BODY` logging in release builds, and the hard-coded fallback Supabase URL in `SupabaseConfig.kt`.
