# MASTER PROMPT 3 — Full Service Catalog for Every Category, with AI-Generated Service Photos (Servora / ServiceAssist)

Paste everything below the line into your coding agent. Work in the repo root `ServiceAssist/`. This is independent of the chat work; do not touch chat code except where a shared file forces it.

---

## 0. ROLE AND WORKING RULES

You are a staff-level engineer with 10+ years of experience in Android (Kotlin, Jetpack Compose), content design for consumer marketplaces, and production image pipelines. You care about accuracy, app size, accessibility and honesty of claims shown to customers.

1. Read every file named in section 1 first. If the code differs from what I describe, trust the code and say so.
2. Post a short plan (file structure, image budget, risks), then work phase by phase (section 8). Do not stop between phases: run straight through and report once at the end, unless you are truly blocked.
3. Do not ask me questions unless truly blocked. State an assumption in one line and continue.
4. Small, reviewable commits per phase. No unrelated refactors.
5. Never print, log, commit or ship API keys. Do not open or echo `.env`. The image tools in section 6 read the key from the environment, and the key must never be used by the Android app.
6. **No tests.** I will test everything myself. Do not write or run unit tests, Deno tests, SQL/RLS test scripts, Compose UI or screenshot tests, and do not fix existing tests. The only check you run is `./gradlew assembleDebug` once at the end of each phase, just to confirm it compiles. Keep every report to a few lines.

## 1. CURRENT STATE (verified in the repo)

- **14 categories** in `data/repository/ServoraRepository.kt` (~lines 170-280): `cat_ac`, `cat_cleaning`, `cat_salon_w`, `cat_salon_m`, `cat_electrician`, `cat_plumber`, `cat_carpenter`, `cat_pest`, `cat_painting`, `cat_ro`, `cat_appliances`, `cat_bathroom`, `cat_kitchen`, `cat_sofa`. They carry only `name`, `description`, `startingPrice`, `iconName`, `tag`, `isFeatured` — **no images**.
- **Only 6 services** exist (`val services` ~line 280): `ac_service_deep` (cat_ac), `deep_home_cleaning` (cat_cleaning), `salon_women_glow` (cat_salon_w), `electrician_quick_fix` (cat_electrician), `plumber_leak_fix` (cat_plumber), `bathroom_deep_scrub` (cat_bathroom). **Eight categories have zero services**: salon_m, carpenter, pest, painting, ro, appliances, kitchen, sofa. Tapping them shows an empty list.
- Each `ServiceItem` (`data/model/Models.kt`) has: id, categoryId, name, subtitle, startingPrice, rating, reviewsCount, duration, warrantyText, description, `imageDrawableRes: Int`, isPopular, isHeroFeatured, recommendedProId, whatIsIncluded, whatIsNotIncluded, `packages: List<ServicePackage>` (id, name, description, price, originalPrice, durationText, includes), `faqs: List<Pair<String,String>>`. The AC service (~lines 280-352) is the model of good content: 5 included, 3 not included, 3 packages, 2 FAQs, an Agra-specific description.
- **Images:** 19 JPGs in `app/src/main/res/drawable/` (about 12 MB total, roughly 500-880 KB each; `img_ac_repair` and `img_cleaning_pro` are 1200×896, the others checked are 1024×1024, so aspect ratios are inconsistent). Only 4 are referenced from Kotlin (`img_ac_repair`, `img_cleaning_pro`, `img_salon_wellness`, `img_hero_service`). `img_hero_service` is reused for both electrician and plumber, `img_cleaning_pro` for both cleaning and bathroom. About 13 others look orphaned (`img_dishwash_liquid`, `img_floor_cleaner`, `img_multisurf_cleaner`, `img_kitchen_essentials`, `img_home_makeover`, `img_cozy_living`, `img_bathroom_cleaner`, `img_fridge_repair`, `img_washing_machine`, `img_threading`, `img_nail_art`, `img_haircut_styling`, `img_facial_cleanup`) — verify with grep, including XML.
- Images are shown with `painterResource(service.imageDrawableRes)` in `ExploreScreen.kt` (~line 303, the `ServiceExploreCard` shown on the Services tab, `ServoraNavTab.SERVICES`), `SearchOverlay.kt` (~line 225) and `ServiceDetailScreen.kt` (~line 127, guarded by `!= 0`). Coil 2.7.0 is a dependency but unused. Minify and resource shrinking are off, so every drawable is packaged.
- **Home category grid is broken** (`HomeScreen.kt` ~lines 105-116, 339-365): 8 hard-coded tiles using Material icons, not the 14 real categories. "Plumbing" points to `cat_plumbing` (does not exist; the real id is `cat_plumber`), "Painting" points to `cat_cleaning`, and every unmatched tile falls back to `categories.firstOrNull()`, which is AC Repair. "More" also opens the first category. Most categories cannot be reached from Home at all.
- Only 4 professionals exist (`ServoraRepository.kt` ~line 585): `pro_rajesh_1` (AC & appliances), `pro_amit_2` (deep home & floor care), `pro_meera_3` (beautician), `pro_dinesh_4` (electrician). Every service uses `recommendedProId` defaulting to Rajesh.
- Promo codes (`FIRST20`, `CLEAN100`, `GLOW50`, `ACCOOL150`, `ELECT50`, `PEST25`, `AGRA200`) exist around line 640; keep them valid.
- `Supabase service_categories` can override categories at runtime (`SupabaseModels.kt` ~line 202). Remote rows carry no image data.
- The merged debug manifest contains `INTERNET` only via a Firebase library; the source manifest does not declare it. Declare `INTERNET` and `ACCESS_NETWORK_STATE` explicitly in `app/src/main/AndroidManifest.xml` (a one-line hygiene fix; `HEAD` had them).

## 2. GOAL

Every one of the 14 categories has real, believable content and AI-generated photos: 4 services per category (56 total), each with a unique photo and full details, plus one cover photo per category (14). Customers can reach every category from Home, browse its services with photos, open a detailed service page, and see the service photo again in search and booking screens.

## 3. CONTENT STANDARDS (apply to all 56 services)

- **Fields to write for each service:** subtitle (one line), description (2 to 3 sentences, concrete: tools, method, what the customer gets), duration, warranty text, 4 to 6 items in `whatIsIncluded`, 2 to 4 in `whatIsNotIncluded` (honest exclusions such as spare parts), **3 packages** (small, combo, best value), **3 FAQs** with useful answers, `recommendedProId`, rating, reviewsCount, `isPopular` on about one per category, `imageAlt`.
- **Local flavour, no fluff.** Agra: hard water and limescale, summer heat and dust, monsoon damp and termites in older houses, marble and vitrified tile floors, RWA/society rules, water TDS. English, plain and specific. Prices in ₹ with the "₹" symbol.
- **Pricing rules:** `startingPrice` equals the cheapest package price; `originalPrice` is above `price`, with a discount between 10% and 40%; combo packages must be cheaper per unit than the single package; prices must be plausible for a tier-2 Indian city.
- **Honest claims.** No "100% guaranteed", "best in India", medical or cure claims, "government approved", "ISI certified", "certified" or "licensed" unless the existing data already says so. Warranty text must be modest and consistent within a category. Pest control: mention re-entry time and keeping children and pets away during treatment, and never name specific chemicals. Salon: mention a patch test for skin products. Electrician: mention mains isolation and a safety check, no promises about wiring code compliance. Painting waterproofing: no lifetime claims.
- **Placeholder honesty.** `rating` and `reviewsCount` are invented seed data (they already are for the existing 6 services). Keep them plausible (4.6 to 4.9, review counts from tens to a few thousand, lower for newer services) and write `docs/catalog-placeholders.md` listing every field that must be replaced with real data before public launch: ratings, review counts, warranties, professional names and experience.
- **Professionals.** Add professionals so every category has a suitable `recommendedProId`: plumber, carpenter, pest control, painter, RO technician, men's groomer (6 new). Reuse Amit's crew for kitchen, bathroom and sofa cleaning, Rajesh for appliance repair. Use initials avatars only (existing `avatarInitials`), no AI-generated faces of "staff", and no phone numbers that look real (follow whatever the chat work did to keep phones out of customer-facing models; if it has not landed yet, use empty strings).

## 4. CATALOG BLUEPRINT (14 categories × 4 services)

Use these ids, names and "from" prices as the starting point. You may adjust a name or price slightly if you can justify it, but keep four services per category and keep the existing 6 services' ids, ratings and content (improve wording only if clearly wrong).

| Category | Services: id — name — from ₹ — duration |
|---|---|
| `cat_ac` | `ac_service_deep` — Intense AC Foam Jet Service (existing) — 499 — 45 min; `ac_repair_diagnosis` — AC Repair & Fault Diagnosis — 299 — 45 min; `ac_gas_refill` — AC Gas Refill & Leak Repair — 1499 — 90 min; `ac_install_uninstall` — AC Installation & Uninstallation — 599 — 90 min |
| `cat_cleaning` | `deep_home_cleaning` — Full Home Deep Cleaning (existing) — 999 — 4-6 h; `empty_home_cleaning` — Empty / Move-in Home Cleaning — 1199 — 3-4 h; `floor_scrub_polish` — Floor Scrubbing & Polishing — 799 — 2-3 h; `carpet_mattress_shampoo` — Carpet & Mattress Shampooing — 599 — 90 min |
| `cat_salon_w` | `salon_women_glow` — Glow Facial & Spa at Home (existing) — 399 — 60 min; `women_waxing` — Full Arms & Legs Waxing — 449 — 50 min; `women_mani_pedi` — Manicure & Pedicure — 599 — 75 min; `women_hair_spa` — Haircut & Hair Spa — 699 — 75 min |
| `cat_salon_m` | `men_haircut_beard` — Haircut & Beard Styling — 249 — 40 min; `men_facial_cleanup` — Facial & Cleanup — 399 — 45 min; `men_head_massage` — Head & Shoulder Massage — 299 — 30 min; `men_detan_pack` — De-Tan Pack & Face Care — 449 — 40 min |
| `cat_electrician` | `electrician_quick_fix` — Electrician On-Demand Visit (existing) — 199 — 30-60 min; `fan_install_repair` — Ceiling Fan Repair & Installation — 249 — 30 min; `mcb_switchboard_wiring` — MCB, Switchboard & Wiring Repair — 299 — 60 min; `decor_light_install` — Chandelier & Decor Light Installation — 399 — 60 min |
| `cat_plumber` | `plumber_leak_fix` — Plumbing Leakage & Tap Care (existing) — 199 — 45 min; `tap_mixer_repair` — Tap & Mixer Repair or Replacement — 249 — 40 min; `flush_tank_repair` — Flush Tank & Toilet Repair — 349 — 60 min; `basin_sink_install` — Washbasin & Sink Installation — 499 — 90 min |
| `cat_carpenter` | `furniture_repair` — Furniture Repair & Polish Touch-up — 249 — 60 min; `door_lock_install` — Door Lock & Hinge Installation — 299 — 45 min; `drill_hang_service` — Drilling, Curtain Rods & Wall Hanging — 199 — 45 min; `bed_wardrobe_assembly` — Bed & Wardrobe Assembly — 499 — 2 h |
| `cat_pest` | `cockroach_ant_control` — Cockroach & Ant Control — 799 — 60 min; `termite_treatment` — Termite Treatment — 1999 — 2-3 h; `bedbug_treatment` — Bed Bug Treatment — 1299 — 90 min; `mosquito_fly_control` — Mosquito & Fly Control — 699 — 45 min |
| `cat_painting` | `room_repaint` — Single Room Repaint — 1499 — 1 day; `full_home_painting_1bhk` — 1 BHK Full Home Painting — 8999 — 3-4 days; `waterproofing_damp` — Waterproofing & Damp Treatment — 2999 — 1 day; `texture_wall_design` — Texture & Accent Wall Design — 2499 — 1 day |
| `cat_ro` | `ro_filter_service` — RO Filter Service — 299 — 45 min; `ro_membrane_replace` — RO Membrane Replacement — 899 — 60 min; `ro_install_uninstall` — RO Installation & Uninstallation — 499 — 60 min; `ro_repair_diagnosis` — RO Repair & Water Quality Check — 349 — 45 min |
| `cat_appliances` | `washing_machine_repair` — Washing Machine Repair — 349 — 60 min; `fridge_repair` — Refrigerator Repair — 399 — 60 min; `microwave_repair` — Microwave Repair — 349 — 45 min; `geyser_repair` — Geyser Repair & Service — 399 — 45 min |
| `cat_bathroom` | `bathroom_deep_scrub` — Intense Bathroom Stain Removal (existing) — 599 — 60-90 min; `hard_water_descaling` — Hard Water Descaling — 799 — 90 min; `tile_grout_whitening` — Tile & Grout Whitening — 899 — 2 h; `fixtures_exhaust_polish` — Fixtures & Exhaust Fan Polish — 399 — 45 min |
| `cat_kitchen` | `kitchen_deep_clean` — Kitchen Deep Cleaning — 799 — 3 h; `chimney_degrease` — Chimney & Hob Degreasing — 899 — 90 min; `cabinet_clean` — Cabinet Clean, Inside & Out — 1099 — 3 h; `gas_stove_clean` — Gas Stove Deep Clean — 399 — 45 min |
| `cat_sofa` | `sofa_fabric_shampoo` — Fabric Sofa Shampoo Cleaning (per seat) — 399 — 45 min; `sofa_leather_care` — Leather Sofa Cleaning & Polish — 599 — 60 min; `sofa_lshape_clean` — L-Shape Sofa Deep Clean — 1299 — 2 h; `dining_chair_clean` — Dining Chair & Cushion Cleaning — 249 — 45 min |

Full-home painting and pest jobs vary by size, so state the assumption in the package name (for example "up to 600 sq ft") and add an "Exact price after a free site visit" line in the FAQs.

## 5. DATA ARCHITECTURE

- Do not grow `ServoraRepository.kt` further. Create `data/catalog/` with one Kotlin file per category (`AcCatalog.kt`, `CleaningCatalog.kt`, `SalonWomenCatalog.kt`, …), a small `CatalogDsl.kt` to remove boilerplate (helper for packages and FAQ pairs), and `ServoraCatalog.kt` exposing `val categories`, `val services`, `val professionals`. `ServoraRepository` keeps its public API and delegates to `ServoraCatalog`, so no screen or ViewModel signature changes.
- Add `imageAlt: String = ""` to `ServiceItem` (default keeps the Supabase mapper and any other constructor call compiling). Do not add image fields to `ServiceCategory`, because Supabase overrides replace those objects. Resolve covers with `CategoryImages.coverFor(categoryId: String): Int` (a map in `data/catalog/`), so remote-overridden categories still get covers. Unknown id returns a neutral fallback drawable, never 0.
- Resource naming: `img_svc_<service_id>.webp` and `img_cat_<category_id_without_cat_prefix>.webp`, all lowercase with underscores, for example `img_svc_ac_service_deep.webp`, `img_cat_ac.webp`.
- Remove every shared or placeholder image reference: no two services may use the same drawable. `imageDrawableRes` must never be 0.

## 6. AI PHOTO PIPELINE

**Deliverables:** 56 service photos and 14 category covers, 70 images in total.

**Tooling.** Build it in `tools/` (not part of the Android app):

- `tools/image_prompts.json` — the single source of truth: for each image, `id`, `type` (service or category), `scene` (a specific, accurate description), `alt`, and the final prompt text.
- `tools/generate_images.py` — calls an image-generation API, resumable and idempotent (skips images that already exist), 2 to 3 concurrent requests, retry with exponential backoff, writes raw PNGs to `tools/raw/`. The project already has a Gemini key in `.env` as `GEMINI_API_KEY`; read it from the environment or from `.env` inside the script only. **Check Google's current documentation and use the current image-generation model** (do not assume a model name). Image generation can need billing; if the API refuses or is rate-limited, stop, and do the fallback below.
- **Fallback (no API access):** write `docs/image-prompts.md` with every prompt and its target filename so I can generate the images in Google AI Studio myself and drop them in `tools/incoming/`. `tools/process_images.py` must work on files from either path.
- `tools/process_images.py` (Pillow) — center-crop to 16:10, resize to 1080×675, convert to WebP quality about 80, target 60 to 120 KB each, write to `app/src/main/res/drawable-nodpi/` using the naming in section 5. It must fail loudly on wrong size, oversize files (over 150 KB) or a blank image.
- `tools/contact_sheet.py` — generates `docs/catalog-contact-sheet.html`: a grid of all 70 images with id, alt text and file size, so a human can review everything quickly.
- `docs/image-credits.md` — model name, generation date and the prompt for each image (provenance and regeneration).

**Save time:** start generation in the background as soon as `tools/image_prompts.json` is ready, and continue with the content and UI work while it runs.

**Style guide (every image):** photorealistic editorial photo, bright natural daylight, warm neutral colour grade, shallow depth of field, clean modern Indian home in a tier-2 city (light vitrified or marble floors, white walls, steel and glass fixtures), 16:10 landscape with the subject centred and safe margins (cards crop by height). One consistent look across all 70 images.

**Prompt template:**
`Photorealistic editorial photograph of {scene}. Modern Indian home interior in Agra, natural window daylight, soft warm neutral colour grade, shallow depth of field, sharp focus on the subject, 16:10 landscape composition with the subject centred. Worker wears a plain unbranded charcoal uniform. No text, no letters, no logos, no brand names, no watermark.`
Add a negative list where the API supports it: `text, watermark, logo, cartoon, illustration, 3D render, oversaturated, distorted hands, extra fingers, deformed tools, blurry`.

**Content rules for images:**
- The scene must show the actual service done correctly and safely: technician with a waterproof jacket bag and bucket under a split AC during foam jet wash; electrician in insulated gloves using a multimeter at an open switchboard with the mains isolated; plumber with a pipe wrench and a dry basin; pest technician in mask and gloves with a hand sprayer along a skirting board; painter with a roller and floor drop sheets and furniture covered; RO technician replacing a filter cartridge; and so on. Write each `scene` yourself, specific and accurate.
- Prefer hands, tools, back views and close-ups. Where faces appear they must look natural; no children's faces; nothing that resembles a real person or celebrity.
- No brand logos or recognisable product packaging (AC, RO, washing machine and chimney brands included). No visible phone numbers or text. No before/after claims.
- Salon photos must be tasteful and fully clothed; facial services show a client relaxing with eyes closed, product-free close-ups of hands for nails.
- Category covers are distinct from the four service photos in that category: a wider, more atmospheric scene of the category as a whole.

**Quality check (keep it quick):** rely on the automated checks in `process_images.py` (dimensions, size, not blank). Do not inspect or regenerate images one by one; I will review `docs/catalog-contact-sheet.html` myself and ask for regenerations by id. List in `docs/image-issues.md` only the images the API failed to produce.

**Existing 19 JPGs:** look at each one. Reuse an existing image only if it matches its service and the new style; convert it to WebP under the new name. Move the rest out of `res/` to `tools/legacy_images/` (do not delete anything). The result must remove at least 8 MB from the packaged resources.

**Budget:** all new images together ≤ 8 MB, and the APK must not grow compared with today.

**Honesty in the UI:** AI images are illustrative. Show a small "Representative image" caption on the service detail screen. Do not use the images in a way that implies they are the actual professional or the customer's real job.

## 7. UI WIRING

1. **Home Categories** (`HomeScreen.kt`): replace the 8 hard-coded icon tiles with photo tiles built from `categories` (cover from `CategoryImages.coverFor`): rounded cover, category name over a dark gradient scrim (text contrast at least 4.5:1), starting price. Show the first 8 and a "See all" tile that opens a new `AppScreen.AllCategories` with a 2-column photo grid of all 14. Remove the `?: categories.firstOrNull()` fallbacks and fix the id mismatches (`cat_plumbing` to `cat_plumber`, Painting to `cat_painting`). An unknown category id must do nothing and log a warning, not open AC Repair.
2. **Category to services:** tapping a tile selects that category and opens the Services tab list (`onCategoryClick` in `MainActivity.kt` ~line 457 already calls `viewModel.selectCategory(cat.id)` and sets `currentTab = ServoraNavTab.SERVICES`). The bottom nav is now Home, Chats, Services, Profile; check the current enum in `ServoraBottomNav.kt` before you rely on any tab name. Add a horizontally scrollable category chip row (All + 14) at the top of `ExploreScreen` and keep it in sync with `selectedCategoryId`.
3. **Service cards** (`ServiceExploreCard`): unique photo, rating, duration, starting price, popular badge.
4. **Service detail:** hero photo with the "Representative image" caption, then the existing sections (included, not included, packages, FAQs) now populated for every service.
5. **Search overlay:** thumbnail per result; search across service name, subtitle, category name and a few keywords.
6. **Booking flow, confirmation, tracking and My Bookings:** show a small service thumbnail, looked up by `serviceId` (fall back to the category cover, then to the neutral drawable).
7. **Performance and accessibility:** `Image(painterResource(...), contentScale = ContentScale.Crop)` with `contentDescription = service.imageAlt` (or the service name when empty), stable LazyColumn/LazyGrid keys, no image decoding outside visible items. Build layouts that hold up at 360 dp and 411 dp widths and font scale 1.3, and give tiles TalkBack labels (I will do the testing).
8. Add explicit `INTERNET` and `ACCESS_NETWORK_STATE` permissions (section 1).

## 8. PHASES

- **Phase 0 — Audit and plan.** Confirm sections 1 facts (category count, service count, image references, image dimensions and aspect ratio of the current images, how card image heights crop). Output the plan.
- **Phase 1 — Restructure without new content.** Create `data/catalog/`, move the 6 existing services, the 4 professionals and categories, add `CatalogDsl`, `CategoryImages`, `imageAlt` and the id fixes. Behaviour unchanged.
- **Phase 2 — Content for all 56 services** and the 6 new professionals, written to the standards in section 3, plus `docs/catalog-placeholders.md`.
- **Phase 3 — Images.** Build the pipeline, generate (or produce prompts for) all 70 images, process them, produce the contact sheet, run the quick quality check, and wire `imageDrawableRes` for every service.
- **Phase 4 — UI wiring** (section 7).
- **Phase 5 — Wrap-up.** Make sure everything compiles and write the short final report.

## 9. MANUAL TEST CHECKLIST (I will run this myself — do not write tests)

1. All 14 categories are reachable from Home (8 tiles plus See all) and each opens the right list.
2. Every category shows 4 services with their own photo; each detail page has full content and the "Representative image" caption.
3. Prices, packages and included/excluded lists look sensible; nothing says "guaranteed" or makes medical claims.
4. Search shows thumbnails; booking screens show the service thumbnail.
5. The contact sheet (`docs/catalog-contact-sheet.html`) looks right; I will name any image ids to regenerate.

## 10. ACCEPTANCE CRITERIA

1. Every category opens a list of 4 services, each with its own photo, full details and a working detail page; no category is empty.
2. All 14 categories are reachable from Home (8 tiles plus See all); no tile opens the wrong category.
3. 70 images exist as WebP in `drawable-nodpi`, each 150 KB or smaller, none reused across services; the contact sheet and credits files exist.
4. The APK is no larger than before; the old unused JPGs are moved out of `res/`, not deleted.
5. No API key and no phone number appears in new code, docs or resources, and the Android app never reads `GEMINI_API_KEY`.
6. `assembleDebug` passes; `docs/catalog-placeholders.md` lists all invented data (ratings, review counts, warranties, staff names).
7. The service detail screen shows "Representative image".

## 11. OUTPUT FORMAT

One short report at the very end (15 lines max): files changed, assumptions, images I should review or regenerate by id, and anything I must do myself (API billing, keys).
