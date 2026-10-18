# Build status and verification

## Verified in the build environment
- **Content pipeline**: runs end to end on the five PDFs and regenerates `content.json`.
- **Traceability**: every question, flashcard, lesson, scenario and coding problem's text was found
  on its cited PDF page (±1 page). 506 of 528 source-backed quiz items matched; the rest were
  glossary-table rows, fixed by excluding the Linux "Final Project Index" from the glossary.
- **Code outputs**: 141 "predict the output" items reproduce their handbook output when executed
  (CPython 3.13); 7 that didn't reproduce were dropped.
- **Coding challenges**: all 105 reference solutions pass all 253 test asserts through the same
  `runner.py` harness the app uses on-device.
- **Curated quiz items**: all 185 have a supporting quote found verbatim in the handbooks.
- **Kotlin**: `core/model`, `core/domain`, the markdown parser and the search-query builder were
  compiled with kotlinc 2.0.21 and their 44 unit tests pass.
- **Content schema**: `content.json` was validated field-by-field against the Kotlin DTOs.

- **Whole app type-check**: every main source file of every module (all screens, ViewModels,
  repositories, Room entities/DAOs, DI modules, MainActivity, the Chaquopy runner) compiles with
  kotlinc 2.0.21 against hand-written stubs of the Compose, Material 3, Navigation, Room, Hilt,
  DataStore, Activity and Chaquopy APIs that mirror the real signatures, plus the real
  kotlinx-coroutines and kotlinx-serialization libraries. The bundled content.json was decoded with
  the app's own DTOs and mappers: 0 errors, 0 orphan references, every quiz item valid for its type.

## Not verified here
The build environment could not download the Android SDK or Google/Maven Central artifacts, so the
real Gradle build (AGP, Compose compiler plugin, Room/Hilt annotation processing via KSP, Chaquopy)
and the app itself were **not run** here. Versions used: AGP 8.7.3, Kotlin 2.0.21 +
KSP 2.0.21-1.0.28, Compose BOM 2024.12.01 (Material 3 1.3.1), Room 2.6.1, Hilt 2.52,
Navigation 2.8.5, Chaquopy 16.0.0. If the first sync or build reports errors (most likely from
annotation processing or a library signature that differs from the stubs), send the output and
they can be fixed quickly.

## Known limitations (v1)
- Offline answer scoring is keyword/coverage based, not semantic. It is labelled as approximate.
- No AI features (by choice for v1). The "Ask AI" entry points were left out rather than shown disabled.
- No notifications (no permission requested). Streak and daily goal are shown on Home instead.
- Some long code lines in lessons keep the PDF's line wrapping (lesson display only; quiz and coding
  code was repaired and verified by execution).
- Selenium, SPI and system design aren't covered by the handbooks and are labelled as such.
- SSD/NVMe has a single detailed source (BMC/SSD prep §9).
- Python version on device is 3.12 (Chaquopy); a few handbook examples target 3.14 features.

## Phase checklist
| Phase | State |
|---|---|
| 1 Analyse PDFs, content architecture | Done (docs/CONTENT_REPORT.md) |
| 2 Android project architecture | Done |
| 3 Home + Learn | Written |
| 4 Quiz + Flashcards | Written |
| 5 Interview + Scenario practice | Written |
| 6 Progress + Weak topics | Written |
| 7 AI features | Skipped in v1 (your choice) |
| 8 UI polish | Written (light/dark theme, accessibility labels) — needs on-device review |
| 9 Testing | JVM tests pass here; instrumented tests written, to run on an emulator |
| 10 Production build | Release build type configured (R8, resource shrinking); needs your signing key |
