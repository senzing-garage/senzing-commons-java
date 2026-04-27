## Java Formatting Standards

### Overview

All Java source files must conform to the formatting rules defined in
`.claude/java-coding-standards.md`. That document is the comprehensive
reference; consult it before writing or reformatting Java code.

### When generating new code

**Apply these standards from the start.** Do not write Java code in a
free-form way and then reformat — generate it already compliant:

- Pick brace placement based on the construct (Allman for type/method/
  constructor definitions; same-line for control flow and lambdas).
- Wrap long lines at `+`, `&&`, `||`, `?`, `:`, `.` with the operator
  starting the continuation line.
- Use the parameter placement priority (single line → paren-aligned →
  next-line double-indented).
- Reflow javadoc to fill near 80 chars; never leave 1-3 orphan words
  on a continuation.
- Prefer single-line short-circuit `if (cond) return X;` over a
  brace-wrapped form when it fits.
- Put `throws` on its own line, single-indented from the method.

After generation, run `mvn -Pcheckstyle validate` to confirm
compliance. The bulk-fix scripts below are an aid for legacy code or
batch updates — they are not a substitute for writing compliant code
on the first pass.

### Key Rules

- **80-character line limit** — enforced by checkstyle via `-Pcheckstyle`
- **Allman braces** for class, interface, enum, method, and constructor
  definitions
- **Same-line braces** for control flow: if/else, for, while, do,
  try/catch/finally, switch, synchronized, lambdas, array initializers,
  static initializers
- **Continuation indentation**: 8 spaces (double indent)
- **Operators on continuation lines**: break BEFORE `+`, `&&`, `||`,
  `?`, `:`, `.`
- **CSOFF/CSON**: only for deliberately aligned multi-line output
  (aligned labels, SQL DDL, column-formatted diagnostics) — NOT a
  general escape hatch
- **Javadoc**: reflow prose and `@tag` descriptions to fill lines near
  80 chars; do not leave orphaned short words
- **Switch case labels**: left-aligned with switch (no extra indent)
- **Single-line `if`**: `if (x == null) return null;` is allowed
  (NeedBraces with `allowSingleLineStatement=true`)

### Checkstyle Configuration

- `checkstyle.xml` — enforces LineLength (80), RightCurly, NeedBraces,
  UnusedImports, Indentation, OperatorWrap, FileTabCharacter, and
  CSOFF/CSON suppression
- `checkstyle-suppressions.xml` — globally suppresses checks that are
  not yet enforced (Indentation, NoWhitespaceAfter, FinalParameters,
  HiddenField, ParameterNumber, MagicNumber, AvoidNestedBlocks,
  MethodLength, FileLength, AvoidStarImport, RegexpSingleline)

Run checkstyle: `mvn -Pcheckstyle validate`

### VSCode Formatter

- `.vscode/java-formatter.xml` — Eclipse JDT formatter profile
  (Allman for methods/types, same-line for blocks)
- `.vscode/jdt-prefs.epf` — JDT preferences (ignore unused-import
  warnings)
- `.vscode/settings.json` — `formatOnSave` disabled for Java
  (manual formatting required)
- **Limitation**: the VSCode formatter cannot enforce per-block-type
  brace placement, cannot keep single-line `if` bodies with Allman
  methods, and does not reflow Javadoc prose. Use the scripts below
  for those.

### Automation Scripts

Five Python scripts in `.claude/scripts/` automate bulk formatting
fixes. Run from project root:

```bash
python3 .claude/scripts/fix_allman_braces.py
python3 .claude/scripts/fix_javadoc_reflow.py
python3 .claude/scripts/fix_javadoc_inline_tags.py
python3 .claude/scripts/fix_javadoc_tags.py
python3 .claude/scripts/fix_need_braces.py
```

- `fix_allman_braces.py` — moves opening braces to Allman style for
  class/interface/enum/method/constructor definitions; splits
  `throws` clauses onto their own line.
- `fix_javadoc_reflow.py` — reflows plain Javadoc prose paragraphs
  (skips paragraphs that begin with `{@link}`/`<code>`/etc.).
- `fix_javadoc_inline_tags.py` — reflows Javadoc paragraphs that
  contain inline tags (`{@link}`, `<code>`, etc.) but are still
  prose. Catches the cases `fix_javadoc_reflow.py` intentionally
  skips.
- `fix_javadoc_tags.py` — reflows `@param`, `@return`, `@throws`
  tag descriptions.
- `fix_need_braces.py` — fixes checkstyle `NeedBraces` violations on
  short-circuit `if`/`else` blocks. Per coding standards Tier 1, the
  preferred fix is to collapse `if (cond)\n    body;` to a single
  line `if (cond) body;` when it fits within 80 chars. If the
  collapsed line would exceed 80 chars, braces are added (Tier 2).
  Handles paired `if`/`else` consistently — if either branch needs
  braces, both get braces.

The scripts scan `src/main/java`, `src/test/java`, and
`src/demo/java` (only the directories that exist). This project does
not currently have `src/demo/java`.

### Full Reference

See `.claude/java-coding-standards.md` for the complete document
including method declaration priority rules, ternary operator tiers,
short-circuit conditional formatting, and the Claude prompt for
formatting.
