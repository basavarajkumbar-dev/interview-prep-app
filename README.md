# Interview Prep — Android app built from your five handbooks

An offline Android app (Kotlin, Jetpack Compose, Material 3) that turns the Python Automation,
API & Protocol, Linux/Bash/Embedded Debugging, Git/Jenkins/CI-CD and BMC/SSD interview-prep
handbooks into lessons, quizzes, flashcards, scenarios, coding practice and mock interviews.

| Content (bundled, offline) | Count | Where it comes from |
|---|---|---|
| Lessons | 1,920 | Every numbered handbook section, in 94 topics / 11 tracks |
| Interview Q&As | 1,277 | Chapter "Interview questions", the 160-question banks, Python Ch 39–41, resume handbook |
| Flashcards | 609 | Chapter assessments + answer keys, revision-flashcard tables, BMC "Top 50" |
| Quiz items | 697 | 8 types — see below |
| Troubleshooting scenarios | 145 | API Ch 26, Linux Ch 27/30, CI/CD Ch 33, BMC §12 |
| Coding challenges | 105 (253 test asserts) | Python Ch 42, every reference solution executed and passing |
| Glossary terms | 160 | API, Linux and CI/CD glossaries |

Every item stores **source handbook, chapter, section and PDF page**; the app shows a *Source* button.
An automated check confirmed that every question, flashcard, lesson, scenario and coding problem
text appears on its cited page (±1).

Quiz types: multiple choice (292), fill in the blank (152, from assessment answers), code output
(141 — each snippet was **executed** and kept only if the output matched the handbook), match the
following (39, glossary pairs), true/false (24), scenario (22), multiple answer (17), debugging (10).
Items marked *source-backed* are entirely handbook text. The 185 hand-written items have a
handbook answer plus app-written distractors; the build fails unless each one's supporting quote
is found verbatim in the PDFs.

## Build and run

Requirements: Android Studio Ladybug (2024.2) or newer, JDK 17, Android SDK 35, internet for the
first Gradle sync.

1. Open this folder in Android Studio (File → Open → `InterviewPrep`).
2. Let Gradle sync (it downloads AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01, Room 2.6.1,
   Hilt 2.52 and Chaquopy 16.0.0).
3. Run the `app` configuration on a device or emulator (Android 8.0 / API 26+).
   Command line: `./gradlew :app:assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
4. Release build: `./gradlew :app:assembleRelease` (signed with the debug key for personal use —
   add your own signing config before publishing).

First launch imports the bundled content into a local database (a few seconds), then shows a
short onboarding (name, target roles, daily study time).

### Embedded Python (Coding Practice)

Coding Practice runs your Python on the phone with [Chaquopy](https://chaquo.com/chaquopy/)
(`app/src/main/python/runner.py`), executing the handbook's own test asserts and reporting
**Passed X/Y** with failure messages and a time limit against infinite loops. It adds ~25 MB per
ABI. If Chaquopy gives you trouble, remove `alias(libs.plugins.chaquopy)` and the `chaquopy { }`
block from `app/build.gradle.kts`, delete `ChaquopyRunner.kt`'s class, and make
`CodeRunnerModule` return `ReferenceOnlyRunner()` — the screen then switches to
compare-with-reference mode.

## What's in the app

Bottom navigation: **Home · Learn · Practice · Quiz · Progress**

- **Home** — greeting, today's goal (minutes), streak, level/XP, today's lessons/quiz score/answers,
  *Continue learning*, quick actions, readiness score, recommended topic, weak area, Daily
  Challenge, "What should I study today?" plan, likely questions and due flashcards.
- **Learn** — track → topic tree with completion and mastery (🔴🟡🟢), role filter, search.
  Topic screen has **Simple / Interview** modes: *Simple* shows the handbook's plain-language
  chapter intro, objectives, key points and common mistakes; *Interview* shows model answers.
  Lessons render code and program output in readable blocks (light and dark), with
  *Mark as learned*, *Next lesson*, bookmark and source.
- **Practice** — question bank (filters: role, difficulty, type, "from my resume", search),
  question detail with *Think before revealing*, optional typed answer with offline scoring,
  "How to answer in interview", follow-ups; mock interview (role, level, 10/20/30 questions,
  per-answer feedback, final report); scenarios (5-dimension evaluation + ideal flow); coding
  practice; 15-minute revision; weak topics; bookmarks; study plan.
- **Quiz** — Daily Interview Challenge (10 questions / 10 minutes), adaptive quiz, weak-topic
  quiz, quiz by topic, flashcards (SM-2 spaced repetition: Know / Review / Don't know), revision.
- **Progress** — interview readiness (0–100) with its six components explained, completion per
  track, quiz accuracy, streak, answered/skipped/bookmarked, mock/coding/scenario scores,
  14-day study chart, level and achievements, per-topic mastery.
- **Settings** — name, target roles, theme (system/light/dark), daily goal (30/45/60/90 min),
  quiz difficulty (adaptive or fixed), export progress (JSON), reload content, reset progress,
  source handbooks. No notifications are sent and no notification permission is requested.

### Personal answers and honesty rules

Only the BMC/SSD handbook (built from your resume) produces "how YOU answer" content, and it keeps
that handbook's tags: **On your resume** (must defend), **Implied by your role**, **Not on your
resume** (learn basics, answer honestly). Everywhere else the app shows the *concept* answer plus
the handbook's answer structure (Python Ch 39.1) and reminds you to add only real experience.
Gaps are labelled "Not covered in the uploaded handbook" (Selenium, SPI, system design).

### Offline scoring

Typed interview and scenario answers are scored offline against the handbook answer's key terms
(with stemming and synonyms), structure and concreteness. The UI labels this as approximate.
AI features (Gemini) are intentionally not in v1; the architecture leaves room for them.

## Architecture

```
app ─────────────── MainActivity, NavHost, bottom bar, Hilt, Chaquopy CodeRunner
feature/home        dashboard
feature/learn       learn tree, topic, lesson reader, search, bookmarks
feature/quiz        quiz engine UI, daily challenge, quiz hub, 15-min revision
feature/flashcards  spaced-repetition cards
feature/interview   practice hub, question bank, mock interview, scenarios
feature/coding      coding list + editor + test runner UI
feature/progress    progress, readiness, weak topics, achievements, study plan
feature/settings    settings, sources, onboarding
core/designsystem   theme (light/dark), components, markdown-lite renderer, routes
core/data           Room (content.db + progress.db), DAOs, repositories, importer, DataStore
core/domain         pure Kotlin: SM-2, adaptive quiz, grading, answer scoring, mastery,
                    readiness, streak, planner, XP/achievements, session composer
core/model          pure Kotlin models
build-logic         Gradle convention plugins
content-pipeline    PDF → JSON pipeline (Python)
```

- **MVVM + repository**: ViewModels (Hilt) expose `StateFlow` UI state; repositories wrap Room DAOs.
- **Two databases**: `content.db` is rebuilt from `assets/content/content.json` whenever its
  `version` changes; `progress.db` holds all user data and is never touched by content updates
  (and is the unit to sync when cloud backup is added). Room schemas are exported to
  `core/data/schemas` for migrations.
- **Content separate from UI**: screens render data; nothing is hard-coded in composables.
- **Performance**: lists use `LazyColumn`; lesson bodies are parsed into blocks and rendered lazily;
  list queries never load lesson bodies; large pools (quiz, cards) are cached in memory once;
  one shared progress snapshot feeds every screen.
- **Search**: SQLite FTS4 index over topics, lessons, questions, cards, scenarios, code and glossary.

## Content pipeline

`content-pipeline/run_pipeline.sh /path/to/pdfs` (needs `pip install pymupdf pdfplumber`):

1. `extract.py` — reads fonts and positions: code (monospace, with indentation), bold, headings,
   tables; drops running headers/footers.
2. `structure.py` — chapters → sections → paragraphs / code / output / tables, with page numbers.
3. `build_content.py` — topic map (`topics.py` merges overlapping chapters), lessons, Q&A parsing
   (answer / follow-up / common-wrong-answer / why / key points), flashcards, scenarios, coding
   problems + tests, glossary, auto quiz items, curated quiz items with evidence check.
4. `verify.py` — executes every code-output snippet and every coding solution with its tests,
   repairing PDF line-wraps; drops anything that doesn't reproduce.

Name the PDFs `python.pdf api.pdf linux.pdf cicd.pdf bmcssd.pdf`. Bump `"version"` in
`build_content.py` when you change content so installed apps re-import it.

## Tests

- `core/domain` + markdown parser + search query: 44 JVM unit tests (spaced repetition, quiz
  grading for all 8 types, answer & scenario scoring, mastery/weak topics, adaptive selection,
  readiness, streak, study planner, XP/achievements, mock-interview composition, code checks,
  markdown, FTS query building). Run: `./gradlew :core:domain:test :core:designsystem:testDebugUnitTest :core:data:testDebugUnitTest`.
- `core/data` instrumented tests: Room DAOs (bookmarks, read/unread, quiz stats, active days) and
  FTS search. `app` instrumented navigation test across all tabs.
  Run: `./gradlew connectedDebugAndroidTest` with an emulator.

## Status

See `docs/STATUS.md` for what was verified where, and known limitations.
