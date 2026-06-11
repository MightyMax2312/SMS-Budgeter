# Hotspots

## `MainViewModel`
- Highest coordination load
- Owns filtering, loading state, onboarding completion, worker dispatch, and periodic scheduling

## `BankMessageParser`
- Highest business-rule density
- Most likely file to change when adding bank support or improving extraction accuracy

## `SmsSyncWorker.kt`
- Contains both background workers
- Good candidate for a shared import pipeline abstraction

## `MessagePopup`
- Platform-intent hotspot for opening a clicked transaction in the user's SMS app
- Uses raw body, timestamp, SMS provider lookup, generated search keys, and fallback intents because exact SMS deep links vary by messaging app

## `HomeScreen`
- Primary presentation hotspot after the statement-style redesign
- Owns summary-card filter affordances, transaction-row date-time display, and popup launch callbacks

## `StatementCalendarDialog`
- Reusable date-selection surface for onboarding and resync
- Owns smooth month paging and date bounds
