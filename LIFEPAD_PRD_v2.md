# LIFEPAD
## Knowledge Graph Journal + Finance Manager with Mindmaps
### Android Application — Product Requirements Document

**Version:** 1.0 – Initial Release  
**Scope:** Feature-rich comprehensive release  
**Storage:** Local only (SQLite, no cloud sync)  
**Target User:** Personal use; personal workflows (not enterprise)

---

## 1. Overview

LIFEPAD is a unified personal productivity and wellness application combining:

- **Knowledge graph notepad** with bidirectional linking (Obsidian inspired)
- **CBT-based journaling** with mood/thought mindmaps via hashtags (Unstuck inspired)
- **Personal finance management** linked to emotional & mental contexts

The app operates as a **unified knowledge ecosystem** where hashtags (#mood, #anxiety, #spending_patterns) create emergent mindmaps across all three modules. Notes, journal entries, and transactions are interconnected through backlinks and tags, allowing you to visualize relationships between ideas, emotional states, and spending behaviors. The graph view reveals hidden patterns: which thoughts correlate with spending spikes? Which hashtags cluster together? How do moods, ideas, and finances intersect?

---

## 2. Core Architecture & Tech Stack

### 2.1 Platform & Framework

- **Target:** Android 11+ (API level 30+)
- **Language:** Kotlin (primary)
- **UI Framework:** Jetpack Compose (modern declarative UI)
- **Database:** Room ORM + SQLite (local only, optimized for graph queries)

### 2.2 Key Libraries

- **Room:** Persistent local storage with reactive LiveData/Flow
- **Jetpack Navigation:** Fragment-less navigation (Compose native)
- **Hilt:** Dependency injection for clean architecture
- **Markdown support:** Markwon (rendering) + EditText integration
- **Graph visualization:** Graphview or custom Canvas-based renderer (network graph rendering)
- **Charts:** MPAndroidChart (mood trends, spending patterns)

---

## 3. Notepad Module (Knowledge Base with Backlinks)

### 3.1 Core Notepad Features

#### Wikilink-Style Linking

- **Internal links:** `[[Note Title]]` syntax creates bidirectional references
- **Backlinks:** every note shows "Linked from" section listing all notes that reference it
- **Link creation:** typing `[[` opens autocomplete of existing notes
- **Link navigation:** tap link to jump to referenced note
- **Unlinked mentions:** suggest hashtags/keywords that could become links but aren't yet

#### Markdown Editing

- Full Markdown support with live preview toggle
- Rich formatting: bold, italic, lists, code blocks, links, headings
- Auto-save on keystroke (debounced)
- Undo/redo stack

#### Hashtag Support

- Type `#tagname` to create tags within notes
- Tags automatically tracked and indexed
- Hashtag suggestions while typing (autocomplete)
- Hashtags clickable: view all notes with that tag

#### Note Organization

- Nested folder structure for organizing notes
- Pin notes to favorites for quick access
- Sorting: by date modified, created, name (A-Z), custom order
- Notes list with preview snippets

#### Search & Discovery

- Full-text search across all notes
- Filter by hashtag
- Find broken links (references to non-existent notes)
- Recent notes, recently modified

#### Core Actions

- Create, edit, delete, duplicate, rename notes
- Word/character count
- Inline images: embed images with `![alt text](path/to/image)`
- Export notes: plaintext, Markdown, PDF

### 3.2 Graph View (Obsidian's "Graph" Feature)

#### Network Visualization

- **Graph canvas:** visual network showing notes as nodes, links as edges
- **Node appearance:** size based on number of backlinks (popular notes = larger)
- **Node coloring:** optional color by hashtag (all #mood notes same color)
- **Zoom/pan:** pinch to zoom, drag to pan across graph
- **Tap nodes:** tap to jump to note or show stats

#### Link Analysis

- Show link strength (how many times do two notes reference each other?)
- Path finding: show shortest path between two notes
- Orphaned notes: highlight notes with no links
- Strongly connected clusters: visual detection of topic clusters

#### Interactive Filtering

- Filter graph by hashtag (show only #work notes and their connections)
- Filter by date range (show notes created in last 30 days)
- Show/hide orphaned notes
- Show connection strength threshold (hide weak links)

---

## 4. Journal Module (CBT Journaling with Hashtag Mindmaps)

### 4.1 Core Journal Features

#### Entry Creation & Editing

- Create new journal entry with auto-timestamp
- Rich text editing with Markdown support
- Edit metadata: date, time, mood, hashtags
- Auto-save on keystroke (debounced)

#### CBT Templates & Guided Prompts

- **Thought Records:** Problem → Thought → Feeling → Behavior → Response (structured fields)
- **Cognitive Reframing:** prompts for challenging negative thoughts (#anxiety #reframe)
- **Daily Reflection:** morning/evening prompts (customizable)
- **Gratitude Log:** dedicated template (#gratitude #thankful)
- **Goal Tracking:** set, track, and reflect on progress (#goal #20250315)
- **Custom Templates:** create user-defined prompt templates

#### Mood & Emotional Tracking

- Mood selector: 1-10 scale or emoji picker
- Mood automatically tagged (#mood_7, #mood_anxious)
- Emotion tags: #anxiety, #sadness, #anger, #calm, #joy, #overwhelmed
- Physical sensations: #headache, #fatigue, #restless

#### Hashtag-Based Organization

- Use hashtags for themes: #work, #relationship, #health, #finances
- Create mood/context clusters: entries with #anxiety tend to correlate with #procrastination
- Hashtag suggestions while typing
- Hashtag browsing: view all entries with a specific tag across all modules

#### Entry Discovery

- Browse by date: calendar view or chronological list
- Search entries: by keyword, date range, mood, hashtags
- View entries by hashtag (with backlinks showing related notes/transactions)
- Pin entries for quick reference

#### Statistics & Insights

- Writing streaks: consecutive days journaled
- Entry count: total, this month, this week
- Mood summary: most common moods, mood trends over time
- Hashtag frequency: which themes dominate your journaling
- Co-occurrence analysis: which hashtags appear together (#anxiety often with #sleep_deprivation?)

### 4.2 Graph View for Journal (Emotional Mindmap)

#### Mood Network Visualization

- **Nodes:** each hashtag or mood state
- **Edges:** entries that contain multiple hashtags (shows relationships)
- **Graph analysis:** which moods cluster together? Which precede others?
- **Temporal view:** show how emotional graph changes over time

#### Pattern Detection

- Highlight recurring patterns: "Whenever you have #anxiety, you also note #insomnia"
- Show mood cycles: identify weekly/monthly patterns
- Correlation visualization: which hashtags most frequently co-occur?

### 4.3 Advanced Journal Features

- Voice-to-text entry option (Android native Speech-to-Text)
- Photo/image attachments in entries
- Export entries: PDF, CSV (with date, mood, hashtags)
- Entry versioning: track edits with timestamps
- Notification reminders: daily journal prompt at customizable time
- Link to transactions: "Today spent $X on #coffee after noting #stress" (link entry to transaction)

---

## 5. Finance Module (Personal Finance with Contextual Links)

### 5.1 Core Finance Features

#### Transaction Logging

- Quick transaction entry: amount, type (income/expense), category, description, date
- Auto-timestamp transactions; manually adjust if needed
- Edit/delete transactions with optional history tracking

#### Categories

- **Pre-defined:** Food, Transport, Utilities, Entertainment, Healthcare, Shopping, Work, Savings, Investments, Subscriptions, Other
- **Custom categories:** create user-defined categories
- **Category icons** for visual scanning

#### Hashtag Support for Transactions

- Add hashtags to transactions: #coffee #work #impulse #savings
- Link to journal entries: when creating/editing transaction, suggest recent journal entries with matching hashtags
- Mood context: see what mood you were in when spending (#spent_after_bad_day)

#### Recurring Transactions

- Set up recurring transactions: daily, weekly, monthly, yearly
- Auto-create transactions on scheduled dates
- Manage recurring: edit, pause, delete

#### Balance & Account Tracking

- Current balance: running total
- Multi-account support: checking, savings, cash, wallet
- Account switching: seamlessly view transactions per account

#### Budget Management

- Set monthly/weekly budgets per category
- Budget progress: visual bars per category
- Alerts: notify when approaching/exceeding limits

#### Reports & Analytics

- Monthly summary: income, expenses, net
- Category breakdown: pie chart
- Trend analysis: line charts over time
- Income vs. Expense comparison
- Date range filtering for custom analysis

#### Search & Filtering

- Filter by category, date range, amount, description, hashtag
- Search transactions by keyword

### 5.2 Advanced Finance Features

- Savings goals: set targets and track progress
- Receipt attachments: photo capture with transactions
- Multi-currency support
- Net worth tracking: total assets minus liabilities
- Tax categorization: mark as tax-deductible or business
- Export to CSV/PDF

### 5.3 Graph View for Finance (Spending Mindmap)

#### Transaction Network

- **Nodes:** categories or hashtags
- **Edges:** spending flows (X spent on #coffee this month)
- **Node size:** amount spent in that category
- **Temporal slicing:** see how spending network changes month-to-month

#### Contextual Linking

- Hover/tap transaction → see if it's linked to a journal entry
- Show mood context: "You spent $80 on shopping when mood was #stress"
- Reveal patterns: which emotional states correlate with overspending?

---

## 6. Unified Graph View (The Mindmap Core)

### 6.1 Global Mindmap

#### Integrated Knowledge Graph

- **Nodes:** notes, journal entries, transactions, hashtags
- **Edges:** wikilinks, hashtag references, entry-to-transaction links
- **Visualization:** all three modules in one network graph
- **Filtering:** show only specific hashtag clusters (e.g., #health + related notes/entries/spending)

#### Example Queries the Graph Answers

- "Show me all #anxiety entries, related notes (via backlinks), and spending during those periods"
- "Which ideas (#work notes) correlate with financial transactions?"
- "What's my mood when I spend on #entertainment vs #necessities?"
- "Which hashtags are most central to my knowledge graph?"

#### Interactive Exploration

- Tap a node → drill into that note/entry/transaction
- Double-tap → expand to show 2nd-degree connections
- Draw path: find connections between two arbitrary entries
- Highlight all entries with specific hashtags

### 6.2 Dashboard

- **Today's summary:** entries created, mood today, spending today, hashtag distribution
- **Recent items:** latest 3–5 notes, entries, transactions
- **Quick stats:** writing streak, balance, most common mood
- **Graph preview:** miniature version of global graph showing today's activity
- **Quick-add buttons:** create note, journal entry, or transaction

### 6.3 Unified Search

- Search across all three modules: notes, journal entries, transactions
- Full-text search with relevance ranking
- Filter results by type (note/entry/transaction) or hashtag
- See results in context: "this entry links to 3 notes, 2 transactions"

### 6.4 Hashtag System (The Central Organizing Principle)

#### Cross-Module Tagging

- All hashtags shared across notes, journal, finance
- Typing `#` in any module suggests existing hashtags
- Create hashtags organically (no pre-defined list required)
- Hashtag browsing: view all items with that tag, their connections

#### Suggested Hashtags

- While journaling: suggest recent note/transaction hashtags
- While noting: suggest related hashtags from other notes
- While spending: suggest journal hashtags from today

#### Hashtag Intelligence

- **Frequency:** see which hashtags dominate
- **Co-occurrence:** which hashtags appear together most often?
- **Temporal patterns:** are certain hashtags time-bound? (#budget_review in month-end?)
- **Mood correlation:** which hashtags associate with which moods?

---

## 7. Data Model & Database Schema

### 7.1 Core Tables

#### Note
- `id` (PRIMARY KEY, auto-increment)
- `title` (TEXT, indexed)
- `content` (TEXT, full-text searchable)
- `folderId` (FOREIGN KEY to Folder)
- `createdAt`, `updatedAt` (timestamps)
- `isPinned` (BOOLEAN)

#### Backlink (for `[[wikilink]]` references)
- `id` (PRIMARY KEY)
- `sourceNoteId` (FOREIGN KEY to Note)
- `targetNoteId` (FOREIGN KEY to Note)
- `createdAt` (timestamp)

#### JournalEntry
- `id` (PRIMARY KEY, auto-increment)
- `content` (TEXT, full-text searchable)
- `mood` (INTEGER: 1–10)
- `template` (TEXT: thought_record, gratitude, reflection, custom, free)
- `entryDate` (DATE, indexed)
- `createdAt`, `updatedAt` (timestamps)
- `isPinned` (BOOLEAN)
- `linkedTransactionIds` (JSON array or junction table)

#### Transaction
- `id` (PRIMARY KEY, auto-increment)
- `amount` (DECIMAL)
- `type` (ENUM: INCOME, EXPENSE)
- `categoryId` (FOREIGN KEY to Category)
- `description` (TEXT)
- `accountId` (FOREIGN KEY to Account)
- `transactionDate` (DATE, indexed)
- `createdAt`, `updatedAt` (timestamps)
- `isRecurring` (BOOLEAN)
- `linkedEntryId` (FOREIGN KEY to JournalEntry, nullable)
- `receiptPath` (TEXT, optional)

#### Hashtag
- `id` (PRIMARY KEY)
- `name` (TEXT, unique, indexed)
- `usageCount` (INTEGER for sorting by frequency)
- `createdAt` (timestamp)

#### HashtagUsage (junction table)
- `id` (PRIMARY KEY)
- `hashtagId` (FOREIGN KEY to Hashtag)
- `itemType` (ENUM: NOTE, ENTRY, TRANSACTION)
- `itemId` (INTEGER)
- `createdAt` (timestamp)

#### Supporting Tables
- `Folder`, `Category`, `Account`, `RecurringTransaction`, `Budget`, `SavingsGoal`

---

## 8. User Interface & Navigation

### 8.1 Main Navigation

- **Bottom navigation bar** (Jetpack Compose): Dashboard, Notepad, Journal, Finance, Graph, Settings
- State preserved when switching tabs

### 8.2 Notepad Screen

- Top: folders filter panel (collapsible)
- Left sidebar: folder tree, recent notes, pinned notes
- Main: notes list with title, preview, last modified
- Fab: create new note
- Swipe actions: delete, pin, share

### 8.3 Note Editor Screen

- Top: note title, save status
- Toolbar: undo/redo, find, link inserter (`[[`), hashtag inserter (`#`)
- Editor: full-height Markdown input
- Backlinks panel: collapsible section showing "Linked from" notes
- Bottom: word count, hashtag cloud from this note

### 8.4 Journal Screen

- Calendar view with entry dots
- Chronological list grouped by date/week
- Entry preview: mood emoji, date, excerpt, hashtags
- Fab: create new entry (template selector dialog)
- Filter bar: mood, hashtag, date range

### 8.5 Finance Screen

- Header: balance, account selector
- Summary cards: income, expenses, net, budget status
- Charts: pie (categories), line (trends)
- Transactions list: newest first, filterable
- Fab: create new transaction

### 8.6 Graph View Screen (Central Feature)

- **Full-screen interactive graph:**
  - Nodes colored by hashtag or item type
  - Nodes sized by importance (link count, entry frequency)
  - Zoom/pan controls
  - Legend showing node types/colors
  
- **Search/filter bar:**
  - Filter by hashtag: typing `#anxiety` shows only those nodes
  - Filter by type: show only notes, or only transactions, etc.
  - Date range filter
  
- **Info panel (tappable node):**
  - Shows node details
  - Lists connections
  - Quick link to open the actual item
  
- **Statistics sidebar:**
  - Most connected hashtags
  - Most popular notes
  - Strongest correlations (which hashtags appear together most?)

---

## 9. Implementation Priority & Phasing

### 9.1 Phase 1 (Core MVP – Weeks 1–3)

- **Notepad:** basic CRUD, Markdown editing, hashtags, wikilinks (no backlink panel yet)
- **Journal:** entry creation, mood tracking, hashtags, basic templates
- **Finance:** transaction logging, categories, hashtags, balance display
- **Search:** unified search, hashtag filtering
- **Database:** Room schemas, hashtag junction tables

### 9.2 Phase 2 (Graphs & Connections – Weeks 4–5)

- **Notepad:** backlinks panel, link suggestions
- **Journal:** mood statistics, hashtag frequency analysis
- **Finance:** budget management, transaction-entry linking
- **Graph view:** basic network visualization (notes + wikilinks only)
- **Dashboard with activity summary**

### 9.3 Phase 3 (Integrated Mindmap – Weeks 6–7)

- **Global graph:** all three modules in one network
- **Advanced filtering:** hashtag clusters, temporal slicing
- **Pattern detection:** hashtag co-occurrence analysis
- **Export:** PDF, CSV
- **Notifications, settings, theming**

---

## 10. Technical Considerations

### 10.1 Graph Rendering

- **Graph library:** Graphview or custom Canvas implementation
- **Node limit:** handle 500+ nodes gracefully (clustering, LOD)
- **Edge bundles:** use edge bundling for clarity when many connections
- **Performance:** debounce pan/zoom, lazy-load graph data

### 10.2 Full-Text Search

- SQLite FTS5 (Full-Text Search) for notes, entries, descriptions
- Hashtag indexed separately for fast filtering

### 10.3 Data Integrity

- Foreign key constraints enforced
- Orphaned backlinks cleaned up when notes deleted
- Transactions for multi-step operations

### 10.4 Memory Management

- Lifecycle-aware observers to prevent leaks
- Limit in-memory caching; paginate large lists
- Graph data loaded on-demand (don't render all 500 nodes at once)

---

## 11. Testing & QA

### 11.1 Unit Tests

- ViewModel logic: filtering, sorting, hashtag parsing
- Repository: CRUD, backlink queries, hashtag frequency
- Graph algorithms: path finding, clustering detection

### 11.2 Integration Tests

- Database: Room DAO tests, junction table operations
- Graph building: create notes/links, verify graph structure
- End-to-end: create note → add wikilink → verify backlink appears

### 11.3 UI Testing

- Navigation between tabs
- Graph interaction: zoom, pan, node selection
- Markdown rendering with links
- Hashtag suggestions while typing

---

## 12. Acceptance Criteria & Success Metrics

The application is considered complete when:

- All features listed in Phases 1 & 2 are implemented and tested
- Graph view renders 500+ nodes without lag
- Wikilinks create bidirectional backlinks correctly
- Hashtags are shared and indexed across all three modules
- Full-text search works across all content types
- Dashboard aggregates data from all modules
- Unit and integration tests achieve 70%+ code coverage
- Graph visualization reveals meaningful patterns (e.g., mood correlations with spending)

---

## 13. Key Differentiators

This app is not just a notes app + journal + finance tracker. It's a **knowledge system** where:

1. **Hashtags are the organizing principle**, not rigid folders
2. **The graph reveals hidden connections** between your ideas, moods, and spending
3. **Backlinks create a web of meaning** rather than linear documents
4. **Context flows between modules**: you see why you spent money by reading journal entries linked to transactions
5. **Emergent structure**: organize through organic tagging, then use graph view to discover unexpected patterns

Example power use: "Show me my mood mindmap for #work entries, the notes I created during #stress, and my spending during those times. What patterns emerge?"

---

**— End of PRD —**
