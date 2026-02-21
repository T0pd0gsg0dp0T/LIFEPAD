# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LIFEPAD is an Android application (Kotlin + Jetpack Compose) that unifies knowledge management, CBT journaling, and personal finance tracking into a single interconnected ecosystem. The central differentiator is a **unified graph view** where hashtags and wikilinks surface hidden patterns across all three modules (e.g., mood correlations with spending).

**Status:** Phase 1 (MVP) implemented
**Full specification:** `LIFEPAD_PRD_v2.md`

## Tech Stack

- **Platform:** Android 11+ (API 30+), Kotlin 2.1
- **UI:** Jetpack Compose with Material 3, bottom nav bar
- **Database:** Room ORM + SQLite with FTS4 for full-text search
- **DI:** Hilt
- **Navigation:** Jetpack Compose Navigation (type-safe routes)
- **Markdown:** Markwon (rendering)
- **Charts:** MPAndroidChart (for Phase 2)

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests (requires device/emulator)
./gradlew testDebugUnitTest --tests "com.lifepad.app.NoteViewModelTest"  # Single test
./gradlew lint                   # Android lint
```

## Local Release Install Notes

- `installRelease` uses `signingConfigs.debug` so release builds can be installed locally.
- If the release app immediately closes, ensure SQLCipher isn't stripped by R8:
  - Proguard keep rules were added for `net.sqlcipher.**` in `app/proguard-rules.pro`.
  - Release startup crash symptom: `NoSuchFieldError mNativeHandle` in `net.sqlcipher.database.SQLiteDatabase`.
- R8 build error for Tink was resolved by adding `com.google.errorprone:error_prone_annotations`
  as `compileOnly` (see `gradle/libs.versions.toml` and `app/build.gradle.kts`).

## Project Structure

```
app/src/main/kotlin/com/lifepad/app/
├── LifepadApplication.kt       # Hilt Application
├── MainActivity.kt             # Single Activity with NavHost
├── di/
│   └── DatabaseModule.kt       # Hilt database providers
├── data/
│   ├── local/
│   │   ├── entity/             # Room entities (11 total)
│   │   ├── dao/                # Room DAOs (9 total)
│   │   ├── Converters.kt
│   │   └── LifepadDatabase.kt
│   └── repository/             # NoteRepository, JournalRepository, etc.
├── domain/parser/
│   ├── HashtagParser.kt        # #tagname extraction
│   └── WikilinkParser.kt       # [[Note Title]] extraction
├── navigation/
│   ├── Screen.kt               # Route definitions
│   ├── BottomNavBar.kt
│   └── LifepadNavHost.kt
├── ui/theme/                   # Material 3 theming
├── components/                 # Shared components (HashtagChip, MoodSelector)
├── dashboard/
├── notepad/
├── journal/
├── finance/
└── search/
```

## Architecture

Clean Architecture: **UI (Compose) → ViewModel → Repository → Room DAO**

### Database Schema

| Table | Key Purpose |
|-------|-------------|
| `notes` / `notes_fts` | Notes with FTS search |
| `backlinks` | Wikilink relationships (source → target) |
| `journal_entries` / `journal_entries_fts` | Mood-tracked entries with templates |
| `transactions` | Income/expense records |
| `hashtags` | Shared tag definitions |
| `hashtag_usage` | **Cross-module junction table** (itemType: NOTE/ENTRY/TRANSACTION) |
| `folders`, `categories`, `accounts` | Organization |

### Key Patterns

**Hashtag Sync (on save):**
1. Parse content with `HashtagParser.extractHashtags()`
2. Call `hashtagDao.syncHashtagsForItem(itemType, itemId, hashtagNames)`
3. Junction table updates automatically

**Wikilink Sync (notes only):**
1. Parse content with `WikilinkParser.extractWikilinks()`
2. Resolve note titles to IDs via `noteDao.getNoteByTitle()`
3. Call `backlinkDao.updateBacklinksForNote(sourceId, targetIds)`

**Unified Search:**
- FTS4 on notes and journal entries
- LIKE query on transactions
- Hashtag search via `searchDao.searchByHashtag()`

## Implementation Status

### Completed (Phase 1)
- [x] Room database with all entities, DAOs, FTS
- [x] HashtagUsage junction table for cross-module linking
- [x] Content parsers (hashtag, wikilink)
- [x] All repositories with hashtag/wikilink sync
- [x] Dashboard with recent items and stats
- [x] Notepad: list, editor with wikilink autocomplete
- [x] Journal: list, editor with mood selector, CBT templates
- [x] Finance: list, editor with category selector
- [x] Unified search across all modules
- [x] Bottom navigation

### Remaining (Phase 2-3)
- [ ] Backlinks panel in note editor
- [ ] Graph view visualization
- [ ] Budget management
- [ ] Transaction-to-journal linking UI
- [ ] Mood statistics and charts
- [ ] Export (PDF/CSV)
- [ ] Notifications
