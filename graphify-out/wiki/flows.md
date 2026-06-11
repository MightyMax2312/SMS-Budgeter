# Flows

## Onboarding Import
`MainActivity` -> `OnboardingScreen` -> `StatementCalendarDialog` -> `MainViewModel.startBulkImport()` -> `BulkImportWorker` -> `SmsRepository` -> `BankMessageParser` -> `AppDatabase`

## Manual Sync
`HomeScreen` -> `MainViewModel.triggerManualSync()` -> `SmsSyncWorker` -> `SmsRepository` -> `BankMessageParser` -> `AppDatabase`

## Periodic Sync
`MainViewModel.schedulePeriodicSync()` -> WorkManager -> `SmsSyncWorker`

## UI Refresh
`TransactionDao.getAllTransactions()` -> `TransactionRepository` -> `MainViewModel.uiState` -> `HomeScreen`

## Resync From Date
`HomeScreen` -> `StatementCalendarDialog` -> `MainViewModel.resyncFromDate()` -> `TransactionRepository.deleteAll()` -> `BulkImportWorker` -> `SmsRepository` -> `BankMessageParser` -> `AppDatabase`

## Statement Filtering
`HomeScreen` summary cards / segmented filter -> `MainViewModel.setFilter()` -> `HomeUiState.transactions` list changes while totals remain full-period totals

## Open Clicked SMS
`HomeScreen.TransactionRow` -> `MainActivity` popup state -> `MessagePopup` -> Android SMS provider lookup by raw body + timestamp -> Messages search intent -> SMS/message/thread/sender fallback intents
