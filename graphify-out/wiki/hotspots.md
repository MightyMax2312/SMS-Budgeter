# Hotspots

## `MainViewModel`
- Highest coordination load
- Owns filtering, loading state, onboarding completion, worker dispatch, periodic scheduling, today/current-month UI aggregates, salary totals, savings target state, monthly average spend, and daily allowance math

## `BankMessageParser`
- Highest business-rule density
- Most likely file to change when adding bank support, improving extraction accuracy, or expanding salary/category keyword detection

## `SmsSyncWorker.kt`
- Contains both background workers
- Good candidate for a shared import pipeline abstraction

## `MessagePopup`
- Platform-intent hotspot for opening a clicked transaction in the user's SMS app
- Uses persisted SMS IDs/thread IDs first, then raw body, timestamp, SMS provider lookup, generated search keys, and fallback intents because exact SMS deep links vary by messaging app

## `HomeScreen`
- Primary presentation hotspot after the statement-style redesign
- Owns the dark dashboard styling, swipeable Daily/Month/History pager, compact icon page tabs, rounded stat cards, month safe-spend progress circle, daily allowance circle, savings-target prompt, activity feeds, summary-card filter affordances, transaction-row date-time display, and popup launch callbacks

## `TransactionEntity`
- Persistence hotspot for future budgeting features
- Stores parsed transaction data plus SMS provider metadata and unique duplicate-protection keys

## `SmsRepository`
- Platform data boundary for Android SMS provider reads
- Now carries provider `_id`, `thread_id`, sender, body, date, and type into the domain layer

## `StatementCalendarDialog`
- Reusable date-selection surface for onboarding and resync
- Owns smooth month paging and date bounds
