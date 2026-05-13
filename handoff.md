# SMS Budget Tracker — Handoff Document

## Repository
- **GitHub:** https://github.com/MightyMax2312/SMS-Budgeter
- **Main branch:** `main` — fully up to date with all dev features (merged from dev)
- **Dev branch:** `dev` — identical to main (fast-forward merge completed)

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
- `OnboardingScreen.kt` — First-run screen with import duration picker + "Start of Month" option
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

### Implemented Features
1. ✅ **"Start of this month" option** — Onboarding has a 5th option that auto-calculates days from the 1st of the current month
2. ✅ **Credit/Debit/Both filter** — Dropdown on HomeScreen to isolate transaction types by ALL, CREDIT ONLY, or DEBIT ONLY
3. ✅ **Enhanced MessagePopup** — Scrollable message (max 300dp), Copy to clipboard button, Open SMS app button
4. ✅ **Mandate exclusion** — UPI-Mandate messages are skipped entirely during parsing (not debit, not credit)
5. ✅ **Word-boundary regex** — Transaction type detection uses `\b` regex boundaries to prevent false substring matches
6. ✅ **Merge to main** — Fast-forward merge from `dev` → `main`

## How to Continue
1. Pull latest `main`: `git pull origin main`
2. Build in Android Studio or via `.\gradlew.bat assembleDebug`
3. Install `app/build/outputs/apk/debug/app-debug.apk` on a physical Android device for testing
4. **Next steps:** Gmail API integration, custom theming (sage/dark olive/terracotta + Space Grotesk), signed release APK, Google Play Store

## Important Notes
- `local.properties` is in `.gitignore` (contains SDK path — machine-specific)
- Build outputs (`app/build/`) are in `.gitignore`
- Logs and crash dumps are in `.gitignore`
- The app requests: READ_SMS, INTERNET, ACCESS_NETWORK_STATE, RECEIVE_BOOT_COMPLETED, POST_NOTIFICATIONS

## Git History (Recent Merges)
- `main` was fast-forwarded to include all 5 commits from `dev`
- Both branches are now at the same commit: `c94d102`