# SMS Budgeter Graph Wiki

## Overview
This wiki is the navigation surface for the generated graph summary of the repo.

## Read First
- [Architecture](architecture.md)
- [Flows](flows.md)
- [Hotspots](hotspots.md)

## Quick Summary
- Entry point: `MainActivity`
- State hub: `MainViewModel`
- Core business logic: `BankMessageParser`
- Persistence: Room + DataStore
- Background execution: WorkManager via `SmsSyncWorker` and `BulkImportWorker`
- Key UI surfaces: `HomeScreen`, `StatementCalendarDialog`, `MessagePopup`
- SMS handoff: `MessagePopup` searches/deep-links into the platform Messages app using raw body, timestamp, and fallback intents
