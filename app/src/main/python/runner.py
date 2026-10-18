"""On-device test runner for Coding Practice (executed through Chaquopy).

run(user_code, tests_json, timeout_s) executes the user's code, then each statement taken
from the handbook's "# tests" block in order. Statements containing `assert` are reported
as tests; other statements are setup. Returns a JSON string.
"""
import io
import json
import sys
import time
import traceback
from contextlib import redirect_stdout


class _Timeout(Exception):
    pass


def _tracer(deadline):
    def trace(frame, event, arg):
        if time.monotonic() > deadline:
            raise _Timeout("time limit exceeded (possible infinite loop)")
        return trace
    return trace


def _short_error(exc):
    if isinstance(exc, AssertionError):
        msg = str(exc)
        return "AssertionError" + (f": {msg}" if msg else ": expected value did not match")
    return f"{type(exc).__name__}: {exc}"


def run(user_code, tests_json, timeout_s=8.0):
    tests = json.loads(tests_json)
    out = io.StringIO()
    ns = {"__name__": "__main__"}
    result = {"compileError": None, "outcomes": [], "stdout": "", "timedOut": False}
    try:
        compiled = compile(user_code, "<your code>", "exec")
    except SyntaxError as e:
        result["compileError"] = f"SyntaxError on line {e.lineno}: {e.msg}"
        result["outcomes"] = [{"test": t, "passed": False, "message": "code did not compile"} for t in tests if "assert" in t]
        return json.dumps(result)
    deadline = time.monotonic() + float(timeout_s)
    old = sys.gettrace()
    sys.settrace(_tracer(deadline))
    try:
        with redirect_stdout(out):
            try:
                exec(compiled, ns)
            except _Timeout as e:
                result["timedOut"] = True
                result["compileError"] = str(e)
            except Exception as e:  # error while defining functions/classes
                result["compileError"] = "Error while running your code: " + _short_error(e)
            setup_error = result["compileError"]
            for t in tests:
                is_test = "assert" in t
                if setup_error:
                    if is_test:
                        result["outcomes"].append({"test": t, "passed": False, "message": setup_error})
                    continue
                try:
                    exec(compile(t, "<test>", "exec"), ns)
                    if is_test:
                        result["outcomes"].append({"test": t, "passed": True, "message": None})
                except _Timeout as e:
                    result["timedOut"] = True
                    setup_error = str(e)
                    if is_test:
                        result["outcomes"].append({"test": t, "passed": False, "message": str(e)})
                except Exception as e:
                    if is_test:
                        result["outcomes"].append({"test": t, "passed": False, "message": _short_error(e)})
                    else:
                        setup_error = "setup failed: " + _short_error(e)
    finally:
        sys.settrace(old)
    result["stdout"] = out.getvalue()[-4000:]
    return json.dumps(result)
