# RecordReader Format Auto-Detection

`com.senzing.io.RecordReader` reads structured records from a
`Reader` without the caller having to specify the format. It
auto-detects JSON array, JSON Lines, or CSV from the **first
non-whitespace character** of the input.

## Detection rules

| First char | Format     | Provider                  |
| ---------- | ---------- | ------------------------- |
| `[`        | JSON array | `JsonArrayRecordProvider` |
| `{`        | JSON Lines | `JsonLinesRecordProvider` |
| any other  | CSV        | `CsvRecordProvider`       |

The reader peeks the first character, dispatches to the matching
provider, and then yields records uniformly as `JsonObject` —
CSV rows are mapped to JSON objects keyed by header column.

## Output uniformity

Regardless of input format, `readRecord()` returns a `JsonObject`.
This lets downstream code treat all three formats interchangeably.

## Error reporting

Parse errors include the line number where the failure was
detected — useful when a multi-megabyte JSON Lines file contains
one malformed record buried in the middle.

## Data source mapping

The reader supports an optional data-source mapping that injects
a `DATA_SOURCE` field into each record. When the source already
carries that field, the mapping can override or merge per
constructor configuration.
