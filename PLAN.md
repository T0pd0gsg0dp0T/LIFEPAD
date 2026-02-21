# LIFEPAD Phase 5: "Enhanced Finance" — Implementation Plan

## Overview
Single-phase release with DB migration v4→v5. ~40 new files, ~9 modified files.

### Feature Groups
**Cashflow & Planning:** Recurring bills detection, safe-to-spend engine, cashflow forecasting, financial goals
**Intelligence & Insights:** Actionable insight cards, budget templates (50/30/20, zero-based, pay-yourself-first), net worth tracking with manual assets & trend snapshots

---

## New Database Entities (4 new tables)

### `recurring_bills`
| Column | Type | Notes |
|--------|------|-------|
| id | INTEGER PK | autoincrement |
| name | TEXT | "Netflix", "Rent" |
| amount | REAL | bill amount |
| transactionType | TEXT | "EXPENSE" or "INCOME" (for recurring income in forecasts) |
| categoryId | INTEGER? | FK → categories (SET NULL) |
| accountId | INTEGER? | FK → accounts (SET NULL) |
| frequency | TEXT | MONTHLY / WEEKLY / BIWEEKLY / YEARLY |
| nextDueDate | INTEGER | epoch ms |
| dayOfMonth | INTEGER? | 1-31 for monthly bills |
| isConfirmed | BOOLEAN | user-confirmed vs auto-detected |
| isEnabled | BOOLEAN | default true |
| reminderId | INTEGER? | FK → reminders (for bill reminder) |
| detectedFromCount | INTEGER | how many transactions matched heuristic |
| notes | TEXT | default '' |
| createdAt/updatedAt | INTEGER | timestamps |

### `goals`
| Column | Type | Notes |
|--------|------|-------|
| id | INTEGER PK | autoincrement |
| name | TEXT | "Emergency Fund", "Vacation" |
| type | TEXT | SAVINGS / DEBT_PAYOFF / EMERGENCY_FUND |
| targetAmount | REAL | goal target |
| currentAmount | REAL | progress (manually updated) |
| monthlyContribution | REAL | feeds safe-to-spend formula |
| deadline | INTEGER? | optional epoch ms |
| accountId | INTEGER? | FK → accounts (SET NULL) |
| notes | TEXT | default '' |
| isCompleted | BOOLEAN | default false |
| createdAt/updatedAt | INTEGER | timestamps |

### `assets`
| Column | Type | Notes |
|--------|------|-------|
| id | INTEGER PK | autoincrement |
| name | TEXT | "Home", "Car" |
| value | REAL | current value |
| assetType | TEXT | PROPERTY / VEHICLE / INVESTMENT / SAVINGS / OTHER |
| isLiability | BOOLEAN | true = debt/loan |
| notes | TEXT | default '' |
| createdAt/updatedAt | INTEGER | timestamps |

### `net_worth_snapshots`
| Column | Type | Notes |
|--------|------|-------|
| id | INTEGER PK | autoincrement |
| snapshotDate | INTEGER UNIQUE | epoch ms, start of month |
| accountsTotal | REAL | sum of account balances |
| assetsTotal | REAL | sum of manual assets |
| liabilitiesTotal | REAL | sum of liabilities |
| netWorth | REAL | computed total |
| createdAt | INTEGER | timestamp |

---

## Domain Logic (6 new files in `domain/finance/`)

### RecurringBillDetector
Heuristic: groups EXPENSE transactions by normalized description, computes interval median, detects MONTHLY (25-35 days), WEEKLY (6-8), BIWEEKLY (13-16), YEARLY (360-370). Requires 2+ matches with <10% amount variance.

### SafeToSpendCalculator
Formula: `availableBuffer = netBalance - futureMonthBills - goalContributions`
`dailyAllowance = availableBuffer / daysRemainingInMonth`

### CashflowForecaster
Projects balance forward day-by-day for 7/14/30 days, adding recurring income and subtracting bills at their due dates. Outputs `List<ForecastPoint(dayIndex, date, balance)>`.

### InsightGenerator
Computes from existing data: budget alerts (>85% used), bills due soon (<3 days), spending trend (vs last month), top category, savings achieved, goal progress. Returns `List<FinancialInsight>` with severity (INFO/WARNING/POSITIVE).

### BudgetTemplateEngine
- **50/30/20:** Needs (Food, Rent, Utilities, Transport, Healthcare, Phone, WiFi) = 50%, Wants (Entertainment, Shopping, Subscriptions, AI, Other) = 30%, Savings = 20%
- **Zero-based:** User allocates 100% across categories via sliders
- **Pay yourself first:** Savings gets 20%, remainder split across expense categories

### Data classes
`DetectedBill`, `SafeToSpendResult`, `ForecastPoint`, `FinancialInsight`, `InsightType`, `InsightSeverity`, `TemplateBudgetItem`

---

## New DAOs (4), Repositories (4), ViewModels (8)

### DAOs
- `RecurringBillDao` — CRUD + getConfirmed + getBillsUpToDate + bills-with-category JOIN
- `GoalDao` — CRUD + getActive + getTotalMonthlyContributions
- `AssetDao` — CRUD + getAssets/getLiabilities + totals
- `NetWorthSnapshotDao` — upsert + getAll + getForPeriod + getLatest

### Repositories
- `RecurringBillRepository` — bill CRUD + reminder scheduling via existing WorkManager infra
- `GoalRepository` — goal CRUD + progress computation
- `NetWorthRepository` — combines accounts + assets - liabilities, monthly snapshot logic
- `InsightRepository` — orchestrates InsightGenerator with data from multiple DAOs

### ViewModels
- `RecurringBillsViewModel` — bill list + detection trigger
- `RecurringBillEditorViewModel` — bill create/edit form (SavedStateHandle pattern)
- `GoalsViewModel` — goal list + aggregate progress
- `GoalEditorViewModel` — goal create/edit form
- `NetWorthViewModel` — assets/liabilities + snapshots + auto-snapshot on month change
- `AssetEditorViewModel` — asset create/edit form
- `SafeToSpendViewModel` — combines net balance + bills + goals → calculator + forecaster
- `FinanceInsightsViewModel` — generates insight list

### Modified ViewModels
- `FinanceStatsViewModel` — add insights field
- `DashboardViewModel` — add safe-to-spend daily allowance (separate flow, avoids combine arity issue)

---

## New UI Screens (9)

1. **RecurringBillsScreen** — auto-detected candidates (with Confirm/Dismiss), confirmed bills list, scan button
2. **RecurringBillEditorScreen** — name, amount, type, category, frequency, next due date, reminder toggle
3. **GoalsScreen** — summary card, goal cards with circular progress, on-track/behind status
4. **GoalEditorScreen** — name, type, target, current, monthly contribution, deadline, account
5. **NetWorthScreen** — hero net worth figure, trend line chart, manual assets/liabilities list
6. **AssetEditorScreen** — name, value, type picker, liability toggle
7. **SafeToSpendScreen** — daily allowance hero, breakdown card, cashflow forecast line chart, upcoming bills
8. **FinanceInsightsScreen** — full insights list with severity-coded cards
9. **BudgetTemplateScreen** — income input, 3 template options with preview, apply button

### Modified Screens
- **FinanceScreen** — add insights LazyRow, safe-to-spend widget card, navigation to new screens
- **FinanceStatsScreen** — add insights section
- **DashboardScreen** — add safe-to-spend widget

---

## New Shared Components (4)

- `InsightCard` — reusable card with severity coloring (WARNING=red, POSITIVE=green, INFO=neutral)
- `CircularGoalProgress` — Canvas-based circular progress indicator
- `CashflowLineChart` — MPAndroidChart LineChart wrapper for balance projections
- `NetWorthLineChart` — MPAndroidChart LineChart wrapper for net worth snapshots

---

## Navigation Routes (9 new)

| Route | Screen |
|-------|--------|
| `finance/bills` | RecurringBillsScreen |
| `finance/bills/edit?billId={billId}` | RecurringBillEditorScreen |
| `finance/goals` | GoalsScreen |
| `finance/goals/edit?goalId={goalId}` | GoalEditorScreen |
| `finance/networth` | NetWorthScreen |
| `finance/networth/edit?assetId={assetId}` | AssetEditorScreen |
| `finance/safetospend` | SafeToSpendScreen |
| `finance/insights` | FinanceInsightsScreen |
| `finance/budget/template` | BudgetTemplateScreen |

---

## Implementation Order (dependency-layered)

**Layer 0 — Entities** (4 files)
**Layer 1 — Migration v4→v5** (1 file)
**Layer 2 — DAOs** (4 files)
**Layer 3 — Database wiring** (modify LifepadDatabase.kt + DatabaseModule.kt)
**Layer 4 — Domain logic** (6 files, pure Kotlin)
**Layer 5 — Repositories** (4 new + 1 modified)
**Layer 6 — Shared components** (4 files)
**Layer 7 — ViewModels** (8 new + 2 modified)
**Layer 8 — UI Screens** (9 new + 3 modified)
**Layer 9 — Navigation** (modify Screen.kt + LifepadNavHost.kt)

Build and verify after each layer.

---

## Key Constraints
- `combine()` max 5 params for mixed types — use separate `viewModelScope.launch` per flow
- Bill reminders reuse existing `ReminderEntity` with `linkedItemType = "BILL"`
- Budget templates use `OnConflictStrategy.REPLACE` (existing) — one budget per category
- Net worth snapshots: guard against double-write (check latest snapshot month)
- All new entity classes annotated with `@Entity`, registered in `@Database(entities = [...])`
