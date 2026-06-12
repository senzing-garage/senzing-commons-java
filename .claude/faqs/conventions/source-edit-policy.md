## Source Edit Policy

**Rule:** Claude may edit source code files in this repository directly
when working on a task — applying patches via `Edit` / `Write` against
`src/main/` and `src/test/` is allowed and expected. No separate
review-before-apply step is required for routine code changes; the
normal PR review loop catches anything that warrants attention before
it lands on `main`.

**How to apply:** When asked to "fix", "refactor", "implement", or
"change" something in `src/`, go ahead and apply the edit. Format
the modified files (the `PostToolUse` hook handles this automatically
via `format_file.py`) and run `mvn -Pcheckstyle validate` plus
relevant tests to confirm the change is clean before reporting back.

**History:** Earlier versions of this policy required Claude to
present diffs and wait for the user to apply them. That predates the
current Claude Code workflow — the user has since signaled comfort
with direct edits, and PR review is the appropriate gate.
