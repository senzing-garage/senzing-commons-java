## Source Edit Policy

**Rule:** Do NOT modify source code files in this repository directly. Present
proposed code changes as suggestions for the user to review and apply.

**Exceptions:**

- `.claude/CLAUDE.md` — may be edited directly, after informing the user.
- `.claude/faq_server.py`, `.mcp.json`, and files under `.claude/faqs/` — Claude
  tooling and FAQ content; may be edited directly when explicitly requested.

**Why:** The user wants explicit review of behavior changes to the published
library before they land. Auto-applied edits to `src/` would bypass that
review.

**How to apply:** When asked to "fix", "refactor", "implement", or "change"
something in `src/main/` or `src/test/`, respond with the proposed diff (file
path, line numbers, before/after) instead of using `Edit` or `Write` against
those paths. Wait for the user to apply (or instruct you to apply) the change.
