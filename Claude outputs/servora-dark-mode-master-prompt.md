# MASTER PROMPT — App-Wide Dark Mode with a Toggle in the My Account Screen (Servora / ServiceAssist)

Paste everything below the line into your coding agent. Work in the repo root `ServiceAssist/`. This is a UI-only change: no Supabase, API or data-model changes. Every screen must keep working exactly as it does now. Only colors change.

---

## 0. ROLE AND WORKING RULES

You are a staff-level Android engineer with 10+ years of experience in Jetpack Compose, Material 3 theming and accessibility (WCAG 2.1 AA).

1. Read every file named in section 1 before you edit anything. If the code differs from what I describe, trust the code and say so in one line.
2. Post a short plan, then work through the phases in order without stopping. Report once at the end.
3. Don't ask me questions unless you are truly blocked. State assumptions in one line.
4. Change **colors only**. Don't change layouts, spacing, copy, navigation, business logic, or any light-mode colors. Light mode must look pixel-identical to today.
5. **No tests.** Don't write or run unit, UI, screenshot or Roborazzi tests. The only check is `./gradlew assembleDebug` at the end of each phase.
6. Keep changes mechanical and reviewable. Migrate one file per step and don't mix in unrelated refactors.

## 1. CURRENT STATE — VERIFIED IN THE CODE

- `ui/theme/Theme.kt`:
  - `MyApplicationTheme(darkTheme: Boolean = false)` already switches between `ServoraLightColorScheme` and `ServoraDarkColorScheme` and sets the status/nav bar icon colors.
  - `MainActivity.kt:141` calls `MyApplicationTheme(darkTheme = false)`, which is hard-coded, so dark mode is never on.
  - The existing `ServoraDarkColorScheme` is an old leftover: its warm browns don't match the brand, and it's missing `secondaryContainer`, `tertiary`, `error*`, `outlineVariant`, `surfaceContainer*` and `inverse*`.
  - `SetDynamicStatusBar()` forces `isAppearanceLightNavigationBars = true`.
- `ui/theme/Color.kt`: the brand is **green `#009051`** (dark green `#0B5433`, mint `#E6F5EE`). The names are historical: "Coral" and "Peach" are green and mint.
- `ui/theme/Type.kt`: **every text style hard-codes `color = ServoraCharcoal` or `ServoraSubtext`.** In dark mode, all text that uses the typography defaults would stay near-black on a dark background.
- **Hard-coded colors are the main problem:** there are **~800 `Color(0x…)` literals** plus many `Color.White` / `Color.Black` across `ui/`, and only **19** usages of `MaterialTheme.colorScheme`. The worst offenders, by literal count:

  | File | Literals |
  |---|---|
  | `BookingFlowScreen` | 124 |
  | `HomeScreen` | 75 |
  | `ProfileScreen` | 75 |
  | `PartnerJobsScreen` | 72 |
  | `NotificationsScreen` | 64 |
  | `PartnerBookingsScreen` | 55 |
  | `AdminDashboardScreen` | 48 |
  | `BookingConfirmationScreen` | 47 |
  | `PartnerSettlementHistoryScreen` | 45 |
  | `PartnerEarningsScreen` | 43 |
  | `BookingsListScreen` | 36 |
  | `ChatComponents` | 33 |
  | `LocationPickerModal` | 31 |
  | `ServiceDetailScreen` | 28 |
  | `ChatThreadScreen` | 27 |
  | `PaymentCollectionScreen` | 26 |
  | `PartnerToolkitScreen` | 25 |
  | `ExploreScreen` | 21 |
  | `ServoraTopBar`, `LoginScreen`, `OffersScreen`, `CancelBookingBottomSheet` | ~18 each |

  Plus the three bottom navs (`ServoraBottomNav`, `PartnerBottomNav`, `AdminBottomNav`), which declare their own `private val Brand…` colors, and `SearchOverlay`. Many files also declare file-private palettes, for example `SlateDarkText`, `SlateMutedText`, `DeepForestGreen` and `MintLinkText` in `ProfileScreen`.
- `ProfileScreen.kt` is the **My Account** screen, used by both customer and partner (`MainActivity` ~lines 555 and 681):
  - green header card with avatar, name, phone and "Edit profile"
  - tiles: My Bookings / Service Assist Money / Help & Support
  - `GreenMenuItemCard` rows
  - dialogs: edit profile, addresses, pass, wallet, support, refer, about/terms/privacy, delete account, log out
- **Admin** has its own profile/account tab inside `AdminDashboardScreen.kt`. Find it.
- Status bar behavior lives in `ui/components/SystemBarsAppearance.kt` (`StatusBarIcons(darkIcons)`, `hideStatusBarOnScroll`) and in the scroll-hide logic in `MainActivity`.
- `res/values/themes.xml`: `Theme.MyApplication` has parent `android:Theme.DeviceDefault.NoActionBar` and sets no window background. There's a risk of a white flash on launch in dark mode.
- DataStore Preferences is already in `libs.versions.toml` (`androidx-datastore-preferences` 1.1.7), but it's commented out in `app/build.gradle.kts:97`.
- Photos (`res/drawable/img_*.jpg`, `app_logo.png`, `img_servora_icon.jpg`) are used through `painterResource` in 8 files.

## 2. WHAT I WANT (requirements)

1. A full **dark mode** for the whole app: all three roles (Customer, Partner, Admin), every screen, dialog, bottom sheet, text field, snackbar, bottom nav, top bar, chat bubble, badge, chip, card, divider and empty state.
2. **Everything stays readable in dark mode:** every icon, text, number, price, OTP, status chip, badge, placeholder, hint, disabled state, border and divider. Minimum contrast:
   - **4.5:1** for text, **3:1** for large text (≥ 18 sp, or 14 sp bold), icons, borders of interactive controls, and focus/selected states.
   - Nothing may rely on a near-black color placed on the dark background.
3. **The user switches theme only from the My Account screen,** using a **small sun/moon icon button**. There's no setting anywhere else, and the app does **not** follow the system dark setting.
   - **Default is Light**, so existing users see no change until they opt in.
4. The choice is **remembered** across app restarts and logout/login on the same device.
   - It applies **instantly** without restarting the activity.
   - There's **no white flash** on cold start in dark mode.
5. **Light mode stays exactly as it is today.**

## 3. LOCKED DESIGN DECISIONS

### 3.1 Toggle
- A 36 dp circular icon button in the **top-right corner of the green profile header card** in `ProfileScreen`, and in the same spot on the Admin account/profile tab.
- Icon: `Icons.Outlined.DarkMode` (moon) while in light mode, `Icons.Outlined.LightMode` (sun) while in dark mode. 20 dp, tinted white, on a `Color.White.copy(alpha = 0.18f)` circle with a 1 dp `Color.White.copy(alpha = 0.35f)` border, so it reads on the green header in both themes.
- Tap → toggle → `Crossfade`/`AnimatedContent` icon swap (≈200 ms) and a light haptic tick.
- `contentDescription` is "Switch to dark mode" or "Switch to light mode". `testTag("theme_toggle")`.
- Minimum touch target 48 dp (`Modifier.minimumInteractiveComponentSize()`).
- No text label, no menu row, no toggle anywhere else.

### 3.2 Storage
- `ThemePreferenceRepository` backed by DataStore Preferences, key `app_theme` = `"LIGHT" | "DARK"`, default `"LIGHT"`.
- **Per device**, not per user: don't clear it on logout.
- It's local only, so no Supabase sync.

### 3.3 Theme architecture
- Material 3 `ColorScheme` plus a small **extended semantic palette**, `ServoraColors`, provided through `staticCompositionLocalOf` and exposed as `ServoraTheme.colors`. It covers what Material doesn't:
  - `success` / `onSuccess` / `successContainer` / `onSuccessContainer`
  - `warning` / `warningContainer` / `onWarningContainer`
  - `info` / `infoContainer` / `onInfoContainer`
  - `danger` / `dangerContainer` / `onDangerContainer`
  - `textPrimary` / `textSecondary` / `textMuted` / `textOnBrand`
  - `cardBackground` / `cardBorder` / `divider`
  - `brandGradientStart` / `brandGradientEnd`
  - `headerBackground` (the green profile/home headers)
  - `chipBackground` / `chipText`
  - `ratingStar`
  - `badgeRed`
  - `navBarBackground` / `navBarSelected` / `navBarSelectedIndicator` / `navBarUnselected`
  - `chatBubbleMine` / `onChatBubbleMine` / `chatBubbleTheirs` / `onChatBubbleTheirs` / `chatSystemBubble`
  - `scrimOnImage`
  - `shimmer`
- Every composable reads colors **only** from `MaterialTheme.colorScheme.*` or `ServoraTheme.colors.*`. The only exceptions are the theme files and the "allowed literal" cases in Phase 3.

### 3.4 Dark palette
Use this as the starting point. Tune it only if a contrast check fails, and report any change.

| Role | Light (today, unchanged) | Dark |
|---|---|---|
| background | `#FFFFFF` | `#0F1412` |
| surface / card | `#FFFFFF` | `#171D1A` |
| surfaceVariant / subtle card | `#F9FAFB` / `#F5F5F7` | `#1F2723` |
| elevated (dialogs, sheets) | `#FFFFFF` | `#222B27` |
| outline / border | `#E5E7EB` | `#33403A` |
| divider | `#E5E7EB` | `#2A3430` |
| textPrimary | `#111827` | `#E8EEEA` |
| textSecondary | `#4B5563` / `#6B7280` | `#A9B7B0` |
| textMuted / hint | `#9CA3AF` | `#7D8B84` (large text / hints only) |
| primary (brand green) | `#009051` | `#34C27F` |
| onPrimary | `#FFFFFF` | `#00210F` |
| primaryContainer (mint) | `#E6F5EE` | `#0B3D26` |
| onPrimaryContainer | `#0B5433` | `#B6F2D2` |
| headerBackground (green header cards) | today's green | `#0B5433` → `#0E6B42` gradient; white text stays white |
| warning / star | `#F59E0B` / `#B45309` on `#FEF3C7` | `#FBBF24` on `#3A2E0B` |
| info | `#2563EB` on `#EFF6FF` | `#7AB8FF` on `#0E2742` |
| danger / badge | `#E53935` | `#FF6B6B` on `#3B1414`; badge stays `#E53935` with white text |
| navBarBackground | `#FFFFFF` | `#171D1A`, selected `#34C27F`, indicator `#0B3D26`, unselected `#8FA098` |

The dark primary `#34C27F` on `#0F1412` passes AA. Keep white text on the dark green `#0B5433` header.

## 4. THE WORK

### Phase 1 — Theme infrastructure
1. Uncomment `implementation(libs.androidx.datastore.preferences)` in `app/build.gradle.kts`.
2. Create `data/prefs/ThemePreferenceRepository.kt`, with:
   - `val themeFlow: Flow<AppTheme>`
   - `suspend fun setTheme(AppTheme)`
   - `enum class AppTheme { LIGHT, DARK }`
   - a single DataStore instance via `preferencesDataStore(name = "servora_settings")`
3. Create `ui/viewmodel/ThemeViewModel.kt` (or add to an existing app-level ViewModel if one fits better, as long as it stays small). It exposes `isDark: StateFlow<Boolean>` and `fun toggle()`.
4. `MainActivity.onCreate`:
   - Read the stored theme **before** `setContent`, using a one-time blocking `runBlocking { repo.themeFlow.first() }` (acceptable: one tiny read). Or hold the `installSplashScreen()` keep-on-screen condition until it loads.
   - Pass it as the initial value, then collect the flow with `collectAsStateWithLifecycle`.
   - Replace `MyApplicationTheme(darkTheme = false)` with `MyApplicationTheme(darkTheme = isDark)`.
5. `ui/theme/Color.kt`: add the dark tokens from 3.4. Keep all existing light constants unchanged, because light mode must stay identical.
6. `ui/theme/Theme.kt`:
   - Rewrite `ServoraDarkColorScheme` completely, filling **every** role: `primary`/`on*`/`*Container`, `secondary*`, `tertiary*`, `error*`, `background`, `surface`, `surfaceVariant`, `surfaceContainerLowest…Highest`, `outline`, `outlineVariant`, `inverseSurface`, `inverseOnSurface`, `inversePrimary`, `scrim`.
   - Complete the light scheme's missing roles with values that match today's look.
   - Add `ServoraColors` (light and dark instances), `LocalServoraColors` and `object ServoraTheme { val colors @Composable get() }`.
   - Provide both in `MyApplicationTheme`.
   - Status and nav bar icons follow `darkTheme`. Fix `SetDynamicStatusBar`, which forces light nav-bar icons, and make `SystemBarsAppearance.StatusBarIcons(...)` / `hideStatusBarOnScroll` respect the current theme, so icons are never dark-on-dark.
7. `ui/theme/Type.kt`: **remove the hard-coded `color =` from every text style.** Text then inherits `LocalContentColor`/`onSurface`. Where a style relied on `ServoraSubtext`, call sites that need secondary text must pass `ServoraTheme.colors.textSecondary`. In light mode, verify that text which was charcoal is still charcoal (`onSurface`/`onBackground` = `#111827`).
8. Window background: don't add a `res/values-night/` folder, because the app ignores the system night mode. Instead, set `window.setBackgroundDrawable(ColorDrawable(bg))` in `onCreate` from the stored theme before `setContent`. This avoids a white flash on cold start in dark mode.
9. Build.

### Phase 2 — The toggle in My Account
1. `ProfileScreen`: add the icon button from 3.1 to the top-right of the green header card, with new params `isDarkTheme: Boolean` and `onToggleTheme: () -> Unit`. Wire both `ProfileScreen(...)` call sites in `MainActivity` (customer and partner) to `ThemeViewModel`.
2. Add the same button to the **Admin** account/profile tab in `AdminDashboardScreen`.
3. Check that there's no other theme entry point anywhere (grep `DarkMode|LightMode|darkTheme`).
4. Build.

### Phase 3 — Migrate every hard-coded color
Go **file by file**, in this order:
1. theme-adjacent components: `ServoraTopBar`, the 3 bottom navs, `ChatComponents`, `SearchOverlay`, `LocationPickerModal`, `CancelBookingBottomSheet`, `SystemBarsAppearance`
2. customer screens: Home, Explore, Offers, ServiceDetail, BookingFlow, BookingConfirmation, BookingsList, Profile, Notifications, Login
3. chat: ChatThread (and ChatList, if it still exists)
4. partner screens: PartnerJobs, PartnerBookings, PartnerEarnings, PartnerSettlementHistory, PartnerToolkit, PaymentCollection
5. AdminDashboard

For each file:
- Replace each `Color(0x…)`, `Color.White`, `Color.Black` and file-private palette `val` with the **semantic** token that matches its *role*, not its hex value. Examples:
  - page background → `colorScheme.background`
  - card → `ServoraTheme.colors.cardBackground`
  - title → `textPrimary`
  - body → `textSecondary`
  - brand green accents → `colorScheme.primary`
  - mint pill → `primaryContainer` / `onPrimaryContainer`
  - status chips → `success/warning/info/danger` pairs
  - borders → `cardBorder` / `outline`
  - dividers → `divider`
- Delete the file-private palettes once nothing uses them.
- **Allowed literals** (leave them and add a `// theme-invariant` comment):
  - `Color.White` text/icons **on** a brand-green, dark-green or image-scrim surface
  - `Color.Transparent`
  - brand-invariant logos
  - the badge red with white text
  - any color inside `ui/theme/`
- **Text fields**: every `OutlinedTextField`/`TextField` uses `OutlinedTextFieldDefaults.colors(...)` built from tokens (text, label, placeholder, cursor, focused/unfocused borders, container), so labels and hints are readable in dark.
- **Dialogs and sheets**: `AlertDialog` `containerColor = surfaceContainerHigh` (or `elevated`), with title and text colors from tokens. `ModalBottomSheet` `containerColor` and drag handle from tokens. Buttons keep brand colors.
- **Snackbars / Toasts**: use a themed `SnackbarHost` where snackbars exist. Toasts are system-rendered, so leave them.
- **Images**: photos keep their colors. Where text sits on a photo, keep or add the `scrimOnImage` gradient. Where a light-background logo (`app_logo.png`, `img_servora_icon.jpg`) sits on a dark surface, put it on a small rounded `Color.White` plate (`// theme-invariant`) so it doesn't look like a white box floating on the page.
- **Gradients / shadows**: gradient stops come from tokens. In dark mode, reduce `shadow()` elevation or replace it with a 1 dp `cardBorder`, because black shadows are invisible on dark and cards lose their edges.
- **Icons**: every `Icon(tint = …)` uses a token. Icons without an explicit tint inherit `LocalContentColor`, so make sure the parent `Surface`/`Card` sets the right content color.
- **Disabled states**: disabled buttons and chips use `onSurface.copy(alpha = 0.38f)` on `onSurface.copy(alpha = 0.12f)`, never a fixed grey.
- **Chat**: my bubble, their bubble and the SYSTEM/BOOKING_UPDATE bubble each have readable text and timestamps. Read-receipt ticks are visible in both themes (blue read ticks become `#7AB8FF` in dark).
- **OTP / price / amounts / status**: check each one explicitly. They must be clearly visible (OTP digits, ₹ totals, the "AWAITING PAYMENT" and "CANCELLED" chips, the 4.9 ★ rating).
- **Payment QR** (`util/UpiQr.kt` / `PaymentCollectionScreen`): the QR code must stay **black on a white plate** in dark mode so UPI apps can scan it.

Build after each file group (1–5 above). Keep light mode visually unchanged at every step.

### Phase 4 — Guardrails and final pass
- Run `grep -rn "Color(0x" app/src/main/java/com/example/ui --include=*.kt | grep -v "/ui/theme/" | grep -v "theme-invariant"`. It must return **nothing**. Do the same for `Color.Black`. Every remaining `Color.White` must carry `// theme-invariant`.
- Run `grep -rn "color = Servora\(Charcoal\|Subtext\|DarkTitle\|BodyGray\)" app/src/main/java/com/example/ui --include=*.kt | grep -v "/ui/theme/"`. It must return nothing: those light constants may only be used inside the light palette.
- Run `grep -rn "isSystemInDarkTheme" app/src`. It must return nothing (the app doesn't follow the system setting).
- Contrast pass: for each dark token pair used for text or icons (text on background/surface/containers, primary on background, chip text on chip bg, nav unselected on nav bg), compute the WCAG contrast ratio with a tiny throwaway Kotlin/JS snippet. Don't commit it. List any pair under 4.5:1 (text) or 3:1 (icons/large text) and fix it.
- Run `./gradlew assembleDebug`.

## 5. MANUAL TEST CHECKLIST (I run this myself — don't write tests)

1. Fresh install → the app is in **Light** and looks exactly like before.
2. My Account → tap the moon icon → the whole app switches to dark instantly, the icon becomes a sun, and there's no activity restart.
3. Kill and reopen the app → still dark, with **no white flash**. Log out and log in as another role → still dark.
4. Walk through **every** screen in dark mode and confirm that nothing is invisible or low-contrast:
   - Customer: Home, Services/Explore, Search overlay, Offers, Service detail, Booking flow (all steps, promo code, address picker, time slots, payment), Booking confirmation/tracking (OTP), My Bookings (all tabs, Message button with badge), Notifications, My Account (every dialog: edit profile, addresses, pass, wallet, support, refer, about/terms/privacy, delete, log out), Login.
   - Chat: thread, composer, quick replies, ticks, error banner.
   - Partner: Duty & Jobs, job-card message icon, Partner Bookings, Earnings, Settlement history, Toolkit, Pro Profile, Payment collection (QR still scannable).
   - Admin: Overview, Bookings, Customers, Partners, Profile/account (toggle present).
5. Status bar and navigation bar icons are light on dark in dark mode and dark on light in light mode, including after scroll-hide/show.
6. Text fields: labels, typed text, placeholders, cursor and error text are all readable in dark.
7. The toggle is **only** in My Account (customer/partner) and the Admin account tab. The system dark-mode setting has no effect on the app.

## 6. ACCEPTANCE CRITERIA

1. The theme toggle exists only in the My Account screens, the choice persists per device, the default is Light, it applies instantly, and there's no cold-start flash.
2. Light mode looks unchanged.
3. The Phase 4 greps return nothing (except annotated `// theme-invariant` lines), and there are no hard-coded colors in `Type.kt`.
4. All text/icon token pairs in dark meet WCAG AA (4.5:1 text, 3:1 icons/large text). List the ratios for the main pairs.
5. `assembleDebug` passes. Every changed or added file is listed.

## 7. OUTPUT FORMAT

One report at the end, 20 lines max, with:
- files changed or added
- the final dark palette (including any tuned values and their contrast ratios)
- any `// theme-invariant` literals you kept, with the reason for each category
- anything you couldn't migrate and why
