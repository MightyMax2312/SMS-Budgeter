# Flows

## Onboarding Import
`MainActivity` -> `OnboardingScreen` -> `StatementCalendarDialog` -> `MainViewModel.startBulkImport()` -> `BulkImportWorker` -> `SmsRepository` with SMS metadata -> `BankMessageParser` -> `AppDatabase`

## Manual Sync
`HomeScreen` -> `MainViewModel.triggerManualSync()` -> `SmsSyncWorker` -> `SmsRepository` with SMS metadata -> `BankMessageParser` -> `AppDatabase`

## Periodic Sync
`MainViewModel.schedulePeriodicSync()` -> WorkManager -> `SmsSyncWorker`

## UI Refresh
`TransactionDao.getAllTransactions()` + `SyncPreferences.monthlySavingsTarget` + `SyncPreferences.selectedStartDate` -> `TransactionRepository` -> `MainViewModel.uiState` with today/month/salary/month-average/daily-allowance aggregates -> `HomeScreen` swipeable Daily, Month, and History pages with page-scoped cards

## Resync From Date
`HomeScreen` -> `StatementCalendarDialog` -> `MainViewModel.resyncFromDate()` -> `TransactionRepository.deleteAll()` -> `BulkImportWorker` -> `SmsRepository` with SMS metadata -> `BankMessageParser` -> `AppDatabase`

## Statement Filtering
`HomeScreen` today/month cards / balance strip / segmented filter -> `MainViewModel.setFilter()` -> `HomeUiState.transactions` list changes while full-period, today, and month totals remain unfiltered

## Open Clicked SMS
`HomeScreen.TransactionRow` -> `MainActivity` popup transaction state -> `MessagePopup` -> persisted SMS ID/thread/sender if present -> Android SMS provider lookup fallback -> Messages search intent -> SMS/message/thread/sender fallback intents

## Duplicate Protection
`SmsRepository` provider IDs -> `BankMessageParser.transactionFingerprint` -> `TransactionEntity.smsId` / `transactionFingerprint` unique indexes -> Room `REPLACE` insert semantics collapse duplicate resync/manual imports

## Savings Target And Daily Allowance
`HomeScreen.SavingsTargetDialog` -> `MainViewModel.updateMonthlySavingsTarget()` -> `SyncPreferences.monthlySavingsTarget` -> `MainViewModel.uiState` salary and allowance math -> `HomeScreen.DailyAllowancePanel`

## Salary Detection
`SmsRepository` SMS body -> `BankMessageParser.detectCategory()` salary keywords -> `Transaction.category = SALARY` -> Room persistence -> `MainViewModel` current-month salary total

## Swipeable Statement Pages
`HomeScreen.SwipePageTabs` -> `HorizontalPager` -> Daily page for today's transactions and daily allowance only / Month page from month start with month-scoped balance, credits, safe-spend progress, and average daily spend / History page from selected import date
