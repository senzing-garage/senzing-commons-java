#!/usr/bin/env python3
"""
Fix checkstyle NeedBraces violations for short-circuit if/else patterns.

For patterns like:
    if (cond)
        body;

Prefer (per coding standards Tier 1) collapsing to a single line:
    if (cond) body;

If the collapsed line would exceed 80 characters, fall back to adding
braces (Tier 2):
    if (cond) {
        body;
    }

Also handles the `else` analogue:
    else
        body;
->  else body;          (if it fits)
or  else {              (otherwise — paired with the preceding if)
        body;
    }

When an if/else pair has bodies that BOTH fit on one line, both are
collapsed independently. When either does not fit, braces are added
around both branches to keep the formatting consistent.

Targets violations checkstyle's NeedBraces module flags. Leaves
already-braced or already-single-line forms alone.
"""

import re
import sys
from pathlib import Path

MAX_LINE = 80
SRC_DIRS = [Path('src/main/java'), Path('src/test/java'),
            Path('src/demo/java')]

# Match an if/else if/else line that opens a body on the next line
# (no trailing brace, no trailing semicolon).
RE_IF_OPEN = re.compile(
    r'^(?P<indent>[ \t]*)(?P<kw>if|else\s+if)\s*\((?P<cond>.*)\)\s*$'
)
RE_ELSE_OPEN = re.compile(
    r'^(?P<indent>[ \t]*)else\s*$'
)
# Body line: deeper indent, ends with ';' (single statement)
RE_BODY_STMT = re.compile(
    r'^(?P<indent>[ \t]+)(?P<stmt>[^\s].*;)\s*$'
)


def line_indent(line):
    return line[:len(line) - len(line.lstrip(' \t'))]


def can_inline(parent_indent, prefix, body_stmt):
    """Return collapsed line if it fits within MAX_LINE, else None."""
    candidate = f"{parent_indent}{prefix} {body_stmt}"
    if len(candidate) <= MAX_LINE:
        return candidate
    return None


def collapse_or_brace_block(lines, header_idx, body_idx, parent_indent,
                            header_prefix):
    """Try to inline header+body. Returns (new_lines, lines_consumed).

    new_lines: list of replacement lines (with trailing newline).
    lines_consumed: how many original lines this consumed (>= 2).
    """
    body_line = lines[body_idx]
    body_match = RE_BODY_STMT.match(body_line.rstrip('\n').rstrip('\r'))
    if not body_match:
        return None, 0

    stmt = body_match.group('stmt').strip()
    body_indent = body_match.group('indent')
    if len(body_indent) <= len(parent_indent):
        return None, 0

    inlined = can_inline(parent_indent, header_prefix, stmt)
    consumed = body_idx - header_idx + 1

    if inlined is not None:
        return [inlined + '\n'], consumed

    braced = [
        f"{parent_indent}{header_prefix} {{\n",
        f"{body_indent}{stmt}\n",
        f"{parent_indent}}}\n",
    ]
    return braced, consumed


def find_next_code_line(lines, start_idx):
    """Return index of the next non-blank, non-comment line, or len."""
    n = len(lines)
    i = start_idx
    in_block = False
    while i < n:
        s = lines[i].strip()
        if in_block:
            if '*/' in s:
                in_block = False
            i += 1
            continue
        if not s:
            i += 1
            continue
        if s.startswith('//'):
            i += 1
            continue
        if s.startswith('/*'):
            if '*/' not in s:
                in_block = True
            i += 1
            continue
        return i
    return n


def process_file(path):
    """Rewrite file in place if any short-circuit if/else can be fixed.

    Returns (changed, fixes_applied).
    """
    text = path.read_text(encoding='utf-8')
    lines = text.splitlines(True)
    n = len(lines)

    out = []
    i = 0
    fixes = 0

    in_block_comment = False

    while i < n:
        raw = lines[i]
        stripped_no_nl = raw.rstrip('\n').rstrip('\r')
        s = stripped_no_nl.strip()

        if in_block_comment:
            out.append(raw)
            if '*/' in s:
                in_block_comment = False
            i += 1
            continue
        if s.startswith('/*') and '*/' not in s:
            in_block_comment = True
            out.append(raw)
            i += 1
            continue
        if s.startswith('//') or s.startswith('*'):
            out.append(raw)
            i += 1
            continue

        m_if = RE_IF_OPEN.match(stripped_no_nl)
        if not m_if:
            out.append(raw)
            i += 1
            continue

        parent_indent = m_if.group('indent')
        kw = m_if.group('kw')
        cond = m_if.group('cond')
        if_header = f"{kw} ({cond})"

        body_idx = find_next_code_line(lines, i + 1)
        if body_idx >= n:
            out.append(raw)
            i += 1
            continue

        # Bail if the body line itself is an `if`, `{`, or other
        # complex construct — only handle plain single-statement bodies.
        body_stripped = lines[body_idx].rstrip('\n').rstrip('\r')
        body_s = body_stripped.strip()
        if (body_s.startswith('{')
                or body_s.endswith('{')
                or not body_s.endswith(';')
                or body_s.startswith('if ')
                or body_s.startswith('if(')):
            out.append(raw)
            i += 1
            continue

        if_replacement, if_consumed = collapse_or_brace_block(
            lines, i, body_idx, parent_indent, if_header)
        if if_replacement is None:
            out.append(raw)
            i += 1
            continue

        # Look for paired else / else if right after the body
        next_idx = i + if_consumed
        else_replacement = None
        else_consumed = 0

        if next_idx < n:
            else_raw = lines[next_idx].rstrip('\n').rstrip('\r')
            m_else_if = RE_IF_OPEN.match(else_raw)
            m_else = RE_ELSE_OPEN.match(else_raw)
            if m_else_if and m_else_if.group('kw') == 'else if':
                if m_else_if.group('indent') == parent_indent:
                    # Recurse-style: collapse the else-if header/body
                    e_body_idx = find_next_code_line(
                        lines, next_idx + 1)
                    if e_body_idx < n:
                        e_body_s = lines[e_body_idx].strip()
                        if (e_body_s.endswith(';')
                                and not e_body_s.startswith('{')
                                and not e_body_s.endswith('{')
                                and not e_body_s.startswith('if ')
                                and not e_body_s.startswith('if(')):
                            e_header = (f"else if "
                                        f"({m_else_if.group('cond')})")
                            ereplace, econsumed = collapse_or_brace_block(
                                lines, next_idx, e_body_idx,
                                parent_indent, e_header)
                            if ereplace is not None:
                                else_replacement = ereplace
                                else_consumed = econsumed
            elif m_else and m_else.group('indent') == parent_indent:
                e_body_idx = find_next_code_line(lines, next_idx + 1)
                if e_body_idx < n:
                    e_body_s = lines[e_body_idx].strip()
                    if (e_body_s.endswith(';')
                            and not e_body_s.startswith('{')
                            and not e_body_s.endswith('{')
                            and not e_body_s.startswith('if ')
                            and not e_body_s.startswith('if(')):
                        ereplace, econsumed = collapse_or_brace_block(
                            lines, next_idx, e_body_idx,
                            parent_indent, 'else')
                        if ereplace is not None:
                            else_replacement = ereplace
                            else_consumed = econsumed

        # If we matched an if/else pair: ensure consistent style.
        # If either branch needed braces, force braces on both so the
        # if/else stays readable as a unit.
        if else_replacement is not None:
            if_inlined = (len(if_replacement) == 1)
            else_inlined = (len(else_replacement) == 1)
            if if_inlined and else_inlined:
                # Both single-line: emit "} else <stmt>;" form? No —
                # standards permit two separate single-line ifs; but
                # an explicit `else stmt;` is cleaner. Emit:
                #   if (cond) body1;
                #   else body2;
                pass
            else:
                # At least one didn't fit — brace both for symmetry.
                # Re-wrap each as braces with same indents.
                if_body_idx = body_idx
                e_first_idx = next_idx + 1
                e_body_idx = find_next_code_line(lines, e_first_idx)
                if_body_indent = line_indent(lines[if_body_idx])
                if_body_stmt = lines[if_body_idx].strip()
                e_body_indent = line_indent(lines[e_body_idx])
                e_body_stmt = lines[e_body_idx].strip()
                e_header = ('else if (' + (m_else_if.group('cond')
                                           if (m_else_if and
                                               m_else_if.group('kw')
                                               == 'else if')
                                           else '') + ')'
                            if (m_else_if and
                                m_else_if.group('kw') == 'else if')
                            else 'else')
                if_replacement = [
                    f"{parent_indent}{if_header} {{\n",
                    f"{if_body_indent}{if_body_stmt}\n",
                    f"{parent_indent}}} {e_header} {{\n",
                    f"{e_body_indent}{e_body_stmt}\n",
                    f"{parent_indent}}}\n",
                ]
                # Combined replacement; clear else_replacement so we
                # don't double-emit.
                out.extend(if_replacement)
                fixes += 2
                i = next_idx + else_consumed
                continue

        out.extend(if_replacement)
        fixes += 1
        i += if_consumed

        if else_replacement is not None:
            out.extend(else_replacement)
            fixes += 1
            i += else_consumed

    if fixes > 0:
        path.write_text(''.join(out), encoding='utf-8')
    return fixes > 0, fixes


def main():
    found = [d for d in SRC_DIRS if d.is_dir()]
    if not found:
        print("ERROR: No src dirs found. Run from project root.",
              file=sys.stderr)
        sys.exit(1)

    total = 0
    modified = 0
    total_fixes = 0
    for d in found:
        for jf in sorted(d.rglob('*.java')):
            total += 1
            changed, fixes = process_file(jf)
            if changed:
                modified += 1
                total_fixes += fixes
                print(f"  Fixed {fixes:3d} in: {jf}")

    print(f"\nProcessed {total} files, modified {modified}, "
          f"{total_fixes} short-circuit fixes applied.")


if __name__ == '__main__':
    main()
