# New Migration Service — Implementation Order

1. **`MigrationType`** — Add the new migration type to identify the new migration module.
2. **`XxxRecord`** — Represents one record/row from the source file.
3. **`XxxExcelReader`** — Reads Excel data and converts rows into `XxxRecord`.
4. **`XxxRowValidator`** — Validates an individual record.
5. **`XxxFileValidationService`** — Validates the complete input file.
6. **`XxxRequest`** — Defines the request payload for the target API.
7. **`XxxResponse`** — Defines the response received from the target API.
8. **`XxxRequestBuilder`** — Converts `XxxRecord` into `XxxRequest`.
9. **`XxxApiClient`** — Calls the target API and returns `XxxResponse`.
10. **`XxxMigrationProcessor`** — Orchestrates the complete migration process.

### Flow

```text
MigrationType
     ↓
XxxRecord
     ↓
XxxExcelReader
     ↓
XxxRowValidator
     ↓
XxxFileValidationService
     ↓
XxxRequestBuilder
     ↓
XxxRequest
     ↓
XxxApiClient
     ↓
XxxResponse
     ↓
XxxMigrationProcessor
```
