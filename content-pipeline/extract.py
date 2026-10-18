"""Stage 1 of the content pipeline: PDF -> structured line stream.

For each handbook we emit a list of "blocks" with the PDF page number, a kind
(heading level, paragraph, code, table row, list item) and text with **bold**
spans preserved. Later stages parse these blocks into Topics, Questions, etc.

Usage: python3 -I extract.py raw/ out/blocks/
"""
import json
import re
import sys
from collections import Counter
from pathlib import Path

import pymupdf as fitz

BOOKS = {
    "python": "Python Automation Engineer Handbook",
    "api": "API & Protocol Automation Handbook",
    "linux": "Linux, Bash & Embedded Debugging Handbook",
    "cicd": "Git, Jenkins & CI/CD Handbook",
    "bmcssd": "BMC/SSD Interview Prep Handbook",
}

LIGS = {"fi", "fl", "ff", "ffi", "ffl"}
MONO_HINTS = ("Mono", "Courier", "Consol")
BOLD_HINTS = ("Bold", "Black", "Heavy")
# The BMC/SSD PDF uses Type3 fonts; object 4 is its bold face (checked by sampling).
TYPE3_BOLD = ("Type3 (4 0 R)", "Type3 (9 0 R)")


def is_mono(font: str) -> bool:
    return any(h in font for h in MONO_HINTS)


def is_bold(font: str) -> bool:
    return any(h in font for h in BOLD_HINTS) or font in TYPE3_BOLD


def page_lines(page):
    """Return physical lines: dicts with y, x, size, mono, text (bold marked)."""
    out = []
    for b in page.get_text("dict")["blocks"]:
        for l in b.get("lines", []):
            spans = l["spans"]
            if not "".join(s["text"] for s in spans).strip():
                continue
            total = sum(len(s["text"]) for s in spans) or 1
            mono_chars = sum(len(s["text"]) for s in spans if is_mono(s["font"]))
            mono = mono_chars / total > 0.6
            # build (text, bold) segments; ligature glyphs inherit the previous span's weight
            segs = []
            for si, s in enumerate(spans):
                t = s["text"]
                b = (not mono) and is_bold(s["font"])
                if t.strip() in LIGS:
                    nxt = spans[si + 1] if si + 1 < len(spans) else None
                    if nxt is not None and nxt["text"][:1].isalnum():
                        b = (not mono) and is_bold(nxt["font"])
                    elif segs:
                        b = segs[-1][1]
                if segs and (segs[-1][1] == b or not t.strip()):
                    segs[-1][0] += t
                else:
                    segs.append([t, b])
            text = ""
            for t, b in segs:
                if b and t.strip():
                    lead = t[: len(t) - len(t.lstrip())]
                    trail = t[len(t.rstrip()):]
                    text += f"{lead}**{t.strip()}**{trail}"
                else:
                    text += t
            text = text.replace("****", "")
            out.append({
                "y": round(l["bbox"][1], 1),
                "y1": round(l["bbox"][3], 1),
                "x": round(l["bbox"][0], 1),
                "size": round(max(s["size"] for s in spans), 1),
                "mono": mono,
                "bold": all(is_bold(s["font"]) for s in spans if s["text"].strip()),
                "text": text,
            })
    return out


def body_size(doc):
    c = Counter()
    for i in range(min(len(doc), 200)):
        for ln in page_lines(doc[i]):
            if not ln["mono"]:
                c[ln["size"]] += len(ln["text"])
    return c.most_common(1)[0][0]


def repeated_furniture(doc, n_pages):
    """Running headers/footers: short texts in page margins repeated on many pages."""
    c = Counter()
    for i in range(n_pages):
        page = doc[i]
        h = page.rect.height
        for ln in page_lines(page):
            if ln["y"] < 45 or ln["y"] > h - 50:
                c[re.sub(r"\d+", "#", ln["text"].strip())] += 1
    return {t for t, k in c.items() if k > max(5, n_pages * 0.03)}


def group_rows(lines):
    """Merge physical lines sharing a baseline into rows (tables / mixed spans)."""
    lines = sorted(lines, key=lambda l: (round(l["y"] / 3), l["x"]))
    rows = []
    for ln in lines:
        if rows and abs(rows[-1][0]["y"] - ln["y"]) < 3:
            rows[-1].append(ln)
        else:
            rows.append([ln])
    return rows


def extract(book: str, pdf: Path):
    doc = fitz.open(pdf)
    n = len(doc)
    base = body_size(doc)
    furniture = repeated_furniture(doc, n)
    blocks = []
    for pi in range(n):
        page = doc[pi]
        h = page.rect.height
        lines = []
        for ln in page_lines(page):
            key = re.sub(r"\d+", "#", ln["text"].strip())
            if (ln["y"] < 45 or ln["y"] > h - 50) and (key in furniture or re.fullmatch(r"#", key)):
                continue
            # running chapter headers of the three LaTeX-built handbooks sit above y=40
            if book in ("api", "linux", "cicd") and ln["y"] < 40 and not ln["mono"]:
                continue
            lines.append(ln)
        rows = group_rows(lines)
        # left margin of body text on this page
        xs = [r[0]["x"] for r in rows if not r[0]["mono"]]
        left = min(xs) if xs else 50
        code_left = min((r[0]["x"] for r in rows if all(l["mono"] for l in r)), default=left)
        for r in rows:
            mono = all(l["mono"] for l in r)
            size = max(l["size"] for l in r)
            if mono:
                cw = r[0]["size"] * 0.602
                indent = max(0, round((r[0]["x"] - code_left) / cw))
                parts = [r[0]["text"]]
                for prev, cur in zip(r, r[1:]):
                    gap = max(1, round((cur["x"] - (prev["x"] + len(prev["text"]) * cw)) / cw))
                    parts.append(" " * gap + cur["text"])
                blocks.append({"p": pi + 1, "k": "code", "t": " " * indent + "".join(parts).rstrip()})
                continue
            if len(r) > 1 and (r[1]["x"] - r[0]["x"]) > 60 and not re.match(r"^\s*(•|\d+\.|[a-z]\))\s*$", r[0]["text"]):
                blocks.append({"p": pi + 1, "k": "row", "t": " | ".join(l["text"].strip() for l in r), "x": r[0]["x"]})
                continue
            text = " ".join(l["text"].strip() for l in r).strip()
            if not text:
                continue
            kind = "p"
            if size >= base + 6:
                kind = "h1"
            elif size >= base + 2.5:
                kind = "h2"
            elif size >= base + 0.8 and all(l["bold"] for l in r):
                kind = "h3"
            blocks.append({"p": pi + 1, "k": kind, "t": text, "x": r[0]["x"] - left, "y": r[0]["y"]})
    return blocks


def to_markdown(blocks):
    out, in_code, last_p = [], False, None
    for b in blocks:
        if b["p"] != last_p:
            if in_code:
                out.append("```"); in_code = False
            out.append(f"\n<!-- page {b['p']} -->")
            last_p = b["p"]
        if b["k"] == "code":
            if not in_code:
                out.append("```"); in_code = True
            out.append(b["t"])
            continue
        if in_code:
            out.append("```"); in_code = False
        prefix = {"h1": "# ", "h2": "## ", "h3": "### ", "row": "| ", "p": ""}[b["k"]]
        out.append(prefix + b["t"])
    if in_code:
        out.append("```")
    return "\n".join(out)


if __name__ == "__main__":
    src, dst = Path(sys.argv[1]), Path(sys.argv[2])
    dst.mkdir(parents=True, exist_ok=True)
    for book in BOOKS:
        blocks = extract(book, src / f"{book}.pdf")
        (dst / f"{book}.json").write_text(json.dumps(blocks, ensure_ascii=False))
        (dst / f"{book}.md").write_text(to_markdown(blocks))
        print(book, len(blocks), "blocks")
