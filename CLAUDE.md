# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

LIFEPAD is an Android application (Kotlin + Jetpack Compose) that unifies knowledge management, CBT journaling, and personal finance tracking into a single interconnected ecosystem. The central differentiator is a **unified graph view** where hashtags and wikilinks surface hidden patterns across all three modules (e.g., mood correlations with spending).

**Status:** Phases 1-8 complete (~165 files, DB version 8)
**Full specification:** `LIFEPAD_PRD_v2.md`

## Tech Stack

- **Platform:** Android 11+ (API 30+), Kotlin 2.1
- **UI:** Jetpack Compose with Material 3, bottom nav bar
- **Database:** Room ORM + SQLite with FTS4 for full-text search
- **DI:** Hilt (includes WorkManager integration via hilt-work)
- **Navigation:** Jetpack Compose Navigation (type-safe routes)
- **Markdown:** Markwon (rendering and editing)
- **Charts:** MPAndroidChart (mood trends, finance charts, assessment history)
- **Security:** SQLCipher (encrypted database), AndroidX Security Crypto
- **Serialization:** kotlinx-serialization (structured journal data)
- **Background Tasks:** WorkManager (reminders and notifications)
- **Image Loading:** Coil (attachments)

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests (requires device/emulator)
./gradlew testDebugUnitTest --tests "com.lifepad.app.NoteViewModelTest"  # Single test
./gradlew lint                   # Android lint
```

## Build & Release Notes

### Local Release Builds
- `installRelease` uses `signingConfigs.debug` so release builds can be installed locally
- CI/production builds require `keystore.properties` in project root
- Release builds use R8 minification with ProGuard rules

### Known Build Issues (Resolved)
- **SQLCipher R8 stripping:** Proguard keep rules added for `net.sqlcipher.**` in `app/proguard-rules.pro`
  - Symptom: `NoSuchFieldError mNativeHandle` crash on startup
- **R8 Tink dependency:** `com.google.errorprone:error_prone_annotations` added as `compileOnly`
- **Room schema export:** KSP arguments configured for schema location at `app/schemas/`

### Environment Requirements
- **Java 17** (SDKMAN recommended: `source ~/.sdkman/bin/sdkman-init.sh`)
- **Android SDK** at `$ANDROID_HOME` or via `local.properties`
  - Platform: `android-35`
  - Build tools: `35.0.0`
- **Gradle 8.10.2** (wrapper included)

## Project Structure

```
app/src/main/kotlin/com/lifepad/app/
├── LifepadApplication.kt       # Hilt Application with WorkManager
├── MainActivity.kt             # Single Activity with NavHost
├── di/
│   ├── DatabaseModule.kt       # Hilt database providers
│   └── WorkerModule.kt         # WorkManager configuration
├── data/
│   ├── local/
│   │   ├── entity/             # Room entities (21 total)
│   │   ├── dao/                # Room DAOs (17 total)
│   │   ├── migration/          # Database migrations (v1→v8)
│   │   ├── Converters.kt
│   │   └── LifepadDatabase.kt  # DB version 8
│   └── repository/             # 14+ repositories
├── domain/
│   ├── parser/                 # HashtagParser, WikilinkParser, ChecklistParser
│   ├── cbt/                    # ThinkingTrap, Emotion, StructuredData
│   ├── finance/                # RecurringBillDetector, SafeToSpendCalculator, etc.
│   ├── export/                 # JSON, CSV, Markdown exporters
│   └── assessment/             # GAD-7, PHQ-9 questions
├── navigation/
│   ├── Screen.kt               # ~30 route definitions
│   ├── BottomNavBar.kt
│   └── LifepadNavHost.kt
├── ui/theme/                   # AMOLED black + deep purple + matrix green
├── components/                 # 20+ shared components
├── dashboard/
├── notepad/
├── journal/                    # Includes CBT, assessments, stats, export
├── finance/                    # Includes bills, goals, net worth, insights
├── search/
├── settings/                   # Backup/restore, security settings
├── notification/               # ReminderScheduler
└── security/                   # AppLifecycleObserver
```

## Architecture

Clean Architecture: **UI (Compose) → ViewModel → Repository → Room DAO**

### Database Schema (Version 8)

| Table | Key Purpose |
|-------|-------------|
| `notes` / `notes_fts` | Notes with FTS search, optional checklist mode |
| `backlinks` | Wikilink relationships (source → target) |
| `journal_entries` / `journal_entries_fts` | Mood-tracked entries with templates, structured CBT data |
| `entry_emotions` | Emotions with 0-100 intensity ratings |
| `entry_thinking_traps` | CBT thinking traps linked to journal entries |
| `transactions` | Income/expense records, optional journal linkage |
| `budgets` | Budget categories with amounts and periods |
| `recurring_bills` | Detected recurring expenses for cashflow forecasting |
| `goals` | Financial goals (savings, debt payoff, emergency fund) |
| `assets` | Asset/liability tracking for net worth |
| `net_worth_snapshots` | Monthly net worth history |
| `assessments` | GAD-7 and PHQ-9 mental health assessments |
| `reminders` | WorkManager-backed notifications (journal, bills, etc.) |
| `attachments` | File attachments for notes/entries/transactions |
| `hashtags` | Shared tag definitions |
| `hashtag_usage` | **Cross-module junction table** (itemType: NOTE/ENTRY/TRANSACTION/BILL) |
| `folders` | Note organization |
| `categories` | Transaction categories with type, color, sort order, archive flag |
| `accounts` | Financial accounts |

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

**Structured Journal Data (Phase 4+):**
- `JournalEntryEntity.structuredData` stores JSON via kotlinx-serialization
- Sealed class hierarchy: `ThoughtRecordData`, `ExposureData`, `SavoringData`, `CheckInData`, `FoodJournalData`
- Separate tables for emotions (`entry_emotions`) and thinking traps (`entry_thinking_traps`)
- Detail screens check entry type to render appropriate read-only view

**Financial Intelligence (Phase 5):**
- `RecurringBillDetector`: analyzes transaction history, detects patterns via interval median + coefficient of variation
- `SafeToSpendCalculator`: `(balance - future bills - goal contributions) / days remaining`
- `CashflowForecaster`: day-by-day projection with bill/income occurrences
- `InsightGenerator`: actionable cards (budget alerts, bills due, spending trends, goal progress)
- `BudgetTemplateEngine`: applies preset budget strategies (50/30/20, Zero-Based, Pay Yourself First)

**Reminders & Notifications:**
- `ReminderEntity` with `linkedItemType` (JOURNAL/NOTE/BILL/ASSESSMENT)
- `ReminderScheduler` uses WorkManager for periodic/one-time notifications
- `HiltWorkerFactory` for DI in background workers
- `AppLifecycleObserver` for app lock on background/foreground transitions

## Common Gotchas & Solutions

### Kotlin Coroutines
- ❌ **Don't** use `.collect()` directly in `init {}` blocks - it blocks the coroutine
- ✅ **Do** use separate `viewModelScope.launch` for each flow collection
- ❌ **Don't** use `combine(arrayOf(...))` with mixed-type flows (requires same type parameter)
- ✅ **Do** use 5-parameter `combine()` overload (max 5 flows)

### Room DAOs
- ❌ **Don't** use `return` inside expression body functions
- ✅ **Do** use block body: `fun delete(id: Long) { dao.delete(id) }`

### Compose Deprecations
- `SearchBar`: use overload with `inputField` parameter
- `Icons.Filled.Note`, `Icons.Outlined.Note`, `Icons.Outlined.Article`: use `AutoMirrored` versions
- `statusBarColor` in Theme.kt: deprecated, use modern window insets

### Database Migrations
- Migration path: v1 → v2 → v3 → v4 → v5 → v6 → v7 → v8
- Always export schemas via KSP arg: `room.schemaLocation`
- Test migrations with Room's `MigrationTestHelper` before release

## Implementation Status

### ✅ Phase 1: MVP (71 files, DB v1)
- Room database with entities, DAOs, FTS
- HashtagUsage junction table for cross-module linking
- Content parsers (hashtag, wikilink)
- Dashboard with recent items and stats
- Notepad: list, editor with wikilink autocomplete
- Journal: list, editor with mood selector, 4 basic templates
- Finance: list, editor with category selector
- Unified search across all modules
- Bottom navigation

### ✅ Phase 2: Graphs & Connections (82 files, DB v2)
- Enhanced backlinks panel (collapsible, outgoing links)
- Force-directed graph view with Canvas
- Budget management (entity, DAO, editor screen)
- Transaction-to-journal linking UI
- Mood statistics with MPAndroidChart (line + bar charts)

### ✅ Phase 3: Enhanced Journaling, Assessments & Reminders (~103 files, DB v3)
- 4 new finance categories (Rent, Phone, WiFi, AI)
- 4 new journal templates (Savoring, Exposure, Check-in, Food Journal)
- Mood calendar heatmap (Canvas-based composable)
- Journaling stats (streaks, word counts, template distribution)
- GAD-7 & PHQ-9 mental health assessments with history + trend charts
- Notes as checklists (toggle, check/uncheck, add/remove items)
- Reminders & notifications (WorkManager + HiltWorkerFactory)

### ✅ Phase 4: ClearMind CBT Integration (~115 files, DB v4)
- kotlinx-serialization for structured journal data
- 12 CBT thinking traps with display names, descriptions, examples
- Named emotions with 0-100 intensity, 15 presets
- Thought Journal: 6-step wizard (situation → emotions → thoughts → evidence → reframe → re-rate)
- Exposure Journal: structured form with SUDS 0-100 ratings
- Enhanced Savoring: before/after emotion tracking
- Enhanced Journal Home: greeting, quick action cards, streak badge
- Detail screens: read-only views for thought_record and exposure entries
- Enhanced Insights: thinking trap frequency + emotion frequency charts
- Export: JSON, CSV, Markdown via FileProvider share intent

### ✅ Phase 5: Cashflow, Planning & Intelligence (~135 files, DB v5)
- Recurring bill detection (heuristic: interval median, CV <10%)
- Safe-to-spend engine (daily allowance calculation)
- Cashflow forecaster (day-by-day projection)
- Financial goals with progress tracking
- Net worth tracking with manual assets/liabilities, monthly snapshots
- Budget templates: 50/30/20, Zero-Based, Pay Yourself First
- Actionable insight cards (budget alerts, bills due, spending trends, goal progress)
- Dashboard safe-to-spend widget

### ✅ Phase 6: Attachments (DB v6)
- AttachmentEntity for linking files to notes/entries/transactions
- Coil image loading support

### ✅ Phase 7: Enhanced Categories (DB v7)
- Category type (INCOME/EXPENSE) and color customization

### ✅ Phase 8: Category Management (DB v8)
- Category sort order and archive functionality
- Settings screen with backup/restore
- SQLCipher encrypted database
- App lock with lifecycle observer

### 🎯 Design & UX
- AMOLED black + deep purple + matrix green color scheme
- Leaf-quill launcher icon (black bg, deep purple circle, matrix green quill)
- Material 3 design system throughout
