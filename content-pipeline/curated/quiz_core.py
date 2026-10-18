"""Hand-written quiz items. Correct answers come from the handbooks; each item carries an
`evidence` phrase that the pipeline must find verbatim in the handbook text (it fills the
source page from that match and fails the build if the phrase is missing). Distractors are
written for the app and the item is marked origin=DERIVED.

Run: python3 curated/quiz_core.py > curated/quiz_core.json
"""
import json

Q = []


def mcq(topic, prompt, options, correct, explanation, evidence, difficulty="MEDIUM", type_="MCQ", code=None):
    Q.append({"topicId": topic, "type": type_, "prompt": prompt, "options": options,
              "correct": correct if isinstance(correct, list) else [correct], "explanation": explanation,
              "evidence": evidence, "difficulty": difficulty, **({"code": code} if code else {})})


def tf(topic, statement, truth, explanation, evidence, difficulty="EASY"):
    Q.append({"topicId": topic, "type": "TRUE_FALSE", "prompt": statement, "options": ["True", "False"],
              "correct": [0 if truth else 1], "explanation": explanation, "evidence": evidence, "difficulty": difficulty})


def multi(topic, prompt, options, correct, explanation, evidence, difficulty="MEDIUM"):
    mcq(topic, prompt, options, correct, explanation, evidence, difficulty, type_="MULTI")


def scenario(topic, prompt, options, correct, explanation, evidence, difficulty="HARD"):
    mcq(topic, prompt, options, correct, explanation, evidence, difficulty, type_="SCENARIO")


def debug(topic, prompt, code, options, correct, explanation, evidence, difficulty="MEDIUM"):
    mcq(topic, prompt, options, correct, explanation, evidence, difficulty, type_="DEBUGGING", code=code)


# ----------------------------------------------------------------- Python core
mcq("py-basics", "How does CPython run your source code?",
    ["It compiles directly to machine code", "It compiles source to bytecode, then the PVM interprets it",
     "It interprets source text line by line with no compilation", "It translates Python to C and compiles that"], 1,
    "CPython compiles source to bytecode; the Python Virtual Machine then interprets the bytecode.",
    "CPython compiles source to bytecode, then the PVM interprets it", "EASY")
tf("py-basics", "Python and CPython are two names for exactly the same thing.", False,
   "Python is the language specification; CPython is its reference implementation in C.",
   "Language specification versus its reference implementation in C")
mcq("py-datastructures", "Why is `x in my_set` usually faster than `x in my_list`?",
    ["Sets are sorted, so they use binary search", "Hashing gives average O(1) lookup versus O(n) scanning",
     "Lists store values on disk", "Sets cache the last lookup"], 1,
    "A set is a hash table: membership is an average O(1) hash lookup, while a list is scanned element by element.",
    "Hashing gives average O(1) lookup versus O(n) scanning", "EASY")
multi("py-datastructures", "Which of these can be dictionary keys? (Select all that apply)",
      ["str", "list", "tuple of hashables", "frozenset", "dict"], [0, 2, 3],
      "Any hashable object can be a key: str, int, tuple of hashables, frozenset. Lists, dicts and sets cannot.",
      "Any hashable object: str, int, tuple of hashables, frozenset. Not lists, dicts or sets.")
mcq("py-datastructures", "What does a shallow copy of a nested list share with the original?",
    ["Nothing — it is fully independent", "The nested (inner) objects", "Only the outer list object", "Only immutable elements"], 1,
    "A shallow copy duplicates only the outer container; nested objects are shared. A deep copy recurses.",
    "Shallow copies the outer container; nested objects are shared. Deep copies recursively.")
debug("py-functions", "This helper sometimes returns readings from earlier calls. What is the bug?",
      "def collect(reading, acc=[]):\n    acc.append(reading)\n    return acc",
      ["append() returns None", "The default list is created once at definition time and shared across calls",
       "acc must be declared global", "Lists cannot be default arguments at all"], 1,
      "Defaults are evaluated once at definition time, so a mutable default is shared across calls. Use acc=None and create the list inside.",
      "Defaults are evaluated once at definition time; mutable defaults are shared across calls. Use None.")
mcq("py-functions", "What does LEGB describe?",
    ["The order of exception handlers", "The name lookup order: Local, Enclosing, Global, Built-in",
     "The garbage collector generations", "The import search path"], 1,
    "LEGB is the scope lookup order for names.", "Local, Enclosing, Global, Built-in lookup order", "EASY")
mcq("py-functions", "What causes UnboundLocalError?",
    ["Reading a global variable inside a function", "Assigning to a name anywhere in a function makes it local for the whole function",
     "Calling a function before it is defined", "Deleting a variable twice"], 1,
    "If a function assigns to a name anywhere, Python treats that name as local throughout the function, so reading it before the assignment fails.",
    "Assigning to a name anywhere in a function makes it local for the whole function")
mcq("py-iteration", "List comprehension versus generator expression — which statement is correct?",
    ["Both build the full list in memory", "A generator yields values lazily one at a time and is single-use",
     "A generator can be iterated any number of times", "List comprehensions are lazy"], 1,
    "A list comprehension is eager; a generator expression is lazy, saves memory, can stop early but is single-use.",
    "Eager list in memory versus lazy one-at-a-time values; generators save memory and can stop early but are single-use")
mcq("py-iteration", "What does `yield` do inside a function?",
    ["Ends the function permanently", "Produces a value and pauses the function, keeping its state until the next request",
     "Starts a new thread", "Returns a list of all values"], 1,
    "yield turns the function into a generator: it produces a value and pauses with its state preserved.",
    "Produces a value and pauses the function, keeping its state until the next request", "EASY")
mcq("py-modules", "What happens when you import a module for the first time?",
    ["The file is executed every time import runs", "Cache check in sys.modules, search sys.path, execute the file once, bind the name",
     "Python compiles it to a .exe", "Only the functions are loaded; top-level code never runs"], 1,
    "Imports are cached in sys.modules, so the module body runs once per process.",
    "Cache check in sys.modules, search sys.path, execute the file once, bind the name")
mcq("py-exceptions", "In try/except/else/finally, when does the `else` block run?",
    ["Always", "Only when an exception occurred", "Only when no exception occurred", "Only if finally is absent"], 2,
    "else runs only on success; finally always runs and is where cleanup goes.",
    "else runs only on success; finally always runs, for cleanup", "EASY")
mcq("py-exceptions", "Why is a bare `except:` bad practice?",
    ["It is a syntax error in Python 3", "It catches KeyboardInterrupt/SystemExit and hides bugs",
     "It only catches ValueError", "It makes finally skip"], 1,
    "A bare except catches everything including KeyboardInterrupt and SystemExit, hiding real bugs.",
    "Catches KeyboardInterrupt/SystemExit and hides bugs", "EASY")
mcq("py-exceptions", "What does `raise ConfigError(...) from e` do?",
    ["Suppresses e completely", "Sets __cause__, preserving the root cause in tracebacks", "Retries the failed operation", "Converts e into a warning"], 1,
    "Exception chaining with `from` sets __cause__ so the traceback shows the original error.",
    "raise X from e sets __cause__, preserving the root cause in tracebacks")
tf("py-exceptions", "`except Exception` stops Ctrl+C (KeyboardInterrupt).", False,
   "KeyboardInterrupt, SystemExit and GeneratorExit derive directly from BaseException, not Exception.",
   "derive from it directly, **not** from Exception, so except Exception does not stop Ctrl+C.")
mcq("py-files", "Which file mode fails if the file already exists?",
    ['"w"', '"a"', '"x"', '"r+"'], 2,
    '"w" truncates, "a" appends, "x" fails if the file exists.', "Truncate, append, fail-if-exists.", "EASY")
mcq("py-files", "How should you process a 10 GB log file in Python?",
    ["f.read() into one string", "Iterate the file object line by line, or read binary chunks", "Load it with json.load", "Copy it to /tmp first"], 1,
    "Iterating the file object streams lines; read() would load the whole file into memory.",
    "Iterate the file object line by line, or read binary chunks; never read().")
mcq("py-oop", "@classmethod versus @staticmethod — which is correct?",
    ["Both receive the instance", "classmethod receives the class (alternative constructors); staticmethod receives nothing",
     "staticmethod receives the class", "They are identical"], 1,
    "A classmethod gets the class as first argument and is used for alternative constructors; a staticmethod is a namespaced helper.",
    "Receives the class (alternative constructors) versus receives nothing (namespaced helper).")
mcq("py-oop", "What does super() call?",
    ["Always the base object class", "The next class in the MRO", "The child class method", "A random parent"], 1,
    "super() calls the next class in the method resolution order, typically the parent's method.",
    "Calls the next class in the MRO, typically the parent’s method, e.g. __init__.")
tf("py-oop", "A mutable class variable (for example a list defined in the class body) is shared by all instances.", True,
   "Class variables are shared by all instances; beware mutable class variables.",
   "Shared by all instances versus per object; beware mutable class variables.")
mcq("py-functional", "What does `@d` above `def f(): ...` mean?",
    ["f = d", "f = d(f)", "d = f(d)", "f is registered in a global list"], 1,
    "A decorator is a callable that takes a function and returns a replacement; @d means f = d(f).",
    "A callable taking a function and returning a replacement; @d means f = d(f).", "EASY")
mcq("py-functional", "Why use functools.wraps in a decorator?",
    ["To make the wrapper faster", "To preserve the wrapped function's name, docstring and signature info",
     "To catch exceptions automatically", "It is required for decorators to work"], 1,
    "wraps copies metadata such as __name__, __doc__ and sets __wrapped__.", "Preserves name, docstring, signature info")
multi("py-functional", "A retry decorator should consider which of these? (Select all that apply)",
      ["Retry only transient exceptions", "Bounded attempts with backoff", "Swallow the final error silently", "Idempotency of the operation"],
      [0, 1, 3], "Retry only transient errors, bound attempts, back off, log, re-raise the last error, and consider idempotency.",
      "Transient exceptions only, bounded attempts, backoff and jitter, max delay, logging, re-raise last error, idempotency.")
mcq("py-regex", "re.search vs re.match vs re.fullmatch?",
    ["All three match the entire string", "search: anywhere; match: at the start; fullmatch: the entire string",
     "match: anywhere; search: at the start", "fullmatch ignores case"], 1,
    "search scans anywhere, match anchors at the start, fullmatch requires the whole string.",
    "Anywhere, at the start, the entire string.", "EASY")
mcq("py-regex", "In a regex, how do `.*` and `.*?` differ?",
    [".* is lazy, .*? is greedy", ".* takes as much as possible; .*? as little as possible", "They are identical", ".*? matches only digits"], 1,
    "Greedy versus non-greedy quantifiers.", ".* takes as much as possible; .*? as little as possible.", "EASY")
mcq("py-stdlib", "What is the safe way to run an external command from Python?",
    ["os.system with an f-string", "subprocess.run with an argument list, timeout, captured output and return-code check",
     "subprocess.run(cmd, shell=True) with user input", "eval() the command string"], 1,
    "Use an argument list (no shell=True with untrusted input), a timeout, capture output and check the return code.",
    "subprocess.run with an argument list, timeout, captured output, return-code check; no shell=True with untrusted input.")
mcq("py-concurrency", "Which tool fits CPU-bound work in standard CPython?",
    ["Threads", "Processes", "asyncio", "Generators"], 1,
    "Threads suit blocking I/O with moderate fan-out, processes suit CPU-bound work, asyncio suits very high concurrency with async libraries.",
    "Blocking I/O with moderate fan-out / CPU-bound work / very high concurrency with async libraries.")
mcq("py-concurrency", "How do you get an exception raised inside a ThreadPoolExecutor worker?",
    ["It is printed and lost", "future.result() re-raises it", "It crashes the main thread immediately", "Use threading.excepthook only"], 1,
    "Calling result() on the future re-raises the worker's exception; handle it per future.",
    "future.result() re-raises them; handle per future.")
tf("py-quality", "Type hints are enforced by Python at runtime.", False,
   "Type hints are not enforced at runtime; checkers like mypy use them. Validate external data separately.",
   "No; checkers and tools use them. Validate external data separately.")
mcq("py-performance", "How does CPython free most objects?",
    ["Only a periodic mark-and-sweep", "Reference counting frees objects immediately; a cyclic GC frees unreachable cycles",
     "Objects are never freed until exit", "The OS frees them at random"], 1,
    "Reference counting handles most objects; the cyclic garbage collector handles reference cycles.",
    "Reference counting frees objects immediately; a cyclic GC frees unreachable cycles")

# ----------------------------------------------------------------- pytest / automation
mcq("pytest", "What is the difference between a failure and an error in pytest?",
    ["They are the same", "Failure: the test body failed. Error: setup or teardown failed",
     "Error: an assert failed. Failure: an exception was raised", "Errors only happen in CI"], 1,
    "pytest reports a failure when the test body fails and an error when a fixture's setup or teardown fails.",
    "Failure: the test body failed. Error: setup or teardown failed.")
mcq("pytest", "When does the code after `yield` in a fixture run?",
    ["Only when the test passes", "After the test, whether it passed or failed (but not if setup itself raised before yield)",
     "Before the test", "Only at the end of the session"], 1,
    "Teardown after yield runs after the test regardless of outcome, unless setup raised before reaching yield.",
    "Code after yield runs after the test whether it passed or failed (but not if setup itself raised before yield).")
multi("pytest", "Which are valid pytest fixture scopes? (Select all that apply)",
      ["function", "class", "thread", "module", "session"], [0, 1, 3, 4],
      "Scopes are function, class, module, package and session; wide scopes share state.",
      "function/class/module/package/session; wide scopes share state and create order dependence.")
mcq("pytest", "How does conftest.py make fixtures available?",
    ["You must import it in each test", "pytest loads conftest.py files automatically from the root down to each test's directory",
     "Only if listed in setup.cfg", "Only for tests in the same file"], 1,
    "Fixtures and hooks in conftest.py are available to all tests below it without imports.",
    "pytest loads conftest.py files automatically from the root directory down to each test’s directory")
mcq("mocking", "Where should you patch a function with unittest.mock.patch?",
    ["Where it is defined", "Where the name is looked up at run time: the module that uses it", "In conftest.py only", "In builtins"], 1,
    "Patch the name in the namespace where the code under test looks it up.",
    "Where the name is looked up at run time: the module that uses it.")
mcq("mocking", "If both return_value and side_effect are set on a Mock, which wins?",
    ["return_value", "side_effect", "Whichever was set last", "Neither — it raises"], 1,
    "side_effect can be a sequence, an exception or a function, and it wins if both are set.",
    "side_effect wins if both are set.")
mcq("mocking", "Why use autospec when mocking?",
    ["It makes mocks faster", "So mocks reject attributes and call signatures the real object does not have",
     "It records network traffic", "It patches all modules at once"], 1,
    "autospec keeps mocks honest by enforcing the real API.", "So mocks reject attributes and call signatures the real object does not have.")
mcq("framework", "How do you run the same tests against a simulator and real hardware?",
    ["Duplicate every test", "Configuration selects adapters through a registry; tests use the domain layer only",
     "Use if/else on hostname in each test", "Only possible with Robot Framework"], 1,
    "Tests talk to a domain layer; configuration picks the adapter (simulator or hardware).",
    "Configuration selects adapters through a registry; tests use the domain layer only.")
mcq("logging-reporting", 'Why prefer log.info("x=%s", x) over an f-string?',
    ["f-strings are not allowed in logging", "Lazy formatting (only if emitted) and stable message templates for aggregation",
     "It is shorter", "It encrypts the message"], 1,
    "The logging module formats only if the record is emitted and keeps a stable template.",
    "Lazy formatting (only if emitted) and stable message templates for aggregation.")
mcq("browser", "Why does Playwright need fewer explicit waits than Selenium?",
    ["It runs slower", "Actions auto-wait for actionability, and expect assertions retry until a timeout",
     "It disables JavaScript", "It uses screenshots instead of the DOM"], 1,
    "Auto-waiting and retrying assertions remove most manual waits.",
    "Actions auto-wait for actionability, and expect assertions retry until a timeout.")
mcq("reliability", "Why is verify=False dangerous in requests?",
    ["It slows requests down", "It disables server authentication; trust the lab CA bundle instead", "It disables JSON parsing", "It turns off timeouts"], 1,
    "Disabling verification removes server authentication; use the lab CA bundle and fix certificates.",
    "It disables server authentication; trust the lab CA bundle and fix certificates.")
tf("reliability", "yaml.load and pickle.loads are safe to use on data received from a device.", False,
   "Both can execute code; use yaml.safe_load and plain data formats.", "Both can execute code; use safe_load and plain data formats.")
mcq("cli-automation", "How can you tell an SSH connection failure from a remote command failure?",
    ["ssh always exits 0", "ssh exits with 255 for its own errors; otherwise it returns the remote command's code",
     "Check stderr for the word 'error'", "Remote failures exit with 127"], 1,
    "Exit code 255 means ssh itself failed.", "ssh exits with 255 for its own errors; otherwise it returns the remote command’s code.")
mcq("test-design", "What makes a good negative test?",
    ["It sends many invalid inputs at once", "It sends one specific invalid input, asserts the specific defined error and checks no side effect occurred",
     "It only checks that something failed", "It disables error handling"], 1,
    "One invalid input, the specific defined error (status, completion code, message ID), and no side effect.",
    "It sends one specific invalid input, asserts the specific defined error")

# ----------------------------------------------------------------- HTTP / Redfish
mcq("http-rest", "PUT versus PATCH — which is correct?",
    ["PUT changes only the properties sent", "PUT replaces the entire resource representation; PATCH changes only the properties sent",
     "PATCH deletes the resource", "They are identical in Redfish"], 1,
    "Redfish uses PATCH because resources contain many read-only and server-computed properties.",
    "PUT replaces the entire resource representation with the body; omitted properties are removed or reset. PATCH changes only the properties sent.")
mcq("requests", "What is the difference between HTTP 401 and 403?",
    ["401: forbidden; 403: not found", "401: not authenticated; 403: authenticated but not permitted",
     "Both mean the server is down", "403 means the session expired"], 1,
    "401 means missing or bad credentials; 403 means you are known but not allowed.",
    "401: not authenticated (missing or bad credentials). 403: authenticated but not permitted.", "EASY")
mcq("http-rest", "What does HTTP 202 Accepted from a Redfish action mean?",
    ["The operation finished successfully", "Work continues asynchronously; poll the task monitor in Location",
     "The request was rejected", "The resource was created"], 1,
    "202 means asynchronous processing; poll the task monitor with a deadline, check TaskState/TaskStatus, then verify the effect.",
    "202 Accepted, meaning work continues asyn")
mcq("requests", "What is the default timeout in Python requests?",
    ["30 seconds", "5 seconds", "No timeout", "60 seconds"], 2,
    "The default is no timeout, so a hung server blocks the test forever — always set one.",
    "The default is no timeout; a hung server would block the test forever.")
mcq("redfish", "Which header carries the Redfish session token?",
    ["Authorization: Bearer", "X-Auth-Token", "Cookie: SESSION", "X-Redfish-Session"], 1,
    "POST credentials to the Sessions collection, receive X-Auth-Token and Location; DELETE the Location to log out.",
    "POST credentials to the Sessions collection, receive X-Auth-Token and Location", "EASY")
mcq("redfish", "How do you read every member of a paginated Redfish collection?",
    ["Use $top=1000", "Follow Members@odata.nextLink until it disappears and compare with Members@odata.count",
     "Read only the first page", "Call the collection with PUT"], 1,
    "Follow nextLink, compare counts, add a page limit and check for duplicates.",
    "Read Members and follow Members@odata.nextLink until it disappears")
mcq("redfish", "A PATCH with a stale ETag in If-Match should return:",
    ["200", "404", "412 Precondition Failed", "500"], 2,
    "ETags give optimistic concurrency; a stale If-Match is rejected with 412.",
    "returns 412 Precondition Failed instead of overwriting another client’s change")
tf("redfish", "The Redfish service root /redfish/v1/ is readable without authentication.", True,
   "So clients can discover the service and its capabilities before having credentials.",
   "So clients can discover the service and its capabilities before having credentials.")
scenario("redfish", "A Redfish GET returns 500 Internal Server Error on an OpenBMC system. What is the most useful next step to find which layer is at fault?",
         ["Retry the request until it succeeds", "Read the same property with busctl to compare D-Bus with the Redfish response",
          "Reflash the BMC immediately", "Switch to IPMI and close the bug"], 1,
         "bmcweb translates Redfish to D-Bus. If D-Bus returns the data, the bug is in bmcweb's handler; if D-Bus errors or the daemon is dead, it is in the backend.",
         "bmcweb is a D-Bus-to-Redfish translator")
mcq("redfish", "How should automation pick the allowed values for a ComputerSystem.Reset action?",
    ["Hard-code ForceRestart", "Read ResetType@Redfish.AllowableValues (or ActionInfo)", "Try every string until one works", "Use IPMI instead"], 1,
    "Allowed values are advertised by the service; tests should read them rather than hard-coding.",
    "allowed values are listed in ResetType@Redfish.AllowableValues")

# ----------------------------------------------------------------- IPMI / RMCP+ / PLDM / MCTP / I2C / UART / SSH
mcq("ipmi", "In IPMI, what NetFn does a response use?",
    ["The same NetFn as the request", "Request NetFn + 1", "Always 0x00", "Request NetFn × 2"], 1,
    "The response uses NetFn + 1, the same command, and data starting with a completion code.",
    "The response uses NetFn + 1")
mcq("ipmi", "What is the difference between in-band and out-of-band IPMI?",
    ["In-band uses the network; out-of-band uses KCS", "In-band goes from the host OS via KCS/BT/SSIF; out-of-band uses RMCP+ on UDP 623 and works with the host off",
     "They are the same path", "Out-of-band requires the host OS to run"], 1,
    "Out-of-band works over the BMC's network with the host powered off.",
    "Out-of-band goes over the BMC’s network connection using RMCP+ on UDP 623 and works with the host powered off.")
mcq("ipmi", "Why can't you interpret Get Sensor Reading without SDRs?",
    ["The reading is encrypted", "The command returns a raw 8-bit value; the SDR provides units, conversion factors and thresholds",
     "SDRs store the reading history", "Sensors only report via SEL"], 1,
    "The SDR gives M, B, K1, K2, linearisation, thresholds, name and type.", "The command returns a raw 8-bit value.")
mcq("ipmi", "What does `ipmitool chassis power soft` do?",
    ["Immediate power off", "Requests an orderly OS shutdown (ACPI)", "Power-cycles the host", "Resets the BMC"], 1,
    "off is immediate; soft requests an orderly OS shutdown; cycle powers off then on; reset is a hard reset.",
    "soft requests an orderly OS shutdown (ACPI)")
mcq("rmcp", "Which IPMI cipher suite does the handbook recommend where supported?",
    ["0", "3", "17", "1"], 2,
    "Use 17 (HMAC-SHA256, HMAC-SHA256-128, AES-CBC-128) where supported, 3 otherwise; ensure 0 is disabled.",
    "Use 17 (HMAC-SHA256, HMAC-SHA256-128, AES-CBC-128) where supported, 3 otherwise; ensure 0 is disabled.")
mcq("mctp", "What is the relationship between MCTP and PLDM?",
    ["PLDM is the transport; MCTP is a data model", "MCTP is the transport; PLDM is one of the message types it carries (type 0x01)",
     "They are competing standards", "MCTP only carries Redfish"], 1,
    "MCTP handles EID addressing, routing, packetisation; PLDM defines management data and commands.",
    "MCTP is the transport; PLDM is one of the message types it carries (type 0x01).")
mcq("mctp", "How does an MCTP endpoint on SMBus usually get its EID?",
    ["It picks a random EID", "The bus owner (typically the BMC) assigns it with Get/Set Endpoint ID, or static EIDs are used",
     "From DHCP", "It is burned in at the factory only"], 1,
    "The bus owner uses MCTP control messages to assign EIDs, or both sides use static EIDs from platform configuration.",
    "The bus owner, typically the BMC, addresses the device at its SMBus address with MCTP control messages")
mcq("pldm", "Why does PLDM firmware update use a pull model?",
    ["It is faster on the network", "The firmware device requests chunks at offsets it chooses, so it controls pacing and buffering",
     "The BMC cannot push data", "To avoid encryption"], 1,
    "The device controls pacing, buffering and retransmission according to its flash constraints.",
    "The firmware device requests chunks at offsets it chooses, so it controls pacing, buffering and retransmission")
mcq("pldm", "What is the PLDM instance ID used for?",
    ["Encrypting messages", "Matching responses to outstanding requests and discarding stale ones", "Selecting the PLDM type", "Numbering firmware components"], 1,
    "A 5-bit value echoed by the responder; tests should verify the echo.",
    "It lets the requester match responses to outstanding requests and discard stale ones.")
mcq("i2c", "What does a NACK right after the I²C address byte mean?",
    ["The data was corrupted", "No device accepted that address — absent, unpowered, busy or wrong address",
     "The bus speed is too high", "The device is in SMBus mode"], 1,
    "The receiver pulls SDA low on the ninth clock to ACK; a NACK after the address means no device accepted it.",
    "A NACK right after the address byte means no device accepted that address")
tf("i2c", "SMBus is a stricter profile of I²C with defined transactions and timeouts so a stuck device can't hang the bus forever.", True,
   "SMBus adds defined transaction types, timeouts, a minimum clock, optional PEC and an alert line.",
   "SMBus is a stricter profile of I²C for system management")
mcq("uart", "What does 115200 8N1 mean?",
    ["115200 bytes/s, 8 stop bits", "115200 bits/s, 8 data bits, no parity, 1 stop bit", "115200 baud, 8 parity bits, 1 start bit", "1152 frames per second"], 1,
    "10 bits on the wire per byte, so about 11,520 bytes per second.",
    "115200 bits per second, 8 data bits, no parity, 1 stop bit", "EASY")
multi("uart", "A UART console shows garbage. Which should you check? (Select all that apply)",
      ["Baud rate and framing", "Voltage levels", "TX/RX swapped or missing ground", "The Redfish session timeout"], [0, 1, 2],
      "Check baud/framing, voltage levels, TX/RX, ground, flow control and whether it is the right port.",
      "Baud rate and framing (115200 8N1 is common but not universal), voltage levels")
mcq("ssh", "How do you verify that a BMC actually rebooted?",
    ["Ping it once", "Compare /proc/sys/kernel/random/boot_id (or uptime) before and after", "Check the date", "Look at the SSH banner"], 1,
    "boot_id changes every boot; also wait for SSH to go down and come back within a deadline, then check readiness.",
    "Compare /proc/ sys/kernel/random/boot_id (or uptime) before and after")
scenario("ssh", "A command works when you run it manually over SSH but fails in Jenkins. What is the most likely class of cause?",
         ["The BMC firmware is broken", "A different environment: non-interactive shell profile, PATH, no TTY, different key or user",
          "Jenkins cannot run SSH", "The command is too long"], 1,
         "Non-interactive shells don't read the same profile files; check PATH, TTY, keys, user, known hosts and network.",
         "Different environment: non-interactive shells don’t read the same profile files")

# ----------------------------------------------------------------- OpenBMC / D-Bus / firmware
mcq("openbmc-arch", "How does a Redfish request flow through OpenBMC?",
    ["Client → ipmid → kernel", "Client → HTTPS → bmcweb → D-Bus calls to owning services → JSON assembled by bmcweb",
     "Client → U-Boot → systemd", "Client → MCTP → PLDM → bmcweb"], 1,
    "bmcweb authenticates and routes, then reads/calls D-Bus services and builds the JSON response.",
    "Client → HTTPS → bmcweb (authentication, routing to the Redfish handler) → D-Bus calls")
mcq("dbus", "What does the D-Bus error ServiceUnknown indicate?",
    ["The object path is wrong", "No connection owns that well-known name — the service isn't running or hasn't claimed it",
     "Permission denied", "The property type is wrong"], 1,
    "ServiceUnknown means nobody owns the bus name.",
    "No connection owns that well-known name — the service isn’t running or hasn’t claimed it.")
mcq("dbus", "In D-Bus, what identifies the set of methods/properties/signals on an object?",
    ["Bus name", "Object path", "Interface", "Unit file"], 2,
    "Bus name = service connection; object path = object; interface = set of methods/properties/signals.",
    "interface identifies the set of methods/properties/signals on that object.")
multi("bmc-health", "To validate a sensor end to end, which values should you compare? (Select all that apply)",
      ["sysfs raw value", "D-Bus property", "Redfish reading", "Git commit hash"], [0, 1, 2],
      "Compare sysfs, D-Bus and Redfish; check units, scaling, availability flags and timing.",
      "Compare sysfs raw value, D-Bus property and Redfish reading")

# ----------------------------------------------------------------- NVMe/SSD
mcq("nvme", "Which NVMe log page ID is the SMART / Health Information log?",
    ["01h", "02h", "03h", "0Dh"], 1,
    "Error Information is 01h, SMART/Health is 02h, Firmware Slot Information is 03h.",
    "SMART / Health Information log (02h)")
mcq("nvme", "In NVMe Firmware Commit, which commit action activates the image at the next reset?",
    ["CA 0", "CA 1", "CA 2", "Commit actions don't exist"], 1,
    "Action 1 activates at next reset; action 3 activates immediately without reset if supported.",
    "Action 1 activates at next reset")
mcq("nvme", "In which unit does the SMART/Health log report composite temperature?",
    ["Celsius", "Fahrenheit", "Kelvin", "Raw ADC counts"], 2,
    "Composite temperature is reported in Kelvin.", "composite temperature (in Kelvin)", "EASY")
mcq("nvme", "What is an NVMe namespace?",
    ["A Linux mount point", "A separately addressable LBA range with its own format", "A firmware slot", "A PCIe lane"], 1,
    "Created with Namespace Management and attached with Namespace Attachment.",
    "It's a separately addressable LBA range with its own format")
scenario("nvme", "After nvme fw-commit with commit action 1 and a reboot, the firmware revision is unchanged. Which check fits the handbook's guidance?",
         ["Assume the drive is broken and replace it", "Check the firmware slot log (fw-log) and id-ctrl FR field, and whether the reset type actually activated the slot",
          "Run fio for 24 hours", "Downgrade the BMC"], 1,
         "Confirm what is running with id-ctrl FR and fw-log; activation depends on the commit action and reset.",
         "Then verify with fw-log and id-ctrl")

# ----------------------------------------------------------------- Linux & Bash
mcq("linux-proc", "Why prefer SIGTERM over SIGKILL?",
    ["SIGTERM is faster", "SIGTERM lets the process clean up; SIGKILL cannot be caught", "SIGKILL only works on zombies", "There is no difference"], 1,
    "SIGTERM allows flushing logs, removing lock files and closing connections.",
    "SIGTERM lets the process clean up (flush logs, remove lock files, close connections). SIGKILL cannot be caught")
mcq("linux-proc", "How do you remove a zombie process?",
    ["kill -9 the zombie", "Fix or restart the parent so it reaps the exit status", "Reboot is the only way", "Delete its /proc entry"], 1,
    "A zombie is already dead; its parent must reap it.",
    "You remove it by fixing/restarting the parent; killing the zombie itself does nothing.")
mcq("linux-net", "Connection refused versus timeout — which is correct?",
    ["Refused means the host is down", "Refused: RST came back, host reachable but nothing listening; timeout: no reply",
     "Timeout means wrong password", "They mean the same"], 1,
    "Refused = reachable, no listener (or reject rule). Timeout = dropped packets, routing issue or host down.",
    "Refused: an RST came back — host reachable, nothing listening (or reject rule). Timeout: no reply")
mcq("linux-storage", '"No space left on device" but df -h shows free space. What should you check first?',
    ["CPU load", "Inode exhaustion with df -i", "The network", "SELinux"], 1,
    "Inode exhaustion, quotas, or the path being on a different, full filesystem.",
    "Inode exhaustion ( df -i), quota limits")
mcq("linux-logs", "How do you view logs from the previous boot?",
    ["dmesg -c", "journalctl -b -1 (if the journal is persistent)", "cat /var/log/boot", "systemctl --failed"], 1,
    "On BMCs the journal is often volatile, so serial captures or remote logging may be the only record.",
    "journalctl -b -1, provided the journal is persistent.")
tf("linux-logs", "`dmesg -c` is a good first command when investigating, because it clears old noise.", False,
   "dmesg -c clears the kernel buffer — never during an investigation.", "Clears the buffer — never during investigation")
mcq("systemd", "What is start-limit-hit?",
    ["A unit started more times than StartLimitBurst within StartLimitIntervalSec", "The unit has no ExecStart", "A disk quota", "A D-Bus timeout"], 0,
    "systemd stops trying until reset or the interval expires.",
    "The unit started more times than StartLimitBurst within StartLimitIntervalSec")
mcq("systemd", "In systemd, After= expresses:",
    ["A strong dependency", "Ordering only", "A conflict", "A timer"], 1,
    "After= is ordering only; Requires= is a strong dependency.", "Ordering only", "EASY")
mcq("kernel", "Oops versus panic?",
    ["Same thing", "Oops: kernel bug detected, offending task killed, system may continue; panic: fatal", "Panic is recoverable", "Oops only happens at boot"], 1,
    "An oops taints the kernel but the system may continue; a panic halts or reboots.",
    "Oops: kernel bug detected, offending task killed, system may continue (tainted). Panic: fatal")
mcq("bash-basics", "What does `2>&1` do?",
    ["Redirects stdout to stderr", "Redirects fd 2 (stderr) to wherever fd 1 currently points", "Runs two commands", "Appends to file 1"], 1,
    "It duplicates stderr onto stdout's current target.", "Redirects fd 2 (stderr) to wherever fd 1 currently points.", "EASY")
mcq("bash-basics", "Why might `a | b` report success although `a` failed?",
    ["Pipes ignore errors by design and nothing can change that", "Pipeline status is the last command's unless pipefail is set",
     "Because b ran in a subshell", "Because of set -u"], 1,
    "set -o pipefail makes the pipeline fail if any stage fails.", "Pipeline status is the last command’s unless pipefail is set.")
mcq("bash-basics", "Single versus double quotes in Bash?",
    ["Identical", "Single quotes are fully literal; double quotes allow $, ` and \\ expansions", "Double quotes are literal", "Single quotes allow variable expansion"], 1,
    "Double quotes still prevent word splitting and globbing.", "Single quotes are fully literal.", "EASY")
mcq("text-processing", "Why must `uniq` follow `sort` when counting duplicates?",
    ["uniq only collapses adjacent duplicates", "sort removes duplicates itself", "uniq requires numeric input", "It doesn't matter"], 0,
    "uniq works on adjacent lines.", "uniq only collapses adjacent duplicates.", "EASY")
debug("bash-reliable", "This cleanup line in a script is dangerous. What is the safest fix from the handbook?",
      'rm -rf "$DIR"/*',
      ["Use rm -rf $DIR/* without quotes", 'Use ${DIR:?}, validate the path against expected prefixes, and use --',
       "Add sudo", "Run it twice"], 1,
      "If DIR is empty, the command becomes rm -rf /*. ${DIR:?} aborts on an empty variable.",
      "Use ${DIR:?}, validate the path against expected prefixes, and use --.")
multi("bash-reliable", "Why isn't `set -e` enough on its own? (Select all that apply)",
      ["It is disabled in conditional contexts", "It ignores failures inside pipelines without pipefail", "It exits silently", "It disables trap"],
      [0, 1, 2], "Explicit checks are still needed.",
      "It’s disabled in conditional contexts and functions called from them, ignores failures in pipelines")
mcq("scheduling", "How do you stop a cron job from overlapping with itself?",
    ["Use sleep 60", "flock -n on a lock file", "Run it as root", "Use crontab -r"], 1,
    "The kernel releases a flock when the process exits.", "Use flock -n on a lock file")
mcq("scheduling", "A script works interactively but not from cron. Most likely cause?",
    ["cron can't run Bash", "Different environment: PATH, working directory, shell, missing variables, no TTY", "The disk is full", "cron runs scripts twice"], 1,
    "cron runs with a minimal environment.", "Different environment: PATH, working directory, shell, missing variables, no TTY.")

# ----------------------------------------------------------------- Git & CI/CD
mcq("git-basics", "git fetch versus git pull?",
    ["Same command", "Fetch downloads and updates origin/* without touching your branch; pull is fetch plus merge (or rebase)",
     "Pull only downloads tags", "Fetch rewrites history"], 1,
    "Fetch is always safe; pull integrates into the current branch.",
    "Fetch downloads commits and updates origin/* without touching your branch. Pull is fetch plus merge (or rebase) into the current branch.")
mcq("git-basics", "How do you undo a commit already pushed to main?",
    ["git reset --hard and force-push", "git revert", "Delete the branch", "git commit --amend"], 1,
    "revert adds an inverse commit without rewriting history.", "git revert, which adds an inverse commit without rewriting history.", "EASY")
mcq("git-branching", "What does --force-with-lease add over --force?",
    ["It is faster", "It only overwrites the remote branch if it is still where you last saw it", "It signs commits", "It skips hooks"], 1,
    "You cannot silently destroy a colleague's newer commits.", "It only overwrites the remote branch if it is still where you last saw it")
tf("git-branching", "It is fine to rebase commits that are already on a shared branch.", False,
   "Rebasing changes hashes and breaks everyone else's history.",
   "When the commits are already on a shared branch; rebasing changes their hashes")
mcq("git-workflows", "How do you recover a commit after git reset --hard?",
    ["It is gone forever", "git reflog to find the hash, then reset or branch at it", "git stash pop", "git fetch --all"], 1,
    "The reflog keeps where HEAD has been.", "git reflog to find the hash, then git reset --hard <hash> or create a branch at it.")
mcq("git-troubleshooting", "A secret was pushed to Git. What is the first action?",
    ["Rewrite history with filter-repo", "Revoke/rotate the secret", "Delete the repository", "Make the repo private"], 1,
    "History cleanup comes after, because copies may already exist.", "Revoke/rotate it. History cleanup comes after")
mcq("jenkins-basics", "Controller versus agent in Jenkins?",
    ["Agents store credentials and serve the UI", "The controller schedules and stores config/results; agents execute build steps",
     "They are the same process", "Agents schedule builds"], 1,
    "Builds should not run on the controller.", "The controller schedules, stores configuration, credentials and results, and serves the UI; agents execute build steps")
mcq("jenkins-pipelines", "How does the handbook make test failures yellow and infrastructure errors red?",
    ["Ignore all failures", "Tolerate pytest exit code 1 via returnStatus, fail on others, let junit mark UNSTABLE",
     "Use catchError everywhere", "Run tests twice"], 1,
    "Exit code 1 means tests ran and found failures (UNSTABLE); other codes mean infrastructure problems (FAILURE).",
    "Tolerate pytest exit code 1 via returnStatus, fail on others, let junit mark UNSTABLE.")
mcq("jenkins-scale", "Does setting executors to 1 guarantee only one job uses a BMC?",
    ["Yes, always", "Only if that BMC is reachable from just that agent and only one job type uses it; use a resource lock",
     "No, executors don't limit builds", "Only on Windows agents"], 1,
    "Use Lockable Resources for real exclusivity.", "Only if that BMC is reachable from just that agent and only one job type uses it.")
mcq("jenkins-security", "How should a password be used in a Jenkins pipeline?",
    ["Hard-code it in the Jenkinsfile", "Credentials store + withCredentials in the smallest scope; single-quoted sh; never echo or Groovy-interpolate",
     "Pass it as a build parameter", "Store it in the repo encrypted"], 1,
    "Keep secrets out of logs and out of Groovy string interpolation.",
    "Store it in the Credentials store, bind it with withCredentials in the smallest scope")
mcq("embedded-ci", "Why run a health check before the test suite in an OpenBMC pipeline?",
    ["To warm up the CPU", "To report an unhealthy lab as one infrastructure failure instead of hundreds of misleading test failures",
     "Jenkins requires it", "To update firmware"], 1,
    "A health gate separates infrastructure problems from product failures.",
    "To report an unavailable or unhealthy lab as one clear infrastructure failure instead of hundreds of misleading test failures.")
mcq("cicd-concepts", "What belongs in a BMC smoke suite?",
    ["Destructive firmware downgrades", "Fast, broad, non-destructive checks: interfaces reachable, BMC ready, key services active, expected version",
     "48-hour soak tests", "Only UI screenshots"], 1,
    "Smoke is fast and broad; destructive and long suites run separately.",
    "Fast, broad, non-destructive checks: each interface reachable")
mcq("python-in-ci", "What is a good quarantine policy for flaky tests?",
    ["Delete them", "Move them to a visible quarantine job with an owner and deadline, keep collecting data, fix the root cause",
     "Retry until green", "Mark them skipped forever"], 1,
    "Quarantine must never become a graveyard.",
    "Flaky tests move to a visible quarantine job with an owner and a deadline")

# ----------------------------------------------------------------- Debugging & RCA
mcq("rca", "Symptom versus root cause?",
    ["Same thing", "A symptom is what you observe; a root cause is the mechanism that, when removed, prevents the symptom",
     "Root cause is the first error in the log", "A symptom is always hardware"], 1,
    "A root cause must be confirmed by controlled evidence.",
    "A symptom is what you observe; a root cause is the mechanism that, when removed, prevents the symptom")
mcq("rca", "How do you avoid confirmation bias during debugging?",
    ["Trust the first hypothesis", "Write competing hypotheses and test the predictions that would disprove the favourite one",
     "Ask the developer", "Reboot first"], 1,
    "Discriminating tests separate hypotheses.", "Write competing hypotheses and actively test the predictions that would disprove the favourite one.")
scenario("protocol-failures", "Tests fail only between 2 and 3 AM. What should you suspect first?",
         ["Random cosmic rays", "Scheduled activity: backups, log rotation, other nightly jobs, certificate renewals, NTP or DHCP changes",
          "The test code has a typo", "A broken Redfish schema"], 1,
         "Correlate failures with schedules and system logs.",
         "Scheduled activity: backups, log rotation, other teams’ nightly jobs on the same BMC or network")
scenario("protocol-failures", "How do you tell whether a failure is in the test or the firmware?",
         ["Ask the firmware team", "Use the 2×2 matrix: same test code on previous firmware and vice versa; reproduce manually; check the spec",
          "Rerun until it passes", "Close it as flaky"], 1,
         "Hold one variable fixed and vary the other; check whether the expectation is backed by a requirement.",
         "compare against the previous firmware with the same test code and vice versa (the 2×2 matrix)")
scenario("hw-fw-debug", "A hardware test fails once in 200 runs. What is the handbook's approach?",
         ["Mark it flaky and move on", "Treat it as a possible product issue: capture full evidence on failure and run a campaign large enough to reproduce several times",
          "Increase the timeout to 1 hour", "Disable the test in CI"], 1,
         "Look for common conditions (temperature, time since boot, preceding steps) across failures.",
         "Treat it as a possible product issue first: capture full evidence on failure")
scenario("boot", "During a 1,000-cycle reboot campaign, cycle 437 fails and the next cycle passes. What do you report?",
         ["Nothing — it passed on retry", "Treat it as a real intermittent failure: collect console, BMC journal/event log, timing and temperature data for cycle 437",
          "Restart the campaign from zero", "Report 99.9% pass and close"], 1,
         "An intermittent failure is a real failure until proven otherwise.",
         "Treat it as a real intermittent failure until proven otherwise.")

# ----------------------------------------------------------------- DSA
mcq("dsa", "How do you get the 10 largest of a million values efficiently?",
    ["sorted(values)[-10:]", "heapq.nlargest(10, values)", "max() ten times", "A nested loop"], 1,
    "heapq.nlargest is O(n log 10).", "heapq.nlargest(10, values): O(n log 10).")
mcq("dsa", "When should you use a deque instead of a list?",
    ["For random access by index", "For queues and anything that adds or removes at the front", "For sorting", "For hashing"], 1,
    "deque has O(1) appends and pops at both ends.", "For queues and anything that adds or removes at the front.", "EASY")

# ----------------------------------------------------------------- Testing fundamentals (BMC handbook §6)
mcq("test-design", "When smoke fails on a new firmware build, what should you do?",
    ["Run full regression anyway", "Stop, don't run full regression, report the build as rejected with logs, and confirm it isn't environmental",
     "Reflash and retry silently", "Skip the failing tests"], 1,
    "Check the previous build on the same bench to rule out the environment.",
    "stop, don't run full regression, report the build as rejected with logs")

if __name__ == "__main__":
    print(json.dumps({"quiz": Q}, ensure_ascii=False, indent=1))
