# Phase 1 — Handbook analysis and content architecture

## Handbooks (2,146 pages)
| Handbook | Pages | Structure | Interview material |
|---|---|---|---|
| Python Automation Engineer | 1,053 | 43 chapters, 12 projects, 180-day plan, cheat sheets | ~150 Q&As with follow-ups and common wrong answers (Ch 39–41), 105 coding problems with tested solutions (Ch 42), mock interviews (Ch 43), 43 chapter assessments |
| API & Protocol Automation | 467 | 27 chapters, 3 projects, glossary | 160 Q&As (Ch 25), 30 scenarios (Ch 26), capstone assessment and revision flashcards (Ch 27) |
| Linux, Bash & Embedded Debugging | 288 | 31 chapters, 3 projects, glossary | 160 tagged Q&As (Ch 29), 30 scenarios (Ch 30), 20 case studies (Ch 27), 40-question assessment (Ch 31) |
| Git / Jenkins / CI-CD | 240 | 34 chapters, 3 projects, glossary | 59 Q&As (Ch 31–32), 32 embedded-CI scenarios (Ch 33), 20 MCQs + revision flashcards (Ch 34) |
| BMC/SSD Interview Prep (resume) | 98 | 24 sections | Most-likely questions, 33 scenarios, self-intro, HR, cross-questioning, top-50 revision, full mock interview, scorecard; ON RESUME / IMPLIED / NOT ON RESUME tags |

## Tracks and topics (merged across handbooks)
1. Python Core — Py 1–23, 36–39, BMC §10
2. Test Automation — pytest (Py 25 · API 19 · CI 15), mocking (Py 26), test design (API 3 · BMC §6), framework (Py 31), logging/reporting (Py 30 · CI 22), Playwright (Py 29), reliability (Py 34 · API 21), CLI automation (Py 28 · Linux 23)
3. REST & Redfish — API 1–8, 22, Py 27
4. Embedded Protocols — IPMI (API 9–10), RMCP+ (API 11), PLDM (API 12–13), MCTP (API 14–15), UART (API 16 · Linux 21 · CI 25), SSH (API 17), I²C (API 18), HW automation (Py 33), protocol briefing (BMC §8)
5. BMC / OpenBMC — architecture (Linux 17 · BMC §7), D-Bus (Linux 18), firmware update (Linux 19 · CI 24), health (Linux 20), projects (API 23–24)
6. SSD / NVMe — BMC §9
7. Linux & Bash — Linux 1–16, 24–25
8. Git & CI/CD — CI 1–30, Py 24, Py 32, BMC §11
9. Debugging & RCA — Linux 22, 26–28, API 20
10. Coding Interview — Py 35, 42, projects P1–P12
11. Interview & Career — BMC §§1–5, 12–24, Py 40, 43, final revision chapters, question and scenario banks

The full chapter→topic map is `content-pipeline/topics.py`.

## Overlaps resolved
pytest (4 sources), Redfish (4), Jenkins/Git (4), UART (3), firmware update (3), troubleshooting
scenarios (5): merged into single topics; each lesson keeps its own source. Duplicate questions
across handbooks are merged, keeping the first answer and listing the others as alternate sources.

## Gaps
Selenium (handbook uses Playwright), SPI, system design (no chapter), SSD/NVMe depth (one source),
KCS (interview level only), personal metrics (the resume has none; the app leaves [adapt] markers).

## Knowledge model
Track → Topic → Lesson (markdown-lite body) · InterviewQuestion · Flashcard · QuizItem (8 types) ·
Scenario (labelled steps) · CodingChallenge (statement, examples, hint, solution, tests) · Glossary,
each with SourceRef {book, chapter, section, page}.
