"""Stage 3: handbook trees -> app content bundle (content.json).

Produces lessons, interview questions, flashcards, quiz questions, scenarios,
coding challenges and glossary entries, every one with a SourceRef.

Usage: python3 build_content.py out/tree curated out/content.json raw/
"""
import hashlib
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

import pdfplumber

from topics import API_GROUPS, BANK_CHAPTERS, LINUX_TAGS, RESUME_TAG_SOURCE, RESUME_TAGS, ROLES, TOPICS, TRACKS

BOOKS = {
    "python": ("Python Automation Engineer Handbook", "Python Handbook", 1053),
    "api": ("API & Protocol Automation Handbook", "API & Protocol Handbook", 467),
    "linux": ("Linux, Bash & Embedded Debugging Handbook", "Linux & Bash Handbook", 288),
    "cicd": ("Git, Jenkins & CI/CD Handbook", "Git & CI/CD Handbook", 240),
    "bmcssd": ("BMC/SSD Interview Prep Handbook (from your resume)", "BMC/SSD Interview Prep", 98),
}

TRACK_ROLES = {t[0]: t[4] for t in TRACKS}
TOPIC_BY_ID = {t[0]: t for t in TOPICS}
CHAPTER_TOPIC = {}
for tid, track, title, chs, kw in TOPICS:
    for bc in chs:
        CHAPTER_TOPIC[bc] = tid


def sid(*parts) -> str:
    return hashlib.sha1("|".join(str(p) for p in parts).encode()).hexdigest()[:12]


def plain(t: str) -> str:
    return re.sub(r"\s+", " ", (t or "").replace("**", "")).strip()


def src(book, ch, sec, page):
    return {
        "book": book,
        "chapter": f"{ch['num']} {ch['title']}".strip() if ch else "",
        "section": f"{sec['num']} {sec['title']}".strip() if sec else "",
        "page": page,
    }


# ------------------------------------------------------------ markdown-lite
def els_to_md(els):
    """Elements -> markdown-lite understood by the app's renderer."""
    out = []
    for e in els:
        t = e["t"]
        if t == "h":
            out.append(f"### {plain(e['text'])}")
        elif t == "p":
            out.append(e["text"])
        elif t == "li":
            txt = e["text"]
            if txt.startswith(("•", "–")):
                txt = "- " + txt[1:].strip()
            out.append(txt)
        elif t == "code":
            out.append("```code\n" + e["text"] + "\n```")
        elif t == "out":
            out.append("```output\n" + e["text"] + "\n```")
        elif t == "table":
            rows = e["rows"]
            if out and out[-1].startswith("|"):
                out[-1] += "\n" + "\n".join("| " + " | ".join(plain(c) for c in r) + " |" for r in rows)
            else:
                out.append("\n".join("| " + " | ".join(plain(c) for c in r) + " |" for r in rows))
    md = "\n\n".join(x for x in out if x and x.strip())
    md = re.sub(r"\n{3,}", "\n\n", md)
    return md.strip()


# ------------------------------------------------------------ section roles
ROLE_PATTERNS = [
    ("interview", re.compile(r"^(Interview questions|Core Q&A|.*questions with (model )?answers|.*interview questions and answers|Practical Jenkins questions|Testing-oriented Python questions)", re.I)),
    ("assessment", re.compile(r"^(Chapter assessment|Final assessment)", re.I)),
    ("assessment_key", re.compile(r"^Answer key", re.I)),
    ("exercises", re.compile(r"^(Exercises?|Practical exercise|Lab\b)", re.I)),
    ("solutions", re.compile(r"^Solutions?\b", re.I)),
    ("summary", re.compile(r"^(Summary|Key takeaways)", re.I)),
    ("mistakes", re.compile(r"^Common mistakes", re.I)),
    ("best", re.compile(r"^Best practices", re.I)),
    ("flashcards", re.compile(r"^Revision flashcards", re.I)),
]


def section_role(title):
    for role, rx in ROLE_PATTERNS:
        if rx.match(title):
            return role
    return "lesson"


# ------------------------------------------------------------ Q&A parsing
QSTART = re.compile(
    r"^(?:[🔴🟡🟢⚪]\s*)?(?:(?P<n1>\d{1,3})\.\s+)?\*\*(?:(?P<n2>\d{1,3}(?:\.\d+)?)\.?\s+)?(?:\[(?P<tag>[A-Z]{1,5})\]\s+)?"
    r"(?:(?P<label>Q\d*|[A-Z]{1,2}\d{1,3})[.:]\s*)?(?P<q>.+?)\*\*\s*(?P<rest>.*)$", re.S)
FIELD_RE = re.compile(r"(?:(?<=^)|(?<=[.!?”\"’)\]]\s)|(?<=\n))(Answer|A|Follow-up|Follow-ups|Common wrong answer|Why|Key points|Ideal|Evaluating|Mistake|Likely follow-up)\s*:\s*")
RESUME_TAG_RE = re.compile(r"\[(ON RESUME|IMPLIED BY ROLE|NOT ON RESUME)[^\]]*\]")


def looks_like_question(m):
    q = plain(m.group("q"))
    if len(q) < 6:
        return False
    if m.group("label") or m.group("tag"):
        return True
    if q.endswith("?") or "?" in q[-3:]:
        return True
    if m.group("n1") or m.group("n2"):
        return q.endswith((".", "?", "”", '"')) and len(q) > 12
    return False


def parse_qa(els):
    """Split a list of elements into question items."""
    items = []
    cur = None
    for e in els:
        if e["t"] in ("p", "li"):
            m = QSTART.match(e["text"])
            if m and looks_like_question(m):
                q = plain(m.group("q"))
                rest = m.group("rest").strip()
                # bold question containing "**" itself (e.g. **kwargs): extend to the first "?**"
                full = e["text"]
                k = full.find("?**")
                if k != -1 and k + 3 > m.start("rest") - 2 and "?" not in q:
                    head = full[:k + 1]
                    q = plain(re.sub(r"^(?:[🔴🟡🟢⚪]\s*)?(?:\d{1,3}\.\s+)?\*\*(?:\d{1,3}\.?\s+)?(?:\[[A-Z]{1,5}\]\s+)?(?:(?:Q\d*|[A-Z]{1,2}\d{1,3})[.:]\s*)?", "", head))
                    rest = full[k + 3:].strip()
                # question may continue in bold after first bold run ("**...** **more?**")
                cur = {"q": q, "tag": m.group("tag"), "label": m.group("label") or m.group("n1") or m.group("n2"),
                       "parts": [{"t": "p", "text": rest, "page": e["page"]}] if rest else [], "page": e["page"]}
                items.append(cur)
                continue
        if cur is not None:
            if e["t"] == "h":
                cur = None
                continue
            cur["parts"].append(e)
    return items


def split_fields(md):
    """Return dict of labelled fields from an answer text."""
    fields = {"answer": ""}
    pos = 0
    key = "answer"
    for m in FIELD_RE.finditer(md):
        fields[key] = (fields.get(key, "") + " " + md[pos:m.start()]).strip()
        lab = m.group(1).lower()
        key = {"a": "answer", "answer": "answer", "ideal": "answer", "follow-up": "followup", "follow-ups": "followup",
               "likely follow-up": "followup", "common wrong answer": "wrong", "why": "why",
               "key points": "keypoints", "evaluating": "evaluating", "mistake": "mistake"}[lab]
        pos = m.end()
    fields[key] = (fields.get(key, "") + " " + md[pos:]).strip()
    return {k: v.strip() for k, v in fields.items() if v.strip()}


def keywords_from(text, limit=8):
    """Technical keywords for offline answer scoring: code-ish tokens and capitalised terms."""
    t = plain(text)
    cands = []
    cands += re.findall(r"`([^`]+)`", text)
    cands += re.findall(r"\b[A-Za-z_][\w.-]*\(\)?", t)
    toks = re.findall(r"[A-Za-z0-9_@/.+-]{2,}", t)
    stop = set("""the a an and or of to in is are be for on with as by it that this you your can not use when
    then than from at if no yes but which what why how do does was were will would should could their its into
    also only one two more most each any all same other so such may must very about after before over under
    there they them we our i me my just like have has had been being where who whom whose while because
    e.g eg i.e answer follow-up example""".split())
    scored = Counter()
    for tok in toks:
        low = tok.lower().strip(".")
        if low in stop or len(low) < 3:
            continue
        w = 1
        if re.search(r"[A-Z]{2,}|[_/@.()-]|\d", tok):
            w += 2
        scored[low] += w
    return [k for k, _ in scored.most_common(limit)]


_KW_RX = {}


def _kw_score(tid, kws, low):
    score = 0
    for k in kws:
        rx = _KW_RX.get(k)
        if rx is None:
            k2 = re.escape(k.lower())
            rx = re.compile((r"\b" if k[0].isalnum() else "") + k2 + (r"\b" if k[-1].isalnum() else ""))
            _KW_RX[k] = rx
        if rx.search(low):
            score += 2 if len(k) > 5 else 1
    return score


def classify_topic(text, default):
    """Keyword topic classifier; only overrides the chapter's topic with a clear margin."""
    low = text.lower()
    scores = {tid: _kw_score(tid, kws, low) for tid, track, title, chs, kws in TOPICS if kws}
    best = max(scores, key=scores.get)
    base = scores.get(default, 0)
    if scores[best] >= 3 and scores[best] >= base + 2:
        return best
    return default


DIFF_BY_LABEL = {"B": "EASY", "I": "MEDIUM", "A": "HARD", "E": "EXPERT"}


def question_type(q, topic, sec_title):
    ql = q.lower()
    st = (sec_title or "").lower()
    if topic in ("hr", "self-intro") or "behaviour" in st or re.search(r"tell me about a time|weakness|strength|why should we|salary|conflict|5 years", ql):
        return "BEHAVIORAL"
    if "scenario" in st or re.search(r"\b(fails?|failing|returns? \d{3}|not responding|stuck|intermittent|hangs?|disappears|after a .* update|walk me through)\b", ql):
        return "SCENARIO"
    if re.search(r"\b(write|implement|code|function that|what does this print|predict)\b", ql):
        return "CODING"
    if re.search(r"\b(debug|why does .* fail|what is wrong|bug)\b", ql):
        return "DEBUGGING"
    return "TECHNICAL"


# ------------------------------------------------------------ builder
class Builder:
    def __init__(self, trees, curated_dir):
        self.trees = trees
        self.curated = Path(curated_dir)
        self.lessons, self.questions, self.flashcards = [], [], []
        self.quiz, self.scenarios, self.coding, self.glossary = [], [], [], []
        self.topic_extra = defaultdict(lambda: {"summary": [], "keypoints": [], "mistakes": [], "objectives": [], "intro": ""})
        self.seen_q = {}

    # -- helpers
    def topic_for(self, book, ch):
        return CHAPTER_TOPIC.get((book, ch["num"]))

    def add_question(self, item, book, ch, sec, topic, difficulty="MEDIUM", personal=False, kind=None, extra=None):
        body = els_to_md(item["parts"])
        f = split_fields(body)
        ans = f.get("answer", "")
        if len(plain(ans)) < 3 and not f.get("keypoints"):
            return None
        if len(plain(ans)) < 3:
            ans = "Key points: " + f["keypoints"]
        q = item["q"]
        resume_tag = None
        m = RESUME_TAG_RE.search(q + " " + (ans if personal else ans[:80]))
        if m:
            resume_tag = {"ON RESUME": "ON_RESUME", "IMPLIED BY ROLE": "IMPLIED", "NOT ON RESUME": "NOT_ON_RESUME"}[m.group(1)]
            q = RESUME_TAG_RE.sub("", q).strip()
            ans = RESUME_TAG_RE.sub("", ans).strip()
        key = re.sub(r"[^a-z0-9]+", " ", q.lower()).strip()
        if key in self.seen_q:
            # duplicate across handbooks: keep the first, record the alternate source
            prev = self.seen_q[key]
            prev["altSources"].append(src(book, ch, sec, item["page"]))
            return prev
        qtype = kind or question_type(q, topic, sec["title"] if sec else "")
        track = TOPIC_BY_ID[topic][1]
        roles = list(TRACK_ROLES[track])
        if qtype in ("SCENARIO", "DEBUGGING") and "SR" not in roles:
            roles.append("SR")
        rec = {
            "id": "q-" + sid(book, ch["num"], q),
            "topicId": topic,
            "question": q,
            "answer": ans,
            "followUps": [x.strip() for x in re.split(r"(?<=\?)\s+(?=[A-Z“\"])", f.get("followup", "")) if x.strip()] if f.get("followup") else [],
            "commonWrong": f.get("wrong") or f.get("mistake"),
            "why": f.get("why") or f.get("evaluating"),
            "keyPoints": f.get("keypoints"),
            "keywords": keywords_from(ans + " " + (f.get("keypoints") or "")),
            "roles": roles,
            "difficulty": difficulty,
            "type": qtype,
            "personal": personal,
            "resumeTag": resume_tag,
            "source": src(book, ch, sec, item["page"]),
            "altSources": [],
        }
        if extra:
            rec.update(extra)
        self.seen_q[key] = rec
        self.questions.append(rec)
        return rec

    # -- lessons
    def build_lessons(self):
        order = Counter()
        for book, tree in self.trees.items():
            for ch in tree["chapters"]:
                topic = self.topic_for(book, ch)
                if not topic:
                    continue
                # chapter intro: objectives + first paragraph
                intro = ch["intro"]
                paras = [e for e in intro if e["t"] == "p" and len(plain(e["text"])) > 80]
                tx = self.topic_extra[topic]
                if paras and not tx["intro"]:
                    tx["intro"] = plain(paras[0]["text"])
                in_obj = False
                for e in intro:
                    if e["t"] == "p" and re.match(r"^\**Learning objectives", e["text"]):
                        in_obj = True
                        continue
                    if in_obj and e["t"] == "li":
                        tx["objectives"].append(plain(re.sub(r"^(•|\d+\.)\s*", "", e["text"])))
                    elif in_obj and e["t"] != "li":
                        in_obj = False
                sections = ch["sections"]
                if not sections and intro:
                    # chapters without numbered sections (projects, BMC prose) become one lesson
                    sections = [{"num": ch["num"], "title": ch["title"], "page": ch["page"], "elements": intro, "_whole": True}]
                for sec in sections:
                    role = section_role(sec["title"])
                    els = sec["elements"]
                    if role == "summary":
                        tx["summary"] += [plain(re.sub(r"^(•|\d+\.)\s*", "", e["text"])) for e in els if e["t"] in ("li", "p")]
                    if role == "mistakes":
                        tx["mistakes"] += [plain(re.sub(r"^(•|\d+\.)\s*", "", e["text"])) for e in els if e["t"] in ("li", "p") and len(plain(e["text"])) > 25]
                    if role == "best":
                        tx["keypoints"] += [plain(re.sub(r"^(•|\d+\.)\s*", "", e["text"])) for e in els if e["t"] in ("li",)]
                    if role in ("interview", "assessment", "assessment_key", "flashcards"):
                        continue
                    md = els_to_md(els)
                    if len(plain(md)) < 60:
                        continue
                    order[topic] += 1
                    words = len(plain(md).split())
                    kind = {"exercises": "PRACTICE", "solutions": "SOLUTIONS", "summary": "SUMMARY",
                            "mistakes": "MISTAKES", "best": "BEST_PRACTICES"}.get(role, "CONCEPT")
                    self.lessons.append({
                        "id": "l-" + sid(book, ch["num"], sec["num"], sec["title"]),
                        "topicId": topic,
                        "title": plain(sec["title"]),
                        "order": order[topic],
                        "kind": kind,
                        "body": md,
                        "readMinutes": max(1, round(words / 180)),
                        "hasCode": "```code" in md,
                        "source": src(book, ch, None if sec.get("_whole") else sec, sec["page"]),
                    })

    # -- interview questions from all Q&A sections
    def build_questions(self):
        for book, tree in self.trees.items():
            for ch in tree["chapters"]:
                topic0 = self.topic_for(book, ch)
                if not topic0:
                    continue
                is_bank = (book, ch["num"]) in BANK_CHAPTERS
                for sec in ch["sections"]:
                    role = section_role(sec["title"])
                    title = sec["title"]
                    take = role == "interview" or (is_bank and role == "lesson")
                    if book == "python" and ch["num"] in ("39", "40", "41"):
                        take = role == "lesson" and not re.match(r"^(How (to answer|embedded)|Common mistakes|Coding tasks)", title)
                    if not take:
                        continue
                    for item in parse_qa(sec["elements"]):
                        topic = topic0
                        diff = "MEDIUM"
                        label = item.get("label") or ""
                        if book == "linux" and item.get("tag"):
                            topic = LINUX_TAGS.get(item["tag"], classify_topic(item["q"], "rca"))
                            n = int(label) if label.isdigit() else 0
                            diff = "EASY" if n <= 45 else "MEDIUM" if n <= 90 else "HARD"
                        elif book == "api" and ch["num"] == "25":
                            topic = classify_topic(item["q"], API_GROUPS.get(sec["num"], "redfish"))
                            diff = "MEDIUM" if sec["num"] < "25.6" else "HARD"
                        elif book == "python" and ch["num"] == "39":
                            topic = classify_topic(item["q"], "py-interview")
                            diff = DIFF_BY_LABEL.get(label[:1], "MEDIUM")
                            if "print" in title.lower():
                                diff = "HARD"
                        elif book == "python" and ch["num"] in ("40", "41"):
                            topic = classify_topic(item["q"] + " " + title, topic0)
                            diff = "MEDIUM" if ch["num"] == "40" else "HARD"
                        elif book == "cicd" and ch["num"] in ("31", "32"):
                            topic = classify_topic(item["q"], "git-branching" if ch["num"] == "31" else "jenkins-pipelines")
                        elif is_bank:
                            topic = classify_topic(item["q"], topic0)
                        personal = book == "bmcssd"
                        self.add_question(item, book, ch, sec, topic, diff, personal=personal)

    # -- BMC handbook specific question sources (sections without numbered subsections)
    def build_bmc_questions(self):
        tree = self.trees["bmcssd"]
        for ch in tree["chapters"]:
            num = ch["num"]
            els = ch["intro"]
            if num in ("4", "16"):
                for item in parse_qa(els):
                    q = item["q"].strip('"“”')
                    item["q"] = q
                    topic = classify_topic(q, "likely-questions" if num == "4" else "cross-questioning")
                    if num == "16":
                        topic = "cross-questioning"
                        # cross-question answers use "→" instead of a label
                        if item["parts"] and item["parts"][0]["t"] == "p":
                            item["parts"][0]["text"] = re.sub(r"^→\s*", "", item["parts"][0]["text"])
                    self.add_question(item, "bmcssd", ch, None, topic, "MEDIUM", personal=True,
                                      kind="BEHAVIORAL" if num == "16" else None)
            if num == "15":
                for item in parse_qa(els):
                    self.add_question(item, "bmcssd", ch, None, "hr", "MEDIUM", personal=True, kind="BEHAVIORAL")

    # -- flashcards: assessments with answer keys, revision flashcard tables, glossaries, BMC top-50
    def build_flashcards(self):
        for book, tree in self.trees.items():
            for ch in tree["chapters"]:
                topic = self.topic_for(book, ch)
                secs = ch["sections"]
                for si, sec in enumerate(secs):
                    role = section_role(sec["title"])
                    if role == "assessment":
                        self._assessment_cards(book, ch, sec, secs[si + 1] if si + 1 < len(secs) else None, topic)
                    if role == "flashcards":
                        for e in sec["elements"]:
                            if e["t"] == "table":
                                for r in e["rows"]:
                                    if len(r) >= 2 and plain(r[0]) and plain(r[0]).lower() != "front":
                                        self._card(book, ch, sec, e["page"], plain(r[0]), plain(" ".join(r[1:])),
                                                   classify_topic(plain(r[0]) + " " + plain(r[1]), topic), "REVISION")
                # glossaries
                if ch["title"].strip() == "Glossary":
                    for e in ch["intro"]:
                        if e["t"] == "table":
                            for r in e["rows"]:
                                if len(r) >= 2 and plain(r[0]) and plain(r[0]).lower() not in ("term", "**term**"):
                                    term, d = plain(r[0]), plain(" ".join(r[1:]))
                                    self.glossary.append({"term": term, "definition": d,
                                                          "topicId": classify_topic(term + " " + d, "api-basics"),
                                                          "source": src(book, ch, None, e["page"])})

    def _card(self, book, ch, sec, page, front, back, topic, origin):
        if not front or not back or not topic:
            return
        self.flashcards.append({"id": "f-" + sid(book, front, back), "topicId": topic, "front": front, "back": back,
                                "origin": origin, "source": src(book, ch, sec, page)})

    def _assessment_cards(self, book, ch, sec, next_sec, topic):
        els = sec["elements"]
        qs, ans_text, ans_page = [], None, sec["page"]
        answers = {}
        mode = "q"
        for e in els:
            txt = e.get("text", "")
            if e["t"] == "p" and re.match(r"^\*\*Answers?:?\*\*", txt):
                mode = "a"
                ans_text = re.sub(r"^\*\*Answers?:?\*\*\s*", "", txt)
                ans_page = e["page"]
                continue
            if e["t"] == "h" and "Answer key" in txt:
                mode = "key"
                continue
            if e["t"] == "p" and re.match(r"^\*\*(Section [A-C]|[A-C]:)", txt) and mode == "key":
                continue
            if mode == "q" and e["t"] == "li":
                m = re.match(r"^(\d+)\.\s+(.*)$", plain(txt))
                if m:
                    qs.append((int(m.group(1)), m.group(2), e["page"]))
            elif mode == "q" and e["t"] in ("code", "out") and qs:
                n, t, p = qs[-1]
                qs[-1] = (n, t + "\n```code\n" + e["text"] + "\n```", p)
            elif mode == "a" and e["t"] == "p" and ans_text is not None and not re.match(r"^\*\*Practical", txt):
                ans_text += " " + txt
            elif mode == "key" and e["t"] == "li":
                m = re.match(r"^(\d+)\.\s+(.*)$", plain(txt))
                if m:
                    answers[int(m.group(1))] = (m.group(2), e["page"])
        # answer key in a following "Answer key" section
        if not answers and next_sec is not None and section_role(next_sec["title"]) == "assessment_key":
            for e in next_sec["elements"]:
                if e["t"] == "li":
                    m = re.match(r"^(\d+)\.\s+(.*)$", plain(e["text"]))
                    if m and int(m.group(1)) not in answers:
                        answers[int(m.group(1))] = (m.group(2), e["page"])
        # inline "**Answers:** 1 no, ... 2 else. 3 ..." format
        if ans_text and not answers:
            parts = re.split(r"(?:(?<=^)|(?<=[.;)\s]))\s*(\d{1,2})\s+(?=\S)", " " + plain(ans_text))
            # parts: ['', '1', 'no, the file...', '2', 'else.', ...]
            expect = 1
            buf = {}
            k = 1
            while k < len(parts) - 1:
                n = int(parts[k])
                if n == expect:
                    buf[n] = parts[k + 1].strip()
                    expect += 1
                elif n != expect and expect - 1 in buf:
                    buf[expect - 1] += f" {parts[k]} {parts[k + 1]}"
                k += 2
            answers = {n: (a.rstrip(". "), ans_page) for n, a in buf.items()}
        for n, q, p in qs:
            if n in answers:
                a = answers[n][0]
                qtopic = classify_topic(q, topic) if book in ("api", "linux", "cicd") and (topic in ("final-revision",)) else topic
                self._card(book, ch, sec, p, q, a, qtopic, "ASSESSMENT")

    # -- BMC tables via pdfplumber (top-50 revision, python quick answers)
    def build_bmc_tables(self, pdf_path):
        tree = self.trees["bmcssd"]
        chs = {c["num"]: c for c in tree["chapters"]}
        pdf = pdfplumber.open(pdf_path)

        def tables(p0, p1):
            for pn in range(p0, p1 + 1):
                for t in pdf.pages[pn - 1].extract_tables():
                    yield pn, t
        # Section 19: Top 50 quick revision (Question | Short answer | Keywords)
        ch = chs["19"]
        for pn, t in tables(77, 79):
            for r in t:
                if len(r) >= 4 and r[0] and r[0].strip().isdigit():
                    q, a, kw = (plain((r[1] or "").replace("\n", " ")), plain((r[2] or "").replace("\n", " ")),
                                plain((r[3] or "").replace("-\n", "-").replace("\n", " ")))
                    if q and a:
                        topic = classify_topic(q + " " + a, "final-revision")
                        self._card("bmcssd", ch, None, pn, q, a, topic, "QUICK_REVISION")
                        self.flashcards[-1]["keywords"] = [k.strip() for k in kw.split(",") if k.strip()]
        # Section 10.1/10.2 Python quick answers (Question | Answer to speak)
        ch = chs["10"]
        for pn, t in tables(42, 45):
            for r in t:
                if len(r) == 2 and r[0] and r[1] and r[0].strip() not in ("Question",):
                    q, a = plain(r[0].replace("\n", " ")), plain(r[1].replace("\n", " "))
                    if len(a) > 10:
                        self._card("bmcssd", ch, None, pn, q, a, classify_topic(q + " " + a, "py-interview"), "QUICK_REVISION")

    # -- scenarios
    def build_scenarios(self):
        LABEL = re.compile(r"^(?:\d+\.\s+|•\s*)?\*\*([A-Z][A-Za-z /’'-]{2,40}?)[.:]?\*\*[.:]?\s*(.*)$", re.S)
        def add(book, ch, sec, title, els, page, topic_default, difficulty="HARD"):
            steps = []
            for e in els:
                if e["t"] in ("p", "li"):
                    m = LABEL.match(e["text"])
                    if m:
                        steps.append({"label": plain(m.group(1)), "text": m.group(2).strip()})
                        continue
                    t = re.sub(r"^(•|\d+\.)\s*", "", e["text"]).strip()
                    if steps:
                        steps[-1]["text"] = (steps[-1]["text"] + "\n" + t).strip()
                    else:
                        steps.append({"label": "Context", "text": t})
                elif e["t"] in ("code", "out"):
                    if steps:
                        steps[-1]["text"] += "\n```code\n" + e["text"] + "\n```"
                elif e["t"] == "table" and steps:
                    steps[-1]["text"] += "\n" + els_to_md([e])
            if len(steps) < 2:
                return
            title = re.sub(r"^Scenario \d+\s*[—:-]\s*", "", plain(title)).strip("“”\" ")
            allt = " ".join(s["text"] for s in steps)
            topic = classify_topic(title + " " + allt[:400], topic_default)
            self.scenarios.append({
                "id": "s-" + sid(book, title),
                "topicId": topic,
                "title": title,
                "prompt": title if title.endswith("?") else f"{title}. How would you investigate?",
                "steps": steps,
                "keywords": keywords_from(allt, 12),
                "concept": "[concept]" in title,
                "difficulty": difficulty,
                "roles": list(TRACK_ROLES[TOPIC_BY_ID[topic][1]]),
                "source": src(book, ch, sec, page),
            })
        for book, chnum in (("api", "26"), ("linux", "30"), ("cicd", "33")):
            ch = next(c for c in self.trees[book]["chapters"] if c["num"] == chnum)
            for sec in ch["sections"]:
                if sec["title"].startswith("Scenario"):
                    add(book, ch, sec, sec["title"], sec["elements"], sec["page"], "scenario-banks")
        # Linux ch27 case studies
        ch = next(c for c in self.trees["linux"]["chapters"] if c["num"] == "27")
        for sec in ch["sections"]:
            if section_role(sec["title"]) == "lesson" and not sec["title"].lower().startswith(("exercise", "interview")):
                add("linux", ch, sec, sec["title"], sec["elements"], sec["page"], "case-studies")
        # BMC section 12: split intro on "Scenario N:" headings / bold paragraphs
        ch = next(c for c in self.trees["bmcssd"]["chapters"] if c["num"] == "12")
        cur_title, cur_els, cur_page = None, [], None
        for e in ch["intro"] + [{"t": "h", "text": "Scenario END:", "page": 0}]:
            txt = plain(e.get("text", ""))
            if e["t"] in ("h", "p") and re.match(r"^Scenario \d+:|^Scenario END:", txt) and (e["t"] == "h" or e["text"].startswith("**")):
                if cur_title:
                    add("bmcssd", ch, None, cur_title, cur_els, cur_page, "scenario-banks")
                cur_title, cur_els, cur_page = re.sub(r"^Scenario \d+:\s*", "", txt), [], e["page"]
                continue
            if cur_title:
                cur_els.append(e)
        # Python ch41.8 scenario questions are already interview questions (type SCENARIO)

    # -- coding challenges (Python ch42)
    def build_coding(self):
        ch = next(c for c in self.trees["python"]["chapters"] if c["num"] == "42")
        problems, solutions = {}, {}
        PSTART = re.compile(r"^\*\*([A-J]\d{1,2})\.\s+(.+?)\*\*\s*(?:\((Essential|Intermediate|Advanced)\))?\s*(.*)$", re.S)
        for sec in ch["sections"]:
            is_sol = sec["title"].startswith("Solutions to")
            if not (re.match(r"^Set [A-J]:", sec["title"]) or is_sol):
                continue
            set_name = re.sub(r"^(Solutions to )?Set ([A-J]): ", r"\2: ", sec["title"])
            cur = None
            for e in sec["elements"]:
                m = PSTART.match(e.get("text", "")) if e["t"] in ("p", "li") else None
                if m:
                    pid = m.group(1)
                    cur = {"pid": pid, "title": plain(m.group(2)), "level": m.group(3), "set": set_name,
                           "page": e["page"], "parts": [], "sec": sec}
                    if m.group(4).strip():
                        cur["parts"].append({"t": "p", "text": m.group(4).strip(), "page": e["page"]})
                    (solutions if is_sol else problems)[pid] = cur
                    continue
                if cur is not None:
                    cur["parts"].append(e)
        for pid, p in problems.items():
            s = solutions.get(pid)
            statement, examples, hint = [], [], None
            for e in p["parts"]:
                t = e.get("text", "")
                if e["t"] in ("code", "out") and plain(t).startswith("Example"):
                    examples.append(re.sub(r"^Examples?:\s*", "", t.strip()))
                elif e["t"] == "p" and plain(t).startswith("Hint:"):
                    hint = plain(t)[5:].strip()
                elif e["t"] == "p" and plain(t).startswith("Example"):
                    examples.append(re.sub(r"^Examples?:\s*", "", plain(t)))
                else:
                    statement.append(e)
            sol_code, complexity, notes = "", None, []
            if s:
                for e in s["parts"]:
                    if e["t"] in ("code", "out"):
                        sol_code += ("\n" if sol_code else "") + e["text"]
                    elif e["t"] == "p" and plain(e["text"]).startswith("Complexity"):
                        complexity = plain(e["text"])
                    elif e["t"] in ("p", "li"):
                        notes.append(plain(e["text"]))
            self.coding.append({
                "id": "c-" + pid.lower(),
                "code": pid,
                "set": p["set"],
                "topicId": "coding-sets",
                "title": p["title"],
                "difficulty": {"Essential": "EASY", "Intermediate": "MEDIUM", "Advanced": "HARD"}.get(p["level"], "MEDIUM"),
                "statement": els_to_md(statement),
                "examples": examples,
                "hint": hint,
                "solution": sol_code,
                "complexity": complexity,
                "notes": " ".join(notes) or None,
                "source": src("python", ch, p["sec"], p["page"]),
                "solutionSource": src("python", ch, s["sec"], s["page"]) if s else None,
            })

    # -- curated (hand-written, source-checked) quiz questions
    def _norm(self, t):
        t = (t or "").replace("**", "").replace("`", "")
        for a, c in (("’", "'"), ("‘", "'"), ("“", '"'), ("”", '"'), ("‐", "-"), ("–", "-"), ("—", "-"), ("\u00a0", " ")):
            t = t.replace(a, c)
        return re.sub(r"\s+", " ", t).strip().lower()

    def _index(self):
        if getattr(self, "_idx", None) is None:
            self._idx = []
            for book, tree in self.trees.items():
                for ch in tree["chapters"]:
                    for sec in [None] + ch["sections"]:
                        els = ch["intro"] if sec is None else sec["elements"]
                        for e in els:
                            txt = e.get("text") or " ".join(" | ".join(r) for r in e.get("rows", []))
                            self._idx.append((self._norm(txt), book, ch, sec, e["page"]))
        return self._idx

    def find_evidence(self, phrase):
        p = self._norm(phrase)
        for txt, book, ch, sec, page in self._index():
            if p in txt:
                return src(book, ch, sec, page)
        return None

    def load_curated(self):
        missing = []
        for f in sorted(self.curated.glob("*.json")):
            data = json.loads(f.read_text())
            for q in data.get("quiz", []):
                q.setdefault("origin", "DERIVED")
                q["id"] = q.get("id") or "z-" + sid(q["prompt"])
                ev = q.pop("evidence", None)
                s = self.find_evidence(ev) if ev else None
                if s is None:
                    missing.append((f.name, q["prompt"][:60], ev))
                    continue
                q["source"] = s
                q["evidenceQuote"] = ev
                self.quiz.append(q)
        if missing:
            print("CURATED ITEMS WITHOUT HANDBOOK EVIDENCE (dropped):")
            for m in missing:
                print("  ", m)

    # -- auto quiz items that stay 100% source-backed
    def auto_quiz(self):
        import random
        rnd = random.Random(42)
        # 1) code-output questions: Python Q&A answers that contain code + output
        for q in self.questions:
            m = re.search(r"```code\n(.*?)\n```\s*(?:Output:?\s*)?```output\n(.*?)\n```", q["answer"], re.S)
            if m and len(m.group(1)) < 700 and len(m.group(2)) < 200 and "input(" not in m.group(1):
                self.quiz.append({
                    "id": "z-out-" + q["id"][2:], "topicId": q["topicId"], "type": "CODE_OUTPUT",
                    "prompt": "What does this code print?", "code": m.group(1),
                    "answerText": m.group(2), "acceptable": [m.group(2)],
                    "explanation": plain(re.sub(r"```.*?```", "", q["answer"], flags=re.S))[:600],
                    "difficulty": "MEDIUM" if q["difficulty"] in ("EASY", "MEDIUM") else "HARD",
                    "origin": "HANDBOOK", "source": q["source"],
                })
        # 2) fill-in-the-blank from short assessment answers
        for c in self.flashcards:
            if c["origin"] in ("ASSESSMENT", "REVISION", "QUICK_REVISION"):
                ans = c["back"].strip().rstrip(".")
                if 1 <= len(ans.split()) <= 3 and len(ans) <= 32 and not c["front"].lower().startswith(("why", "explain", "describe", "write", "name two", "list")):
                    self.quiz.append({
                        "id": "z-fill-" + c["id"][2:], "topicId": c["topicId"], "type": "FILL_BLANK",
                        "prompt": c["front"], "answerText": ans,
                        "acceptable": sorted({ans, ans.lower(), re.sub(r"[()`]", "", ans)}),
                        "explanation": f"Handbook answer: {c['back']}",
                        "difficulty": "EASY", "origin": "HANDBOOK", "source": c["source"],
                    })
        # 3) glossary multiple choice + match-the-following (both sides are handbook text)
        by_book = defaultdict(list)
        for g in self.glossary:
            if 3 <= len(g["definition"]) <= 160 and len(g["term"]) <= 40:
                by_book[g["source"]["book"]].append(g)
        for book, gl in by_book.items():
            for g in gl:
                others = [o for o in gl if o["term"] != g["term"]]
                if len(others) < 3:
                    continue
                opts = rnd.sample(others, 3) + [g]
                rnd.shuffle(opts)
                self.quiz.append({
                    "id": "z-gl-" + sid(book, g["term"]), "topicId": g["topicId"], "type": "MCQ",
                    "prompt": f"Which term matches this definition?\n\n“{g['definition']}”",
                    "options": [o["term"] for o in opts], "correct": [opts.index(g)],
                    "explanation": f"{g['term']}: {g['definition']}",
                    "difficulty": "EASY", "origin": "HANDBOOK", "source": g["source"],
                    "note": "Options are other glossary terms from the same handbook.",
                })
            rnd.shuffle(gl)
            for i in range(0, len(gl) - 3, 4):
                grp = gl[i:i + 4]
                self.quiz.append({
                    "id": "z-match-" + sid(book, *[g["term"] for g in grp]), "topicId": grp[0]["topicId"],
                    "type": "MATCH", "prompt": "Match each term to its meaning.",
                    "pairs": [{"left": g["term"], "right": g["definition"]} for g in grp],
                    "explanation": "All pairs are taken from the handbook glossary.",
                    "difficulty": "MEDIUM", "origin": "HANDBOOK", "source": grp[0]["source"],
                })

    # -- predict-the-output questions from Python lessons (handbook shows the real output)
    def lesson_code_output(self):
        UNSAFE = re.compile(r"open\(|Path\(|socket|subprocess|requests|sleep|input\(|random|time\.|datetime\.now|os\.|threading|asyncio|serial|paramiko|tempfile|import (?!re\b|json\b|collections\b|functools\b|itertools\b|math\b|dataclasses\b|typing\b|string\b)")
        for book in ("python",):
            for ch in self.trees[book]["chapters"]:
                topic = self.topic_for(book, ch)
                if not topic or not ch["num"].isdigit() or int(ch["num"]) > 23 and ch["num"] != "39":
                    continue
                for sec in ch["sections"]:
                    els = sec["elements"]
                    for i in range(len(els) - 2):
                        a, lab, o = els[i], els[i + 1], els[i + 2]
                        if not (a["t"] == "code" and lab["t"] == "p" and plain(lab["text"]).startswith("Output") and o["t"] == "out"):
                            continue
                        code, out = a["text"], o["text"]
                        n_out = len(out.splitlines())
                        max_out = 9 if ch["num"] == "39" else 3
                        if len(code.splitlines()) > 16 or n_out > max_out or UNSAFE.search(code) or "0x7f" in out or "at 0x" in out:
                            continue
                        expl = ""
                        if i + 3 < len(els) and els[i + 3]["t"] == "p":
                            expl = plain(els[i + 3]["text"])[:500]
                        self.quiz.append({
                            "id": "z-out-" + sid(book, ch["num"], sec["num"], code), "topicId": topic, "type": "CODE_OUTPUT",
                            "prompt": "Predict the output of this code.", "code": code,
                            "answerText": out, "acceptable": [out],
                            "explanation": expl or f"Output as shown in the handbook ({sec['num']} {plain(sec['title'])}).",
                            "difficulty": "HARD" if ch["num"] == "39" else ("EASY" if int(ch["num"]) <= 10 else "MEDIUM"),
                            "origin": "HANDBOOK", "source": src(book, ch, sec, a["page"]),
                        })

    # -- keyword refinement with document frequency (used for offline answer scoring)
    def refine_keywords(self):
        import math
        docs = [q["answer"] for q in self.questions] + [" ".join(s["text"] for s in sc["steps"]) for sc in self.scenarios]
        df = Counter()
        tok = lambda t: set(w.lower().strip(".,;:()") for w in re.findall(r"[A-Za-z0-9_@/.+-]{3,}", plain(t)))
        for d in docs:
            df.update(tok(d))
        N = len(docs)
        stop = set("""the and for with that this from when then than you your are can not use which what into also only each
        any all same other such may must very about after before over under there they them our just like have has had been
        being where who whose while because answer example does did don’t it’s i’d i’m that’s isn’t won’t""".split())
        def kws(text, limit):
            tf = Counter(w.lower().strip(".,;:()") for w in re.findall(r"[A-Za-z0-9_@/.+-]{3,}", plain(text)))
            sc = {}
            for w, c in tf.items():
                if w in stop or w.isdigit():
                    continue
                idf = math.log((N + 1) / (df.get(w, 0) + 1))
                boost = 1.6 if re.search(r"[_/@.()-]|\d|^[a-z]+[A-Z]", w) else 1.0
                sc[w] = c * idf * boost
            return [w for w, _ in sorted(sc.items(), key=lambda x: -x[1])[:limit]]
        for q in self.questions:
            q["keywords"] = kws(q["answer"] + " " + (q.get("keyPoints") or ""), 8)
        for s in self.scenarios:
            s["keywords"] = kws(" ".join(st["text"] for st in s["steps"]), 14)

    # -- CICD final assessment section A: real MCQs with key
    def cicd_mcq(self):
        ch = next(c for c in self.trees["cicd"]["chapters"] if c["num"] == "34")
        sec = next(s for s in ch["sections"] if s["title"].startswith("Final assessment"))
        keysec = next(s for s in ch["sections"] if s["title"].startswith("Answer key"))
        keytxt = " ".join(plain(e.get("text", "")) for e in keysec["elements"])
        m = re.search(r"A:\s*(.*?)\s*B \(model", keytxt)
        key = dict(re.findall(r"(\d+)\s+([a-d])", m.group(1))) if m else {}
        cur = None
        items = []
        in_a = False
        for e in sec["elements"]:
            t = plain(e.get("text", ""))
            if "Section A" in t:
                in_a = True
                continue
            if "Section B" in t:
                break
            if not in_a:
                continue
            if e["t"] in ("code", "out") and cur is not None:
                t = cur.pop("_raw") + " " + plain(e["text"])
                cur["opts"] = []
                mq = None
            else:
                mq = re.match(r"^(\d+)\.\s+(.*)$", t)
            if mq:
                cur = {"n": mq.group(1), "q": mq.group(2), "opts": [], "page": e["page"]}
                items.append(cur)
                # options may be inline "(a) .. (b) .."
                t = mq.group(2)
            if cur is None:
                continue
            cur["_raw"] = t
            if cur is None:
                continue
            for om in re.finditer(r"(?:^|\s)\(?([a-d])\)\s+(.+?)(?=\s+\(?[a-d]\)\s+|$)", t):
                if not cur["opts"] or om.group(1) != cur["opts"][-1][0]:
                    cur["opts"].append((om.group(1), om.group(2)))
            if cur["opts"] and mq:
                cur["q"] = re.split(r"\s+\(?a\)\s+", cur["q"])[0]
        for it in items:
            if len(it["opts"]) >= 3 and it["n"] in key:
                letters = [o[0] for o in it["opts"]]
                if key[it["n"]] not in letters:
                    continue
                q = it["q"]
                topic = classify_topic(q + " " + " ".join(o[1] for o in it["opts"]), "jenkins-pipelines")
                self.quiz.append({
                    "id": "z-cicd-" + it["n"], "topicId": topic, "type": "MCQ", "prompt": q,
                    "options": [o[1] for o in it["opts"]], "correct": [letters.index(key[it["n"]])],
                    "explanation": "Answer key, Git & CI/CD Handbook final assessment.",
                    "difficulty": "MEDIUM", "origin": "HANDBOOK", "source": src("cicd", ch, sec, it["page"]),
                })

    def topics_json(self):
        out = []
        for i, (tid, track, title, chs, kw) in enumerate(TOPICS):
            tx = self.topic_extra[tid]
            sources = []
            for book, chnum in chs:
                ch = next((c for c in self.trees[book]["chapters"] if c["num"] == chnum), None)
                if ch:
                    sources.append(src(book, ch, None, ch["page"]))
            out.append({
                "id": tid, "trackId": track, "title": title, "order": i,
                "intro": tx["intro"],
                "objectives": tx["objectives"][:8],
                "summary": tx["summary"][:12],
                "keyPoints": tx["keypoints"][:12],
                "commonMistakes": tx["mistakes"][:10],
                "roles": TRACK_ROLES[track],
                "sources": sources,
                "resumeTag": RESUME_TAGS.get(tid),
                "resumeTagSource": RESUME_TAG_SOURCE if tid in RESUME_TAGS else None,
            })
        return out

    def bundle(self):
        return {
            "version": 1,
            "handbooks": [{"id": k, "title": v[0], "shortTitle": v[1], "pages": v[2]} for k, v in BOOKS.items()],
            "roles": [{"id": k, "title": v} for k, v in ROLES.items()],
            "tracks": [{"id": t[0], "title": t[1], "emoji": t[2], "description": t[3], "order": i, "roles": t[4]}
                       for i, t in enumerate(TRACKS)],
            "topics": self.topics_json(),
            "lessons": self.lessons,
            "questions": self.questions,
            "flashcards": self.flashcards,
            "quiz": self.quiz,
            "scenarios": self.scenarios,
            "coding": self.coding,
            "glossary": self.glossary,
        }


if __name__ == "__main__":
    tree_dir, curated, out = Path(sys.argv[1]), sys.argv[2], Path(sys.argv[3])
    raw_dir = Path(sys.argv[4]) if len(sys.argv) > 4 else tree_dir.parent.parent / "raw"
    trees = {b: json.loads((tree_dir / f"{b}.json").read_text()) for b in BOOKS}
    b = Builder(trees, curated)
    b.build_lessons()
    b.build_questions()
    b.build_bmc_questions()
    b.build_flashcards()
    b.build_bmc_tables(raw_dir / "bmcssd.pdf")
    b.build_scenarios()
    b.build_coding()
    b.load_curated()
    b.auto_quiz()
    b.lesson_code_output()
    b.cicd_mcq()
    b.refine_keywords()
    data = b.bundle()
    out.write_text(json.dumps(data, ensure_ascii=False))
    for k in ("topics", "lessons", "questions", "flashcards", "quiz", "scenarios", "coding", "glossary"):
        print(f"{k:12} {len(data[k])}")
    print("quiz types", Counter(q["type"] for q in data["quiz"]))
    print("q by book", Counter(q["source"]["book"] for q in data["questions"]))
    print("q types", Counter(q["type"] for q in data["questions"]))
