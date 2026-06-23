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
- `MessagePopup` depends on persisted SMS metadata first, then Android SMS provider/search intents to hand a clicked transaction back to the platform Messages app.

## Notable Boundaries
- `SmsRepository` is the Android SMS inbox boundary.
- `SmsRepository` now carries SMS `_id`, `thread_id`, sender, body, and date into domain parsing.
- `TransactionEntity` stores SMS metadata and unique duplicate-protection fingerprints.
- `MessagePopup.openClickedSms()` is the Android Messages app handoff boundary.
- `BankMessageParser` is the text-to-domain boundary, including salary keyword categorization for credit messages.
- `TransactionEntity` is the domain-to-storage boundary.
- `SyncPreferences` stores the user's monthly savings target for daily allowance calculations.

## UI Notes
- `HomeScreen` owns the dark finance-dashboard presentation, swipeable Daily/Month/History pages, compact icon page tabs, rounded stat cards, daily allowance circle, month safe-spend circle, savings-target prompt, activity feeds, summary-card filter affordances, and transaction row timestamp display.
- `StatementCalendarDialog` owns reusable date selection and smooth month paging for onboarding and resync.
- `MessagePopup` owns raw-message inspection, timestamp/sender display, persisted SMS metadata use, search-key construction, and SMS-app fallback intents.
