# MASTER PROMPT 16: Minimal, low-weight app-wide typography with DM Sans, readable in light and dark (Servora / ServiceAssist)

Paste everything below the line into your coding agent. Work in the repo root `ServiceAssist/`.

---

## 0. ROLE AND WORKING RULES

You are a senior Android engineer (Kotlin, Jetpack Compose, Material 3 typography).

1. Post a 6-line plan, then work through every phase without stopping. Report once at the end.
2. Don't ask questions unless you are blocked.
3. **Typography only.** Don't change layouts, colors (except the contrast fixes in Phase 5), spacing, logic or copy.
4. **No tests.** The only checks are `./gradlew assembleDebug` after each phase and the manual checks in Phase 6.

## 1. GOAL
- The whole app uses **one clean, minimal sans-serif: DM Sans** (Google Fonts, SIL Open Font License, free for commercial use). Visually, it's the closest open-license match to the Urban Company category labels in the reference ("Salon Prime", "Salon Prime for kids & men", "Cleaning"): a geometric grotesk with a single-story `g`, a classic `&` and generous letter shapes.
- **Low boldness:** body text Regular (400), titles and buttons Medium (500), and at most SemiBold (600) for big headings and prices. **Nothing heavier than 600 anywhere.**
- Clearly readable in **both light and dark mode**.
- **Bundled** in the APK (works offline, with no Google Play Services dependency). Not a downloadable font.

Note: Urban Company's own app font isn't publicly available or licensed for reuse, so don't try to extract or copy it. Use DM Sans.

## 2. CURRENT STATE (verified in the repo)
- `ui/theme/Type.kt`: every style uses `FontFamily.Default` (Roboto), with heavy weights: `displayLarge` Black, `displayMedium` ExtraBold, `headline*` Bold, `title*` SemiBold, `labelLarge` / `labelSmall` Bold.
- `Theme.kt:325` passes `typography = Typography` to `MaterialTheme`.
- **Hard-coded weight overrides across `ui/`:** `FontWeight.Bold` × **324**, `SemiBold` × 60, `Black` × 28, `ExtraBold` × 15, `Medium` × 44. So changing `Type.kt` alone won't reduce boldness.
- One explicit `fontFamily = FontFamily.Monospace` in `PaymentCollectionScreen.kt:643` (likely a UPI/reference code). Keep it.
- There's no `res/font/` folder. `res/values/themes.xml` has no `fontFamily`.

## 3. THE FIX

### Phase 1: Add the font files
1. Download DM Sans from Google Fonts (<https://fonts.google.com/specimen/DM+Sans> → "Get font" → "Download all"). Unzip it and take the **static** TTFs for weights **400, 500 and 600**, upright only:
   - `DMSans-Regular.ttf`, `DMSans-Medium.ttf`, `DMSans-SemiBold.ttf`
   - If the zip only has optical-size variants (`DMSans_18pt-…`, `DMSans_24pt-…`, `DMSans_36pt-…`), use the **`18pt`** ones for all three weights.
   - If you can't download files, stop and ask me to place the three TTFs in `app/src/main/res/font/`. That's the one allowed stop.
2. Copy them to `app/src/main/res/font/` with Android-legal names: `dm_sans_regular.ttf`, `dm_sans_medium.ttf`, `dm_sans_semibold.ttf`.
3. Add `app/src/main/assets/licenses/DMSans-OFL.txt` (the OFL text from the zip).
4. Check that the files contain `₹` (U+20B9), using `fc-query` or `fonttools`. The full Google Fonts build does; the Latin-only web subsets don't. If it's missing, you have the wrong files.

### Phase 2: Font family + typography (`ui/theme/Type.kt`)
```kotlin
val DmSans = FontFamily(
    Font(R.font.dm_sans_regular,  FontWeight.Normal),
    Font(R.font.dm_sans_medium,   FontWeight.Medium),
    Font(R.font.dm_sans_semibold, FontWeight.SemiBold),
    // Safety net: anything heavier still renders as SemiBold (keeps the app "low bold")
    Font(R.font.dm_sans_semibold, FontWeight.Bold),
    Font(R.font.dm_sans_semibold, FontWeight.ExtraBold),
    Font(R.font.dm_sans_semibold, FontWeight.Black),
    // Anything lighter than Regular renders as Regular (thin text is unreadable in dark mode)
    Font(R.font.dm_sans_regular,  FontWeight.Light),
    Font(R.font.dm_sans_regular,  FontWeight.ExtraLight),
    Font(R.font.dm_sans_regular,  FontWeight.Thin),
)
```
Replace `Typography` with the following. **Keep the existing sizes**, so no layout shifts. Only the family, weights, tracking and line heights change:

| Style | Weight | Size / line height | Letter spacing |
|---|---|---|---|
| displayLarge | SemiBold | 36 / 44 | -0.25sp |
| displayMedium | SemiBold | 28 / 36 | -0.2sp |
| headlineLarge | SemiBold | 24 / 32 | -0.1sp |
| headlineMedium | SemiBold | 20 / 28 | 0sp |
| titleLarge | Medium | 18 / 26 | 0sp |
| titleMedium | Medium | 16 / 24 | 0.1sp |
| (add) titleSmall | Medium | 14 / 20 | 0.1sp |
| bodyLarge | Normal | 15 / 22 | 0.15sp |
| bodyMedium | Normal | 13 / 19 | 0.15sp |
| (add) bodySmall | Normal | 12 / 17 | 0.2sp |
| labelLarge (buttons) | Medium | 14 / 20 | 0.1sp |
| labelMedium | Medium | 12 / 16 | 0.2sp |
| labelSmall | Medium | **11** / 15 (was 10, raised for readability) | 0.3sp |

- Every style sets `fontFamily = DmSans` and uses `platformStyle = PlatformTextStyle(includeFontPadding = false)` plus `lineHeightStyle = LineHeightStyle(Alignment.Center, Trim.None)`, so DM Sans's metrics don't clip or shift text vertically.
- Also fill any Material 3 styles that aren't defined (`displaySmall`, `headlineSmall`) with DmSans, so no style falls back to Roboto.

### Phase 3: Apply the family everywhere
1. **Default for plain `Text`:** in `ServoraTheme` (`Theme.kt`), wrap the content in
   `CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) { content() }`
   inside the `MaterialTheme` block. That way any `Text` without a `style` also gets DM Sans.
2. `grep -rn "FontFamily\.\(Default\|SansSerif\|Serif\)" app/src/main/java`: replace every hit with `DmSans`. **Keep** `FontFamily.Monospace` in `PaymentCollectionScreen.kt:643`.
3. `TextStyle(` constructions made outside the theme (for example inline `style = TextStyle(platformStyle = …)` in `ServoraBottomNav.kt` / `PartnerBottomNav.kt`): add `fontFamily = DmSans`, or build from `LocalTextStyle.current.copy(...)`.
4. Non-Compose text: in `res/values/themes.xml`, add `<item name="android:fontFamily">@font/dm_sans_regular</item>` to `Theme.MyApplication` (covers toasts, dialogs and the splash label).

### Phase 4: Reduce hard-coded boldness across `ui/`
Do a mechanical replace in `app/src/main/java/com/example/ui/**` only, then review the diff:

| Find | Replace with |
|---|---|
| `FontWeight.Black` | `FontWeight.SemiBold` |
| `FontWeight.ExtraBold` | `FontWeight.SemiBold` |
| `FontWeight.Bold` | `FontWeight.Medium` |
| `FontWeight.SemiBold` (**original occurrences only**) | `FontWeight.Medium` |
| `FontWeight.W700` / `W800` / `W900` | `FontWeight.Medium` / `SemiBold` / `SemiBold` |

- Order matters. First convert the original `SemiBold` → `Medium`, then `Black`/`ExtraBold` → `SemiBold`, then `Bold` → `Medium`. That way the new SemiBolds aren't downgraded.
- **Keep SemiBold (600)** only for:
  - prices / amounts (₹) in cards and totals
  - the screen title in each top bar
  - the brand wordmark "Service Assist" (`ServoraTopBar`)
  - big hero numbers (earnings totals, OTP digits)

  After the replace, set these back to `SemiBold` by hand, and list them in the report.
- Don't change `fontSize` values, except any hard-coded `fontSize` < 11.sp on text using `FontWeight.Normal`/`Medium`: raise it to **11.sp**. Badge dots have no text, so they're not affected.
- `letterSpacing` overrides that were negative on body or label text (tight tracking designed for Roboto Bold) → set to `0.sp`. Keep negative tracking only on sizes ≥ 24.sp.

### Phase 5: Readability in dark and light mode
Lighter weights look thinner, especially on dark backgrounds. Check and fix only these color usages:
1. Regular (400) text **< 14sp** must use `textPrimary` or `textSecondary`, **never `textMuted`**. Replace `textMuted` on such text with `textSecondary`. Keep `textMuted` only for ≥ 14sp or for disabled states.
2. Confirm the contrast of the token pairs used for text (compute and report the numbers):
   - light: `textPrimary #111827`, `textSecondary #6B7280`, `textMuted #9CA3AF` on `#FFFFFF` / `cardBackground`
   - dark: `textPrimary #E8EEEA`, `textSecondary #A9B7B0`, `textMuted #7D8B84` on `cardBackground #171D1A` / `cardBackgroundSubtle #1F2723`
   - Any text pair below **4.5:1** gets bumped:
     - light `textMuted` → `#6B7280` for text use
     - dark `textMuted` → at least `#8E9C95` (must be ≥ 4.5:1 on `#1F2723`)

   Don't change any other colors.
3. White text on brand-green buttons and headers stays white. With Medium (500), it stays legible at ≥ 14sp.
4. Respect the system font scale: don't use `.value` hacks or fixed-`dp` text. Test at font scale 1.3.

### Phase 6: Verify (manual, on device)
1. `./gradlew assembleDebug`.
2. Home: category tiles ("Salon Prime", "Cleaning" and so on) render in DM Sans Regular or Medium. They should look like the reference: light, airy, not bold.
3. Walk through Home → Service Detail → Booking Flow → Confirmation/Tracking → Bookings → Chat → Notifications → Profile, and as a partner through Jobs → Job Detail → Payment Collection → Earnings. There's no Roboto left (compare the `g` and `&` shapes) and **nothing looks heavy or black-bold**.
4. `₹` renders correctly in DM Sans in prices.
5. Dark mode on the same screens: all text is clearly readable, and small grey text isn't washed out.
6. Font scale 1.3 (Settings → Display → Font size): no clipped text in buttons, chips, bottom nav or chat bubbles.
7. The OTP / UPI reference code still shows in monospace.

## 4. ACCEPTANCE CRITERIA
- DM Sans (400/500/600) is bundled in `res/font`, with the OFL license included, and applied to all Compose text and the XML theme.
- No text renders heavier than SemiBold. Most UI text is Regular or Medium.
- Every text color pair used for text is ≥ 4.5:1 in both themes (numbers reported).
- No layout or behavior changes. The build passes.

## 5. FINAL REPORT FORMAT
Plan (6 lines), font files added (with the ₹ check), replace counts per weight, the list of places deliberately kept at SemiBold, the contrast table (before → after), and pass/fail for Phase 6 steps 2-7.
