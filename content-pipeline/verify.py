"""Stage 4: execute handbook code to verify extracted items.
- CODE_OUTPUT quiz items: run the code, compare stdout with the handbook output.
- Coding challenges: run solution + each test assert.
Items that fail are repaired (wrapped-line join) or dropped/flagged.
"""
import ast, contextlib, io, json, multiprocessing as mp, re, sys

def run(code, q):
    buf = io.StringIO()
    try:
        with contextlib.redirect_stdout(buf):
            exec(compile(code, "<x>", "exec"), {"__name__": "__main__"})
        q.put(("ok", buf.getvalue()))
    except BaseException as e:
        q.put(("err", buf.getvalue() + f"{type(e).__name__}: {e}"))

def execute(code, timeout=5):
    q = mp.Queue(); p = mp.Process(target=run, args=(code, q)); p.start(); p.join(timeout)
    if p.is_alive():
        p.kill(); return ("timeout", "")
    return q.get() if not q.empty() else ("err", "no result")

def norm(s):
    return "\n".join(l.rstrip() for l in s.strip().splitlines())

def fix(code):
    """PDF line-wrapping breaks long lines. Join the line a SyntaxError points at with
    the next line, repeatedly. Lines ending in a backslash are joined without a space."""
    lines = code.split("\n")
    # pre-pass 1: backslash at the end of a line inside an open string literal was a wrapped escape ("\\n")
    # pre-pass 2: a wrapped trailing comment leaves a bare word line ("trial")
    out = []
    for ln in lines:
        if out and out[-1].endswith("\\") and (out[-1].count('"') % 2 == 1 or out[-1].count("'") % 2 == 1):
            out[-1] = out[-1] + ln.lstrip()
        elif out and "  #" in out[-1] and re.fullmatch(r"[A-Za-z][A-Za-z ,.'()-]*", ln.strip() or "1") and ln.strip() not in ("pass", "break", "continue", "return", "raise", "else:", "try:", "finally:"):
            out[-1] = out[-1] + " " + ln.strip()
        elif out and out[-1].startswith((" ", "\t")) and not ln.startswith((" ", "\t")) and ln.strip() and re.search(r"[,(\[{=+\-*/]\s*$", out[-1]):
            out[-1] = out[-1] + " " + ln.strip()
        else:
            out.append(ln)
    lines = out
    def err_line(src):
        try:
            compile(src, "<x>", "exec")
            return None
        except SyntaxError as e:
            return (e.lineno or 1) - 1

    def join(ls, i):
        nxt = ls[i + 1]
        j = ls[i] + nxt.lstrip() if ls[i].endswith("\\") else ls[i] + " " + nxt.lstrip()
        return ls[:i] + [j] + ls[i + 2:]

    def search(ls, depth):
        e = err_line("\n".join(ls))
        if e is None:
            return ls
        if depth == 0:
            return None
        for i in (e - 1, e):
            if 0 <= i < len(ls) - 1 and not ls[i].lstrip().startswith("#"):
                r = search(join(ls, i), depth - 1)
                if r is not None:
                    return r
        return None

    r = search(lines, 8)
    if r is not None:
        return "\n".join(r)
    return None

if __name__ == "__main__":
    path = sys.argv[1]
    d = json.load(open(path))
    keep, stats = [], {"out_ok": 0, "out_fixed": 0, "out_drop": 0}
    for q in d["quiz"]:
        if q["type"] != "CODE_OUTPUT":
            keep.append(q); continue
        code = fix(q["code"])
        if code is None:
            stats["out_drop"] += 1; continue
        st, out = execute(code)
        exp = norm(q["answerText"])
        if st == "ok" and norm(out) == exp:
            stats["out_ok" if code == q["code"] else "out_fixed"] += 1
            q["code"] = code; q["verified"] = True; keep.append(q)
        elif st == "ok" and norm(out).replace("\n", " ") == exp.replace("\n", " "):
            # handbook output was wrapped by the PDF layout; use the real output
            q["code"] = code; q["answerText"] = norm(out); q["acceptable"] = [norm(out)]; q["verified"] = True
            stats["out_fixed"] += 1; keep.append(q)
        else:
            stats["out_drop"] += 1
    d["quiz"] = keep
    cstats = {"ok": 0, "fixed": 0, "fail": 0}
    for c in d["coding"]:
        code = c["solution"]
        if "# tests" not in code:
            c["tests"] = []; c["verified"] = False; cstats["fail"] += 1; continue
        fixed = fix(code)
        if fixed is None:
            c["tests"] = []; c["verified"] = False; cstats["fail"] += 1; continue
        sol, tests_src = fixed.split("# tests", 1)
        try:
            tree = ast.parse(tests_src)
        except SyntaxError:
            c["tests"] = []; c["verified"] = False; cstats["fail"] += 1; continue
        src_lines = tests_src.splitlines()
        def seg(n):
            start = min([n.lineno] + [d.lineno for d in getattr(n, "decorator_list", [])])
            return "\n".join(src_lines[start - 1:n.end_lineno])
        tests, last_span = [], None
        for n in tree.body:
            span = (min([n.lineno] + [d.lineno for d in getattr(n, "decorator_list", [])]), n.end_lineno)
            if last_span and span[0] <= last_span[1]:
                continue  # several statements on one line ("a(); b()") form one test step
            tests.append(seg(n)); last_span = span
        st, out = execute(fixed)
        c["solution"] = sol.rstrip()
        c["tests"] = tests
        c["verified"] = st == "ok"
        # function name the user must implement
        names = [n.name for n in ast.parse(sol).body if isinstance(n, (ast.FunctionDef, ast.ClassDef))]
        c["entryPoints"] = names
        cstats["ok" if st == "ok" and fixed == code else "fixed" if st == "ok" else "fail"] += 1
        if st != "ok":
            c["verifyError"] = out[-300:]
    json.dump(d, open(path, "w"), ensure_ascii=False)
    print(stats, cstats)
