## Adding New FAQs

The FAQ MCP server reads `.claude/faqs/<category>/<topic>.md` at startup and
serves the contents through `search_faqs`, `get_faq`, and `get_faq_categories`.

### How to add an entry

1. Pick or create a category directory under `.claude/faqs/` (e.g.
   `architecture`, `building`, `release`, `troubleshooting`, `conventions`).
2. Create a markdown file. **The filename becomes the searchable title** — use
   dashes for spaces (e.g. `connection-pool-tuning.md` → "connection pool
   tuning").
3. Keep each file focused on one topic. BM25 ranks shorter, focused documents
   higher than sprawling ones.
4. Restart the Claude session (or reconnect MCP) so the server re-indexes.

### What belongs in a FAQ vs CLAUDE.md

- **CLAUDE.md** — coding conventions, build commands, package overview;
  always loaded into context, so keep it lean.
- **FAQ** — design rationale, operational gotchas, troubleshooting recipes,
  release checklists; pulled on demand, so detail is cheap.
- **Auto-memory** (`~/.claude/projects/.../memory/`) — user preferences and
  cross-session state; not project-scoped knowledge.

### Feedback loop

After Claude resolves a non-obvious issue (build quirk, dependency interaction,
test flake, release hiccup), it should ask the user whether to capture the
solution as a new FAQ. The goal is to grow the corpus so future sessions
recover the answer instantly via `search_faqs`.
