# SMS Budget Tracker — Handoff Document

## Repository
- **GitHub:** https://github.com/MightyMax2312/SMS-Budgeter
- **Main branch:** `main` — preserved working build checkpoint (original APK build)
- **Dev branch:** `dev` — all new features and fixes (currently ~8 commits ahead of main)

## Current Build Status
- **APK:** `app/build/outputs/apk/debug/app-debug.apk` (~25.5 MB)
- **Build:** SUCCESSFUL (minor deprecation warnings only)
- **Environment:** Java 17, Gradle 8.4, AGP 8.2.2, Kotlin 1.9.22, Compose BOM 2024.02.00

## What Has Been Built So Far

### Architecture
- **Clean Architecture** with layers: domain → data → UI
- **MVVM** pattern with Jetpack Compose UI
- **Room + SQLCipher** for encrypted local SQLite storage
- **WorkManager** for background SMS sync (periodic + one-time)
- **DataStore** for preferences (sync timestamps, onboarding state)

### Source Files (21 Kotlin files)
- `MainActivity.kt` — Entry point, SMS permission handling, navigation between screens
- `BudgetTrackerApp.kt` — Application class (declared in AndroidManifest.xml)
- `HomeScreen.kt` — Main screen with balance cards, transaction list, filter dropdown, message popup
- `OnboardingScreen.kt` — First-run screen with import duration picker + new "Start of Month" option
- `PermissionScreen.kt` — SMS permission grant screen
- `MessagePopup.kt` — Dialog showing raw SMS with scrollable text, Copy button, Open SMS button, parsed details
- `MainViewModel.kt` — ViewModel with `uiState`, `isLoading`, `isOnboardingCompleted`, `filter`, `transactions`
- `SmsSyncWorker.kt` — Background periodic sync worker
- `BulkImportWorker.kt` — One-time onboarding import worker
- `BankMessageParser.kt` — Regex-based SMS parser for HDFC/ICICI/SBI/Axis/Kotak/PNB/Yes Bank/BoB
- `TransactionRepository.kt` — Data access layer wrapping TransactionDao
- `SyncPreferences.kt` — DataStore-backed preferences
- `AppDatabase.kt` — Room database with encrypted and unencrypted instances
- `TransactionDao.kt` — Room DAO with queries for all transactions, totals, ranges
- `Transaction.kt` / `TransactionType.kt` / `Bank.kt` / `BankRegistry.kt` / `SmsMessage.kt` / `SyncResult.kt` — Domain models

### Latest Features (on `dev` branch)
1. **"Start of this month" option** — Onboarding now has a 5th option: "Start of this month" which calculates days from the 1st of the current month
2. **Credit/Debit/Both filter** — Dropdown on HomeScreen allows filtering transactions by ALL, CREDIT ONLY, or DEBIT ONLY
3. **Enhanced MessagePopup** — Scrollable message (max 300dp), Copy to clipboard button, Open SMS app button
4. **Mandate exclusion** — UPI-Mandate messages are now skipped during parsing (not counted as debit or credit)
5. **Word-boundary regex** — Transaction type detection uses `\b` word boundaries to prevent false matches (e.g., "mandate" won't match "debit")

## Known Issues / Bugs Fixed During Dev
- Duplicate composables (PermissionScreen, OnboardingScreen, MessagePopup) existed in HomeScreen.kt — removed
- Missing BudgetTrackerApp.kt Application class — added
- `combine()` flow call had wrong overload — fixed with `listOf()` wrapper
- Missing imports (clip, background, sp, TextAlign) — added
- "debit" substring matched inside "mandate" — fixed with word-boundary regex

## Pending Features (Not Yet Implemented)
- Gmail API integration (dependency removed from build.gradle.kts)
- Custom UI design (sage #C8C9B0, dark olive #4A5240, terracotta #D9694A, Space Grotesk font)
- Physical device testing
- Signed release APK/AAB generation
- Google Play Store publishing ($25 one-time fee)

## How to Continue
1. Pull from `dev` branch: `git checkout dev`
2. Build in Android Studio or via `.\gradlew.bat assembleDebug`
3. Install `app/build/outputs/apk/debug/app-debug.apk` on a physical Android device for testing
4. Next steps: Gmail API, custom theming, release build

## Important Notes
- `local.properties` is in `.gitignore` (contains SDK path — machine-specific)
- Build outputs (`app/build/`) are in `.gitignore`
- Logs and crash dumps are in `.gitignore`
- The app requests: READ_SMS, INTERNET, ACCESS_NETWORK_STATE, RECEIVE_BOOT_COMPLETED, POST_NOTIFICATIONS