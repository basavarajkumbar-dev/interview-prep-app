"""Stage 2: line stream -> handbook tree (chapters -> sections -> elements).

Elements: p (paragraph), li (bullet/numbered item), code, out (program output),
table (list of rows), each with the PDF page it starts on.

Usage: python3 -I structure.py out/blocks out/tree
"""
import json
import re
import sys
from pathlib import Path

LIG = re.compile(r"\*\*(fi|fl|ff|ffi|ffl)\*\*")


def clean(t: str) -> str:
    t = LIG.sub(r"\1", t)
    t = t.replace("** **", " ").replace("****", "")
    t = re.sub(r"\*\*(\s*)\*\*", r"\1", t)
    t = t.replace(" ", " ").replace("​", "")
    return t


def strip_bold(t: str) -> str:
    return re.sub(r"\s+", " ", t.replace("**", "")).strip()


def join_lines(a: str, b: str) -> str:
    if not a:
        return b
    if a.endswith(("‐", "-")) and re.match(r"^\**[a-z]", b) and not a.endswith(" -"):
        # soft hyphen at line end ("re‐ sources"); keep real hyphens like "first-"
        if a.endswith("‐"):
            return a[:-1] + b
        return a + b
    return a + " " + b


# ---------------------------------------------------------------- headings
CH_PATTERNS = {
    "python": [
        (re.compile(r"^Chapter (\d+): (.+)$"), "chapter"),
        (re.compile(r"^Project (P\d+): (.+)$"), "chapter"),
        (re.compile(r"^Part ([IVX]+)\b[ ·:—-]*(.*)$"), "part"),
    ],
    "api": [
        (re.compile(r"^Part ([IVX]+) — (.+)$"), "part"),
        (re.compile(r"^(\d+) (.+)$"), "chapter"),
        (re.compile(r"^Appendix ([A-D]) — (.+)$"), "chapter"),
    ],
    "cicd": [
        (re.compile(r"^Part ([IVX]+) — (.+)$"), "part"),
        (re.compile(r"^(\d+) (.+)$"), "chapter"),
        (re.compile(r"^Appendix ([A-D]) — (.+)$"), "chapter"),
    ],
    "linux": [
        (re.compile(r"^Part ([IVX]+) — (.+)$"), "part"),
        (re.compile(r"^Chapter (\d+) (.+)$"), "chapter"),
        (re.compile(r"^(Glossary)$"), "chapter"),
        (re.compile(r"^(Final Project Index)$"), "chapter"),
        (re.compile(r"^(Completion and Validation Report)$"), "chapter"),
    ],
    "bmcssd": [
        (re.compile(r"^(\d+)\. (.+)$"), "chapter"),
    ],
}
SEC_RE = re.compile(r"^(\d+\.\d+(?:\.\d+)?)\.? +(.+)$")


def is_toc_row(b):
    return b["k"] == "row" and b["t"].count(". |") > 3


def merge_headings(blocks):
    out = []
    for b in blocks:
        if (out and b["k"] in ("h1", "h2", "h3") and out[-1]["k"] == b["k"] and out[-1]["p"] == b["p"]
                and not re.match(r"^\**\s*(\d|Part |Chapter |Appendix )", b["t"])):
            out[-1] = dict(out[-1], t=out[-1]["t"] + " " + b["t"])
        else:
            out.append(dict(b))
    return out


def classify_heading(book, b, front_matter_done):
    """Return ('part'|'chapter'|'section', num, title) or None."""
    text = strip_bold(clean(b["t"]))
    k = b["k"]
    if book == "python":
        if k in ("h1", "h2", "h3"):
            for rx, kind in CH_PATTERNS[book]:
                m = rx.match(text)
                if m:
                    return kind, m.group(1), m.group(2) if m.lastindex > 1 else ""
            if k == "h3" and not front_matter_done:
                return "front", "", text
        if k == "p" and b["t"].startswith("**") and b["t"].rstrip().endswith("**"):
            m = SEC_RE.match(text)
            if m and len(text) < 110:
                return "section", m.group(1), m.group(2)
        if k == "p" and b["t"].startswith("**Part "):
            m = re.match(r"^Part ([IVX]+)[:·]?\s*(.*)$", text)
            if m:
                return "part", m.group(1), m.group(2)
        return None
    if book == "bmcssd":
        if k == "h2":
            m = re.match(r"^(\d+)\. (.+)$", text)
            if m:
                return "chapter", m.group(1), m.group(2)
            return "front", "", text
        if k == "h3":
            m = SEC_RE.match(text)
            if m:
                return "section", m.group(1), m.group(2)
            return "sub", "", text
        return None
    # api / cicd / linux
    if k == "h1":
        for rx, kind in CH_PATTERNS[book]:
            m = rx.match(text)
            if m:
                return kind, m.group(1), m.group(2) if m.lastindex > 1 else m.group(1)
        return None
    if k in ("h2", "h3"):
        m = SEC_RE.match(text)
        if m:
            return "section", m.group(1), m.group(2)
        if k == "h2" and text in ("How to Use This Handbook",):
            return "front", "", text
        return "sub", "", text
    return None


# ---------------------------------------------------------------- elements
BULLET_RE = re.compile(r"^(•|–|-|\d{1,2}\.|[a-h]\))\s+\S")


def build_elements(blocks):
    """Turn body blocks of one section into elements."""
    els = []
    prev_p = None  # last paragraph block (for y-gap logic)
    i = 0
    while i < len(blocks):
        b = blocks[i]
        k = b["k"]
        if k == "code":
            lines = []
            page = b["p"]
            while i < len(blocks) and blocks[i]["k"] == "code":
                lines.append(blocks[i]["t"])
                i += 1
            # strip common indent
            ind = min((len(l) - len(l.lstrip()) for l in lines if l.strip()), default=0)
            code = "\n".join(l[ind:] for l in lines).strip("\n")
            # merge with previous code element if split only by a page break
            if els and els[-1]["t"] in ("code", "out") and els[-1].get("_open"):
                els[-1]["text"] += "\n" + code
            else:
                kind = "code"
                if els and els[-1]["t"] in ("p", "li") and re.search(
                        r"(Output|output|prints|Expected output|Expected)( \([^)]*\))?:\s*$", strip_bold(els[-1]["text"])):
                    kind = "out"
                els.append({"t": kind, "text": code, "page": page, "_open": True})
            prev_p = None
            continue
        if k == "row":
            rows = []
            page = b["p"]
            while i < len(blocks) and blocks[i]["k"] == "row":
                rows.append([clean(c) for c in blocks[i]["t"].split(" | ")])
                i += 1
            els.append({"t": "table", "rows": rows, "page": page})
            prev_p = None
            continue
        if k in ("h1", "h2", "h3"):
            els.append({"t": "h", "text": strip_bold(clean(b["t"])), "page": b["p"]})
            prev_p = None
            i += 1
            continue
        # paragraph line
        t = clean(b["t"]).strip()
        # wrapped second-column text of a two-column table row
        if els and els[-1]["t"] == "table" and b.get("x", 0) > 50 and not BULLET_RE.match(strip_bold(t)):
            els[-1]["rows"][-1][-1] = join_lines(els[-1]["rows"][-1][-1], t)
            i += 1
            continue
        plain = strip_bold(t)
        new_para = True
        if prev_p is not None and els and els[-1]["t"] in ("p", "li"):
            same_page = prev_p["p"] == b["p"]
            gap = (b.get("y", 0) - prev_p.get("y", 0)) if same_page else 0
            lh = 16
            starts_new = bool(BULLET_RE.match(plain)) or bool(re.match(r"^[🔴🟡🟢⚪]", plain)) or (t.startswith("**") and t.endswith("**")) or t.startswith("**") and re.match(
                r"^\*\*(Q\d*[:.]|[A-Z]?\d+\.|\d+\.|Answer|Follow|Scenario|Problem|Hint|Example|Step)", t)
            if same_page and 0 < gap < lh * 1.45 and not starts_new:
                new_para = False
            if not same_page and not starts_new and not re.search(r"[.:!?)]\**$", els[-1]["text"]):
                new_para = False
            # a pure label line like "Output:" stays separate
        if not new_para:
            els[-1]["text"] = join_lines(els[-1]["text"], t)
        else:
            kind = "li" if BULLET_RE.match(plain) else "p"
            els.append({"t": kind, "text": t, "page": b["p"]})
        for e in els[:-1]:
            e.pop("_open", None)
        if els[-1]["t"] in ("p", "li"):
            # an output label keeps the following code (possibly on next page) attached
            pass
        prev_p = b
        i += 1
    for e in els:
        e.pop("_open", None)
    # tidy paragraphs
    for e in els:
        if e["t"] in ("p", "li", "h"):
            e["text"] = clean(re.sub(r"\s+", " ", e["text"]).strip())
    # a short mono line inside a sentence is inline code, not a code block
    merged = []
    for k, e in enumerate(els):
        nxt = els[k + 1] if k + 1 < len(els) else None
        if (e["t"] == "code" and merged and merged[-1]["t"] in ("p", "li")
                and len(e["text"].splitlines()) <= 2 and len(e["text"]) < 160
                and not merged[-1]["text"].rstrip("*").endswith(":")
                and (re.search(r"[.,;)]$", e["text"].strip())
                     or (nxt is not None and nxt["t"] == "p" and re.match(r"^[a-z(]", nxt["text"])))):
            merged[-1]["text"] += " `" + " ".join(l.strip() for l in e["text"].splitlines()) + "`"
            if nxt is not None and nxt["t"] == "p" and re.match(r"^[a-z(]", nxt["text"]):
                nxt["_join"] = True
            continue
        if e.get("_join") and merged and merged[-1]["t"] in ("p", "li"):
            merged[-1]["text"] += " " + e["text"]
            continue
        e.pop("_join", None)
        merged.append(e)
    return merged


def build_tree(book, blocks):
    blocks = [b for b in blocks if not is_toc_row(b)]
    blocks = merge_headings(blocks)
    tree = {"book": book, "front": [], "chapters": []}
    part = None
    chapter = None
    section = None
    body = []
    started = False

    def flush():
        nonlocal body
        if not body:
            return
        els = build_elements(body)
        if section is not None:
            section["elements"].extend(els)
        elif chapter is not None:
            chapter["intro"].extend(els)
        else:
            tree["front"].extend(els)
        body = []

    # skip the table of contents pages: content starts at the first chapter
    # heading whose page is after the TOC.
    for b in blocks:
        h = classify_heading(book, b, started)
        if h is None or h[0] in ("sub", "front"):
            if h and h[0] == "sub":
                body.append(dict(b, k="h3"))
            else:
                body.append(b)
            continue
        kind, num, title = h
        if kind == "part":
            flush()
            part = f"Part {num}" + (f" — {title}" if title else "")
            continue
        if kind == "chapter":
            # ignore TOC echoes (same chapter appearing early on low pages)
            if b["p"] < {"api": 15, "linux": 14, "cicd": 5}.get(book, 0):
                body.append(b)
                continue
            if book == "python" and b["p"] < 6:
                continue
            flush()
            started = True
            chapter = {"num": num, "title": title.strip(), "page": b["p"], "part": part,
                       "intro": [], "sections": []}
            tree["chapters"].append(chapter)
            section = None
            continue
        if kind == "section":
            if chapter is None:
                body.append(b)
                continue
            flush()
            section = {"num": num, "title": title.strip(), "page": b["p"], "elements": []}
            chapter["sections"].append(section)
            continue
    flush()
    return tree


if __name__ == "__main__":
    src, dst = Path(sys.argv[1]), Path(sys.argv[2])
    dst.mkdir(parents=True, exist_ok=True)
    for f in sorted(src.glob("*.json")):
        book = f.stem
        tree = build_tree(book, json.loads(f.read_text()))
        (dst / f"{book}.json").write_text(json.dumps(tree, ensure_ascii=False, indent=1))
        n_sec = sum(len(c["sections"]) for c in tree["chapters"])
        print(f"{book}: {len(tree['chapters'])} chapters, {n_sec} sections")
