# Notes Manager (Course Manager) — Release Notes & Project Knowledge

## What this is
A fully local, offline Android app for organizing study material — folder-based file management, a built-in PDF viewer, notes, a manual timetable, and a study task scheduler. No cloud sync, no accounts, no backend.

## Honesty note on how this was built
This project was **vibe-coded** — I (Muneeb) didn't write the implementation code myself. The workflow was:
- **Claude** handled architecture decisions, feature scoping, debugging diagnosis, and wrote tightly-scoped implementation prompts.
- **DeepSeek** generated the actual Kotlin code from those prompts.
- I built, tested on-device, and reported errors back for the next round.

I'm saying this upfront because I think it matters — this was as much a project about learning how to direct and debug an AI-assisted build as it was about the app itself. I didn't fake writing this from scratch, and I'm not going to pretend otherwise in the README of my own repo.

## Tech Stack
- **Language / UI:** Kotlin, Jetpack Compose, Material3
- **Local database:** Room (SQLite), schema version 8 at release
- **File access:** Android Storage Access Framework (SAF) — no broad storage permission requested
- **PDF rendering:** `io.github.oothp:android-pdf-viewer` (PDFium-backed)
- **Image loading:** Coil 3
- **Preferences:** Jetpack DataStore
- **Navigation:** Navigation Compose 2.x
- **Reminders:** `AlarmManager.setExactAndAllowWhileIdle()` + a `BroadcastReceiver` that reschedules itself for weekly-recurring timetable reminders, plus a `BootReceiver` that restores all scheduled alarms after a device reboot (since `AlarmManager` alarms are cleared on restart)
- **Build:** AGP 9 (built-in Kotlin compiler, no separate `kotlin-android` plugin), KSP for Room's annotation processing

## Architecture
- MVVM per screen: each screen has a `ViewModel` (or `AndroidViewModel` where `Context` is needed for `AlarmManager`), exposing a single `StateFlow<UiState>`
- Repository layer wraps every Room DAO — screens never touch DAOs directly
- `AppDatabase` is a single Room database with cascading foreign-key deletes across the hierarchy (deleting a Semester cascades down through Courses → Categories → Items)
- Two separate notification channels (`class_reminders`, `study_reminders`) so the two reminder types can be muted independently
- Onboarding branches into three tracks (University / Inter / Matric) driven by a `SubjectTemplates` object that resolves the correct subject list and default category set per education level, part/grade, and group

## Data model
- `Semester` → `Course` → `Category` → `Item` (files, notes, or grouped photos)
- `Page` — individual photos belonging to a `PHOTO_GROUP` item
- `QuickNote` — standalone notes outside the folder hierarchy
- `TimetableEntry` — weekly recurring class schedule
- `StudyTask` — one-off dated tasks with optional reminders
- `Bookmark` — schema exists in the database but has no UI wired to it in this release

## What's in this release
- Folder hierarchy + onboarding (University/Inter/Matric flows)
- File import via SAF, photo-group notes, in-category and global quick notes
- Native PDF viewer (night mode, page/scroll toggle, last-read-page memory)
- Photo group viewer
- Manual timetable with weekly recurring reminders
- Manual study task list with one-shot reminders
- Day/night theme
- Navigation drawer shell

## What was deliberately cut or never built
Being upfront about the gap between the original plan and what shipped:
- **AI-assisted timetable parsing** (OCR + LLM extraction from an uploaded timetable PDF/photo) — this was the original centerpiece AI feature. It was descoped in favor of manual timetable entry to remove the API dependency and OCR-accuracy risk from v1.
- **General AI document summarization** — considered early on, dropped before development started as unnecessary scope/cost risk.
- **Share-current-file feature** — was in the original roadmap, never implemented in this build.
- **Bookmarks in the PDF viewer** — the database table exists, but there's no button or screen to actually create/view a bookmark yet.
- **App launcher icon** — currently no custom app icon; the manifest points at a system placeholder.

## Known rough edges
- Several screens catch errors into UI state but don't always visibly render them to the user — failures can be silent in a few spots.
- The overall visual design was never given a single deliberate styling pass — the UI evolved reactively while fixing bugs, not from one design decision.

## Build & signing
- Release builds are signed via a `keystore.properties` file (gitignored) referencing a local `.jks` keystore kept outside the repository.
- Lint is disabled for release builds (`checkReleaseBuilds = false`) to keep build times reasonable on lower-spec hardware.

---
Built by Muneeb, directed and debugged with Claude, implemented with DeepSeek. GG.
