# Graphify Report

## Project
- Name: `SMSBudgetTracker`
- Type: Android app
- Stack: Kotlin, Jetpack Compose, Room, DataStore, WorkManager, SQLCipher, Gradle Kotlin DSL

## Scope
- Kotlin source files analyzed: 16
- Primary resources/config files analyzed: 10
- Build output under `app/build/` excluded from structural analysis

## Entry Points
- Android app entry: `app/src/main/AndroidManifest.xml`
- Application class: `app/src/main/java/com/budgettracker/BudgetTrackerApp.kt`
- UI entry/activity: `app/src/main/java/com/budgettracker/MainActivity.kt`

## Community Structure

### 1. UI Layer
Files:
- `app/src/main/java/com/budgettracker/MainActivity.kt`
- `app/src/main/java/com/budgettracker/ui/screens/HomeScreen.kt`
- `app/src/main/java/com/budgettracker/ui/screens/StatementCalendarDialog.kt`
- `app/src/main/java/com/budgettracker/ui/screens/OnboardingScreen.kt`
- `app/src/main/java/com/budgettracker/ui/screens/PermissionScreen.kt`
- `app/src/main/java/com/budgettracker/ui/screens/MessagePopup.kt`
- `app/src/main/java/com/budgettracker/ui/theme/Theme.kt`

Responsibilities:
- Compose-driven permission, onboarding, statement-style home, calendar, and SMS detail views
- User actions for sync, resync, statement-card filter changes, date selection, and onboarding import
- Popup inspection of raw SMS content with timestamp display and SMS-app search/deep-link handoff

### 2. State And Orchestration Layer
Files:
- `app/src/main/java/com/budgettracker/ui/viewmodel/MainViewModel.kt`
- `app/src/main/java/com/budgettracker/worker/SmsSyncWorker.kt`

Responsibilities:
- Converts repository data into `HomeUiState`
- Manages onboarding completion, loading state, and transaction filter state
- Schedules periodic sync and dispatches one-time background work
- Drives bulk import and date-based resync flows

### 3. Domain Parsing Layer
Files:
- `app/src/main/java/com/budgettracker/domain/usecase/BankMessageParser.kt`
- `app/src/main/java/com/budgettracker/domain/model/Transaction.kt`
- `app/src/main/java/com/budgettracker/domain/model/TransactionType.kt`
- `app/src/main/java/com/budgettracker/domain/model/SmsMessage.kt`
- `app/src/main/java/com/budgettracker/domain/model/Bank.kt`
- `app/src/main/java/com/budgettracker/domain/model/BankRegistry.kt`
- `app/src/main/java/com/budgettracker/domain/model/SyncResult.kt`

Responsibilities:
- Identifies bank senders
- Extracts amount, account suffix, transaction type, and category from SMS text
- Defines core transaction and SMS models

### 4. Data Layer
Files:
- `app/src/main/java/com/budgettracker/data/repository/SmsRepository.kt`
- `app/src/main/java/com/budgettracker/data/repository/TransactionRepository.kt`
- `app/src/main/java/com/budgettracker/data/local/AppDatabase.kt`
- `app/src/main/java/com/budgettracker/data/local/dao/TransactionDao.kt`
- `app/src/main/java/com/budgettracker/data/local/entity/TransactionEntity.kt`
- `app/src/main/java/com/budgettracker/data/datastore/SyncPreferences.kt`

Responsibilities:
- Reads SMS inbox content from `Telephony.Sms`
- Persists parsed transactions to Room
- Stores sync timestamps and onboarding state in DataStore

### 5. Platform And Build Layer
Files:
- `app/build.gradle.kts`
- `build.gradle.kts`
- `settings.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/**`

Responsibilities:
- Android/Compose build wiring
- Permission declarations
- App branding and theme resources

## God Nodes

### `MainViewModel`
Why it stands out:
- Central state aggregator for repository flows, UI filters, onboarding state, sync scheduling, and worker dispatch
- Couples UI actions directly to WorkManager and persistence concerns

Connected areas:
- UI screens
- Room-backed repository
- DataStore preferences
- WorkManager workers

### `BankMessageParser`
Why it stands out:
- Core business logic for converting raw SMS text into usable transaction records
- Encodes bank detection, transaction-type heuristics, amount extraction, account extraction, and category inference

Connected areas:
- `SmsRepository`
- Both workers
- Domain models

### `SmsSyncWorker` / `BulkImportWorker`
Why they stand out:
- Bridge platform scheduling and business processing
- Fetch SMS, parse them, map to entities, write to Room, and update sync metadata

### `MainActivity`
Why it stands out:
- Top-level UI router
- Owns permission handling, screen switching, popup message/timestamp state, and resync dialog state

### `MessagePopup`
Why it stands out:
- Bridges app-owned transaction rows back to the platform SMS app
- Looks up matching SMS provider rows by raw body and timestamp, builds a search key, and falls back across app search, message URI, thread URI, sender URI, and inbox intents

## Main Execution Flows

### Flow A: First-run onboarding import
1. `MainActivity` shows `OnboardingScreen` when SMS permission is granted but onboarding is incomplete.
2. `OnboardingScreen` uses `StatementCalendarDialog` and returns a chosen start date.
3. `MainViewModel.startBulkImport()` enqueues `BulkImportWorker`.
4. `BulkImportWorker` reads SMS via `SmsRepository`.
5. `BankMessageParser` converts SMS into `Transaction`.
6. Transactions are mapped to `TransactionEntity` and inserted into Room.
7. `SyncPreferences` marks onboarding complete and updates the last SMS sync timestamp.

### Flow B: Periodic or manual sync
1. `MainViewModel` schedules periodic `SmsSyncWorker` in `init`.
2. `HomeScreen` manual sync action calls `MainViewModel.triggerManualSync()`.
3. `SmsSyncWorker` reads the last sync timestamp from `SyncPreferences`.
4. Repository + parser pipeline imports only the new SMS window.
5. Parsed transactions are written into Room.
6. Updated data flows back to UI through `TransactionRepository.getAllTransactions()`.

### Flow C: UI projection
1. Room DAO emits `Flow<List<TransactionEntity>>`.
2. `TransactionRepository` maps entities to domain `Transaction`.
3. `MainViewModel` combines transactions with `TransactionFilter`; list rows filter while summary totals remain based on all transactions.
4. `HomeUiState` is exposed as a `StateFlow`.
5. `HomeScreen` renders statement totals, balance, tappable credit/debit filters, date-time transaction rows, and popup launch callbacks.

### Flow D: Open clicked SMS
1. `HomeScreen` passes raw SMS text and timestamp from the clicked transaction row to `MainActivity`.
2. `MainActivity` stores popup message/timestamp state and shows `MessagePopup`.
3. `MessagePopup` displays raw SMS details, amount, and timestamp including seconds.
4. `MessagePopup.openClickedSms()` searches the SMS provider for the raw body near the transaction timestamp.
5. The app launches Messages with a precise search key first, then falls back to exact SMS URI, thread URI, sender conversation, and inbox intents.

## Structural Observations
- The project mostly follows a simple layered structure: UI -> ViewModel/workers -> repositories -> local storage/domain parsing.
- `BulkImportWorker` is declared in the same file as `SmsSyncWorker`, so worker responsibilities are grouped physically.
- `BankMessageParser` is the most business-critical logic in the codebase.
- `MessagePopup` now contains platform-intent heuristics because Android SMS apps do not consistently support exact message deep links.
- `StatementCalendarDialog` centralizes reusable calendar date selection and smooth month paging.
- SQLCipher support exists in `AppDatabase`, but active code paths currently use `getInstanceWithoutEncryption(...)`.
- Several dependencies and permissions suggest planned expansion beyond the current implementation:
  - Google Play Services Auth
  - Retrofit/Moshi
  - `INTERNET` and `ACCESS_NETWORK_STATE`
  - `LAST_GMAIL_SYNC` in `SyncPreferences`

## Architectural Risks
- `MainViewModel` mixes presentation-state shaping with job orchestration, which can make it a future change hotspot.
- Parsing logic is heuristic and regex-heavy, so unsupported bank formats will silently drop transactions.
- Worker-driven import logic is duplicated between periodic sync and bulk import.
- Permission state is held locally in `MainActivity`, not exposed as a durable app state model.
- Exact SMS opening depends on external Messages app behavior; the app mitigates this with SMS-provider lookup and search/deep-link fallbacks.

## Suggested Future Split Points
- Extract a shared transaction import service used by both workers.
- Move sync scheduling behind a dedicated coordinator/use case instead of keeping it in `MainViewModel`.
- Separate parser strategies by bank or message family if supported formats grow.
- Introduce a clearer app-state model for permission/onboarding/navigation state.
- Persist SMS `_id`, `thread_id`, and sender alongside each parsed transaction to avoid re-querying by body/timestamp when opening a clicked SMS.
