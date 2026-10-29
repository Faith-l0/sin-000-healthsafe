# IngestionServiceApp

## Overview

Parses and cleans `wards-outdated.csv`, a messy legacy export of wards, wings, and specialist departments data, and is the
first stop in the HealthSafe pipeline. Independent Maven module, no parent pom.

Part of the [HealthSafe](../README.md) project.

REST: exposes the cleaned records for `ward-service` (`../ward-service`) to
consume — see [Integration contracts](../README.md#integration-contracts) in the
root README for the endpoint shape.

## Example: one row cleaned

Input (`wards-outdated.csv`, row 6):

```
w-05,east wing ,PAEDIATRICS,five
```

Expected shape after cleaning (exact field names are up to you — this illustrates
the *kind* of transform expected, not a fixed schema to match exactly):

```json
{
  "wardId": "W-05",
  "wing": "East Wing",
  "department": "Paediatrics",
  "bedsAvailable": null,
  "notes": "bedsAvailable was non-numeric ('five') — flagged for follow-up"
}
```

Note this row is also a near-duplicate of `W-05` two rows above it (same real ward,
different ID casing and field values) — deciding how to merge or flag duplicates
like this is part of the exercise.

## Known data issues

`wards-outdated.csv` is deliberately messy — cleaning it is the point of this service. Look
out for (and handle) at least:

- **Inconsistent casing** in IDs, names, and status/category values (`Active` /
  `active` / `ACTIVE`)
- **Padding** — leading/trailing spaces, and the occasional double space, inside
  fields
- **Duplicate records** for the same real-world entity, written with a different ID
  casing/format and/or slightly different field values
- **Inconsistent date formats** (`YYYY-MM-DD`, `MM/DD/YYYY`, `DD-MM-YYYY`, one- and
  two-digit months/days) and outright invalid dates
- **Missing / placeholder values** — blank fields, `N/A`, `n/a`, `TBD`, `unknown`,
  `-`, `NaN`
- **Invalid or non-numeric values** in numeric columns (negative counts, spelled-out
  numbers, unrealistic values)
- **Inconsistent boolean/flag representations** (`Y`/`N`, `yes`/`no`, `1`/`0`,
  `true`/`FALSE`)
- **Naming/spelling variants** for the same thing (e.g. regional spelling
  differences, synonyms)

## What it does now

Reads `wards-outdated.csv` once at startup, cleans it with `WardCleaner`, and serves the result.

| Endpoint | Returns |
|---|---|
| `GET /health` | `OK` |
| `GET /wards` | the cleaned ward records as JSON |

The CSV has 18 rows, and 17 wards come out (W-05 appears twice and is merged).

## Decisions and trade-offs

- IDs are upper-cased (`w-02` becomes `W-02`), padding is trimmed, and repeated spaces collapse.
- Placeholders such as `N/A`, `TBD`, `-`, `NaN`, `null`, `none` and blank are treated as missing. A row with no ID is dropped, a missing wing becomes "Unknown Wing", and a missing department becomes "General".
- Wings are title-cased, and department spellings are normalised (`Pediatrics` and `paediatrics` both become `Paediatrics`).
- Bed counts: written-out numbers from one to five become numbers (`five` becomes 5), `full` becomes 0, and negative, unrealistic (over 500) or unreadable values become 0.
- Duplicates are merged by ID, ignoring case. When two rows disagree, the later non-empty value wins. That assumes later rows are newer, which the file does not guarantee.
- Trade-off: a missing or invalid bed count becomes 0, so unknown cannot be told apart from full. The example above suggests storing it as empty with a note, and that is what I would do with more time.
- Malformed rows are skipped without a log message, which keeps the service running but hides problems.
- The data is loaded once, so a change to the CSV needs a restart.

## Project structure

    ingestion-service/
    ├── pom.xml
    └── src
        ├── main
        │   ├── java/co/wethinkcode/healthsafe/
        │   │   ├── IngestionServiceApp.java   starts the service and serves /wards
        │   │   ├── WardCleaner.java           reads and cleans the CSV
        │   │   └── WardRecord.java            a cleaned ward
        │   └── resources/wards-outdated.csv
        └── test/java/co/wethinkcode/healthsafe/WardCleanerTest.java

## Build

```
mvn package
```

## Run

```
java -jar target/ingestion-service.jar
```

Listens on port `7030`.

## Test

    mvn test

5 tests: casing and whitespace, placeholder values, word numbers and negative counts, duplicate resolution, and malformed rows. No broker or other service is needed.
