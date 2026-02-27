# LIFEPAD

[![CI](https://github.com/T0pd0gsg0dp0T/LIFEPAD/actions/workflows/ci.yml/badge.svg)](https://github.com/T0pd0gsg0dp0T/LIFEPAD/actions/workflows/ci.yml)

> A privacy-first, all-in-one Android app that unifies knowledge management, CBT-enhanced journaling, and intelligent personal finance tracking.

LIFEPAD is a spyware-free alternative to multiple productivity apps, built with modern Android development practices. It connects your notes, journal entries, and financial transactions through a unified graph view powered by hashtags and wikilinks, revealing hidden patterns across all aspects of your life.

## Features

### 📝 Notepad
- **Markdown-powered notes** with Markwon rendering
- **Wikilinks** (`[[Note Title]]`) for connecting ideas
- **Checklist mode** for todo lists with check/uncheck functionality
- **Folder organization** for structured note-taking
- **Full-text search** (FTS4) for instant retrieval
- **Backlinks panel** showing incoming and outgoing connections

### 📔 Journal
- **Mood tracking** with visual indicators and statistics
- **Template-based entries**: Free-form, Gratitude, Reflection, Savoring, Check-in, Food Journal
- **CBT Integration**:
  - **Thought Journal**: 6-step cognitive restructuring wizard (situation → emotions → thoughts → evidence → reframe → re-rate)
  - **Exposure Journal**: SUDS ratings (0-100) for anxiety exposure therapy
  - **12 thinking traps** with descriptions and examples
  - **Named emotions** with 0-100 intensity ratings
- **Mental Health Assessments**: GAD-7 (anxiety) and PHQ-9 (depression) with history and trend charts
- **Journaling Statistics**:
  - Streak tracking with visual badges
  - Word count analytics
  - Template distribution charts
  - Mood calendar heatmap
- **Export**: JSON, CSV, or Markdown format with date range selection
- **Reminders**: WorkManager-backed notifications for journaling prompts

### 💰 Finance
- **Transaction tracking** with income/expense categorization
- **Custom categories** with colors, icons, sorting, and archiving
- **Account management** for multiple financial accounts
- **Budgets** with monthly/yearly periods
- **Recurring bill detection**: Automatic pattern recognition using statistical analysis
- **Safe-to-spend calculator**: Daily allowance after bills and goal contributions
- **Cashflow forecasting**: Day-by-day projection of account balance
- **Financial goals**: Savings, debt payoff, emergency fund tracking
- **Net worth tracking**: Manual asset/liability entry with monthly snapshots and trend charts
- **Budget templates**: 50/30/20, Zero-Based, Pay Yourself First
- **Actionable insights**: Budget alerts, upcoming bills, spending trends, goal progress
- **Transaction-to-journal linking**: Connect spending with emotional context

### 🔗 Graph View
- **Force-directed graph** visualization using Canvas
- **Cross-module connections** via hashtags (#tagname)
- **Wikilink relationships** between notes
- **Interactive exploration** of your personal knowledge graph

### 🔍 Unified Search
- **Cross-module search** across notes, journal entries, and transactions
- **Hashtag-based filtering** to find related items
- **Full-text search** for notes and journal entries
- **Real-time results** as you type

### 📊 Dashboard
- **Recent items** from all modules
- **Quick stats**: note count, journal streak, safe-to-spend amount
- **At-a-glance overview** of your day

### 🔒 Security & Privacy
- **SQLCipher encryption**: Database encrypted at rest
- **App lock**: Auto-lock when backgrounded
- **No cloud sync**: All data stays on your device
- **No analytics or tracking**: Completely spyware-free
- **Backup/restore**: Manual export and import via Settings

### 📎 Attachments
- **File attachments** for notes, journal entries, and transactions
- **Image support** with Coil image loading
- **Cross-module linking** via unified attachment system

## Tech Stack

- **Language**: Kotlin 2.1
- **UI**: Jetpack Compose with Material 3
- **Architecture**: Clean Architecture (UI → ViewModel → Repository → DAO)
- **Database**: Room ORM + SQLCipher + FTS4 full-text search
- **Dependency Injection**: Hilt
- **Navigation**: Jetpack Compose Navigation
- **Charts**: MPAndroidChart
- **Markdown**: Markwon
- **Serialization**: kotlinx-serialization
- **Background Tasks**: WorkManager
- **Image Loading**: Coil

## Getting Started

### Prerequisites

- **Java 17** (SDKMAN recommended)
- **Android SDK** with:
  - Platform: `android-35`
  - Build tools: `35.0.0`
- **Gradle 8.10.2** (wrapper included)

### Build

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires keystore.properties or uses debug signing)
./gradlew assembleRelease

# Install to connected device
./gradlew installDebug
```

### Test

```bash
# Unit tests
./gradlew test

# Single test class
./gradlew testDebugUnitTest --tests "com.lifepad.app.NoteViewModelTest"

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

### Lint

```bash
./gradlew lint
```

### Security Scanning

```bash
# OWASP dependency check (requires NVD_API_KEY for auto-update)
./gradlew dependencyCheckAnalyze
```

## Project Structure

```
app/src/main/kotlin/com/lifepad/app/
├── LifepadApplication.kt       # Hilt Application with WorkManager
├── MainActivity.kt             # Single Activity with NavHost
├── di/                         # Hilt modules (Database, WorkManager)
├── data/
│   ├── local/
│   │   ├── entity/             # 21 Room entities
│   │   ├── dao/                # 17 Room DAOs
│   │   ├── migration/          # Database migrations (v1→v8)
│   │   └── LifepadDatabase.kt  # DB version 8
│   └── repository/             # 14+ repositories
├── domain/
│   ├── parser/                 # Hashtag, Wikilink, Checklist parsers
│   ├── cbt/                    # ThinkingTrap, Emotion, StructuredData
│   ├── finance/                # RecurringBillDetector, SafeToSpendCalculator, etc.
│   ├── export/                 # JSON, CSV, Markdown exporters
│   └── assessment/             # GAD-7, PHQ-9 questions
├── navigation/                 # ~30 route definitions
├── ui/theme/                   # AMOLED black + deep purple + matrix green
├── components/                 # 20+ shared components
├── dashboard/
├── notepad/
├── journal/                    # CBT, assessments, stats, export
├── finance/                    # Bills, goals, net worth, insights
├── search/
├── settings/                   # Backup/restore, security
├── notification/               # ReminderScheduler
└── security/                   # AppLifecycleObserver
```

## Database Schema

LIFEPAD uses Room ORM with SQLCipher encryption (version 8):

- **notes** / **notes_fts**: Markdown notes with full-text search
- **backlinks**: Wikilink relationships
- **journal_entries** / **journal_entries_fts**: Mood-tracked entries with templates
- **entry_emotions**: Emotions with 0-100 intensity ratings
- **entry_thinking_traps**: CBT thinking traps
- **transactions**: Income/expense records
- **budgets**: Budget categories with amounts and periods
- **recurring_bills**: Auto-detected recurring expenses
- **goals**: Financial goals with progress tracking
- **assets**: Asset/liability tracking
- **net_worth_snapshots**: Monthly net worth history
- **assessments**: GAD-7 and PHQ-9 mental health assessments
- **reminders**: WorkManager-backed notifications
- **attachments**: File attachments for all item types
- **hashtags**: Shared tag definitions
- **hashtag_usage**: Cross-module junction table
- **folders**: Note organization
- **categories**: Transaction categories
- **accounts**: Financial accounts

## Development Phases

- ✅ **Phase 1**: MVP (71 files, DB v1) - Core modules, navigation, search
- ✅ **Phase 2**: Graphs & Connections (82 files, DB v2) - Backlinks, graph view, budgets, mood charts
- ✅ **Phase 3**: Enhanced Journaling (103 files, DB v3) - Templates, assessments, checklists, reminders
- ✅ **Phase 4**: CBT Integration (115 files, DB v4) - Thought journal, exposure journal, emotions, thinking traps
- ✅ **Phase 5**: Financial Intelligence (135 files, DB v5) - Bills, goals, net worth, cashflow, insights
- ✅ **Phase 6**: Attachments (DB v6) - File attachments with Coil
- ✅ **Phase 7**: Enhanced Categories (DB v7) - Category types and colors
- ✅ **Phase 8**: Category Management (DB v8) - Sorting, archiving, backup/restore, app lock

## Key Design Patterns

### Hashtag Sync
```kotlin
1. HashtagParser.extractHashtags(content)
2. hashtagDao.syncHashtagsForItem(itemType, itemId, hashtagNames)
3. Junction table auto-updates
```

### Wikilink Sync
```kotlin
1. WikilinkParser.extractWikilinks(content)
2. noteDao.getNoteByTitle(title) // Resolve to IDs
3. backlinkDao.updateBacklinksForNote(sourceId, targetIds)
```

### Structured Journal Data
```kotlin
- JournalEntryEntity.structuredData stores JSON
- Sealed classes: ThoughtRecordData, ExposureData, etc.
- Separate emotion/thinking trap tables
```

### Financial Intelligence
```kotlin
- RecurringBillDetector: interval median + coefficient of variation
- SafeToSpendCalculator: (balance - bills - goals) / days
- CashflowForecaster: day-by-day projection
- InsightGenerator: actionable insight cards
```

## Contributing

This is a personal project, but issues and pull requests are welcome. Please ensure:
- Code follows Kotlin coding conventions
- All tests pass (`./gradlew test`)
- No new lint errors (`./gradlew lint`)
- Database migrations are properly tested

## Privacy & Ethics

LIFEPAD is designed with privacy as a core principle:
- **No network requests**: All data stays local
- **No analytics**: No usage tracking or telemetry
- **No third-party SDKs**: Only open-source libraries
- **Encrypted storage**: SQLCipher protects data at rest
- **Open source**: Full code transparency

## License

This project is provided as-is for personal use. See LICENSE file for details.

## Acknowledgments

Built with:
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Room](https://developer.android.com/training/data-storage/room)
- [Hilt](https://dagger.dev/hilt/)
- [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)
- [Markwon](https://github.com/noties/Markwon)
- [SQLCipher](https://www.zetetic.net/sqlcipher/)
- [Coil](https://coil-kt.github.io/coil/)

Inspired by the need for privacy-respecting productivity tools that work offline and keep your data yours.
