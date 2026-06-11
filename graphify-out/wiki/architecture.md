# Architecture

## Layers
1. UI
2. State and orchestration
3. Domain parsing
4. Data and persistence
5. Platform/build

## Dependency Direction
- Compose screens depend on `MainViewModel` state and callbacks.
- `MainViewModel` depends on `TransactionRepository`, `SyncPreferences`, `AppDatabase`, and WorkManager.
- Workers depend on `SmsRepository`, `BankMessageParser`, `AppDatabase`, and `SyncPreferences`.
- `TransactionRepository` depends on `TransactionDao`.
- `TransactionDao` and `TransactionEntity` define the Room persistence boundary.
- `MessagePopup` depends on Android SMS provider/search intents to hand a clicked transaction back to the platform Messages app.

## Notable Boundaries
- `SmsRepository` is the Android SMS inbox boundary.
- `MessagePopup.openClickedSms()` is the Android Messages app handoff boundary.
- `BankMessageParser` is the text-to-domain boundary.
- `TransactionEntity` is the domain-to-storage boundary.

## UI Notes
- `HomeScreen` owns statement-style presentation, summary-card filter affordances, and transaction row timestamp display.
- `StatementCalendarDialog` owns reusable date selection and smooth month paging for onboarding and resync.
- `MessagePopup` owns raw-message inspection, timestamp display, search-key construction, and SMS-app fallback intents.
