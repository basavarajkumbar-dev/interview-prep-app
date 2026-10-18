"""Second batch of hand-written quiz items: more True/False, multiple-answer, scenario and
debugging questions. Same rules as quiz_core.py: the correct answer comes from the handbook
and `evidence` must appear verbatim in the handbook text (checked by build_content.py).

Run: python3 curated/quiz_more.py > curated/quiz_more.json
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from quiz_core import Q, debug, mcq, multi, scenario, tf  # noqa: E402

Q.clear()

# ----------------------------------------------------------------- True / False
tf("nvme", "A SMART composite temperature of about 310 means the drive is overheating at 310 °C.", False,
   "SMART reports temperature in Kelvin: 310 K is about 37 °C.", "It's in Kelvin, so 310 K ≈ 37 °C")
tf("fw-update", "In an A/B update scheme you update the inactive slot, boot it, and fall back to the known-good slot if it fails.", True,
   "That is the definition the handbook gives for A/B firmware updates.", "Update the inactive slot; boot it; fall back to the known-good slot if it fails.")
tf("fw-update", "If a downgrade to an older firmware is rejected, it is always a defect.", False,
   "It may be a downgrade-protection policy or version check; check updater logs and documentation first.",
   "Possibly a downgrade-protection policy or version check")
tf("ipmi", "Saving the SEL before clearing it matters because clearing is destructive.", True,
   "The SEL is prime evidence after failures; save before clearing.", "Save before clearing; clearing is destr")
tf("uart", "/dev/ttyUSB0 is a stable name for a USB serial adapter across re-plugs.", False,
   "ttyUSB numbering depends on enumeration order; use /dev/serial/by-id/ paths, which are stable.",
   "They are stable names based on the USB adapter’s identity")
tf("pldm", "The PLDM datagram (D) bit marks a request for which the requester expects no response.", True,
   "Tests must not wait for responses to datagrams.", "It marks an unacknowledged request — the requester expects no response.")
tf("mctp", "In PLDM over MCTP, the EID and the TID are the same identifier.", False,
   "EID is the MCTP transport address; TID is the PLDM terminus identifier. Tests should not confuse them.",
   "EID is the MCTP endpoint address at the transport layer; TID is the PLDM terminus identifier")
tf("systemd", "After editing a unit file you must run systemctl daemon-reload so systemd re-reads it.", True,
   "daemon-reload is needed after creating or editing unit files.", "After creating or editing unit files, so systemd re-reads them.")
tf("linux-logs", "\"Suppressed N messages\" in the journal means evidence may be incomplete.", True,
   "journald rate limiting dropped messages from a noisy unit.", "journald’s rate limiting dropped messages from a noisy unit; evidence may be incomplete.")
tf("kernel", "Raw i2c-tools access on a live BMC is safer than reading the value through sysfs.", False,
   "Sysfs goes through the bound driver with proper locking; raw access can race with drivers or change hardware state.",
   "Sysfs reads go through the bound driver with proper locking")
tf("pytest", "Code coverage tells you whether behaviour was checked correctly.", False,
   "Coverage shows which lines ran, not whether behaviour was checked correctly.", "Which lines ran; not whether behaviour was checked correctly.")
tf("py-concurrency", "Calling time.sleep inside a coroutine is fine because it only pauses that coroutine.", False,
   "time.sleep blocks the event loop and therefore all tasks.", "It blocks the event loop and all tasks.")
tf("python-in-ci", "Hiding a flaky test with automatic retries is the recommended fix.", False,
   "Find the cause (timing, order, shared state, environment, real defect), fix or quarantine with a ticket; do not hide with retries.",
   "do not hide with retries")
tf("rca", "If you only have a hypothesis when a manager asks for the root cause, you should say so explicitly.", True,
   "State symptom, first error, leading hypothesis, evidence for/against, next test and ETA. Don't overstate.",
   "Say so explicitly: symptom, first error, leading hypothesis")
tf("redfish", "In Redfish, a missing optional property in a 200 response is automatically a test failure.", False,
   "Many properties are optional; absence usually means not implemented unless requirements or a profile mandate it.",
   "In Redfish many properties are optional, so absence usually means “not implemented”.")

# ----------------------------------------------------------------- Multiple answer
multi("fw-update", "How do you verify a firmware update was applied? (Select all that apply)",
      ["Running version on multiple layers (Redfish, /etc/os-release)", "Boot ID changed", "Health equal to baseline, settings preserved", "The update task said 100%"],
      [0, 1, 2], "The task percentage alone proves nothing; verify version, boot ID, health and settings.",
      "Running version on multiple layers (Redfish, /etc/os-release), boot ID changed, health equal to baseline, settings preserved.")
multi("nvme", "Which belong in an SSD firmware bug report? (Select all that apply)",
      ["Firmware revision (old and new)", "Exact commands and iteration count", "dmesg, SMART and error log before and after", "Your opinion of the firmware team"],
      [0, 1, 2], "Evidence that lets the firmware team reproduce and compare.",
      "Firmware revision (old and new), drive model and serial (masked if needed), host and kernel, nvme-cli version, exact commands, iteration count, dmesg, SMART and error log before and after")
multi("python-in-ci", "A test passes locally but fails in CI. What should you diff? (Select all that apply)",
      ["Locale and TZ", "Versions and PATH", "Permissions, network and parallelism", "The colour of the Jenkins theme"],
      [0, 1, 2], "Reproduce in a container matching CI.", "Diff environment: locale, TZ, versions, PATH, permissions, network, parallelism")
multi("pytest", "What breaks when you run pytest tests in parallel with pytest-xdist? (Select all that apply)",
      ["Shared files and ports", "Session fixtures created once per worker", "Tests that depend on order", "Pure functions with no shared state"],
      [0, 1, 2], "Hardware tests also need device leases.", "shared files and ports, session fixtures that are created once **per worker**")
multi("rca", "How do you prevent a fixed issue from recurring? (Select all that apply)",
      ["Add a regression test or health check", "Add a log pattern to automated triage", "Monitor the leading indicator", "Close the bug and move on"],
      [0, 1, 2], "Prevention means detection next time, automatically.",
      "Add a regression test or health check, a log pattern to automated triage, and monitoring of the leading indicator")
multi("jenkins-pipelines", "What goes in post { always } for hardware stages? (Select all that apply)",
      ["Log collection", "Power-off or restore", "JUnit publishing and artifact archiving", "Deleting the test results"],
      [0, 1, 2], "post { always } runs even when the stage failed.", "Log collection, power-off or restore, JUnit publishing, artifact archiving.")
multi("embedded-ci", "What should a pipeline preserve for later analysis? (Select all that apply)",
      ["Versions and parameters", "Health before/after and JUnit with messages", "Console/journal and a timeline", "Only the final pass/fail colour"],
      [0, 1, 2], "Evidence makes old failures analysable.",
      "Versions and parameters, health before/after, JUnit with messages, per-test evidence")
multi("ipmi", "Which IPMI completion codes does the handbook mention for platform differences? (Select all that apply)",
      ["0xC1 — command may be optional and not implemented", "0xD4 — privilege", "0xCB — missing sensor or FRU", "0x00 — fatal error"],
      [0, 1, 2], "0x00 means success.", "0xC1 may mean the command is optional and not implemented there; 0xD4 means privilege; 0xCB a missing sensor or FRU")
multi("py-oop", "Which does a @dataclass generate for you? (Select all that apply)",
      ["__init__", "__repr__", "__eq__", "A database table"], [0, 1, 2],
      "Plus ordering, immutability options and safe defaults.", "Generated __init__/__repr__/__eq__, ordering, immutability, safe defaults.")
multi("linux-proc", "What does free -h show? (Select all that apply)",
      ["Total and used memory", "buff/cache", "available memory", "CPU temperature"], [0, 1, 2],
      "\"available\" is the important estimate for new workloads.", "Total, used, free, shared, buff/cache and available memory")
multi("test-design", "When there's no time for full regression, what does risk-based prioritisation cover first? (Select all that apply)",
      ["Areas the release notes say changed", "Features that depend on them", "Historically flaky or high-defect areas", "Tests sorted alphabetically"],
      [0, 1, 2], "Then customer-critical paths; and state explicitly what was not run.",
      "first the areas the release notes say changed, then features that depend on them, then historically flaky or high-defect areas")

# ----------------------------------------------------------------- Scenarios
scenario("fw-update", "A BMC update succeeds but the version is unchanged after reboot. What do you check first?",
         ["Re-run the update five times", "Boot ID (did it reboot?), serial console (did the new image fail and fall back?) and the image MANIFEST version",
          "Change the Redfish password", "Clear the SEL"], 1,
         "Distinguish 'never rebooted', 'rolled back' and 'wrong image'.", "Check boot ID (did it reboot?), serial (did new image fail and fall back?)")
scenario("fw-update", "A firmware update's progress is stuck at 40%. What is the right first move?",
         ["Power-cycle the BMC immediately", "Compare with the baseline duration and capture updater state, journal and dmesg before intervening",
          "Start a second update in parallel", "Mark the test as flaky"], 1,
         "Capture evidence before intervening.", "Compare with baseline duration; check updater process state, journal, dmesg flash errors")
scenario("embedded-ci", "Many tests fail at once with connection errors in a nightly run. How do you treat it?",
         ["File one bug per failing test", "As one infra/device event: find when it started, check device logs and health, recover, rerun",
          "Ignore it — nightly runs are noisy", "Disable the failing tests"], 1,
         "One root cause, one investigation.", "Treat as one infra/device event")
scenario("python-in-ci", "A Jenkins job hangs for hours. What fixes the class of problem?",
         ["Bigger agents", "Per-command, stage and job timeouts, then find the hanging step (thread dump, process tree)", "Restart Jenkins nightly", "Run fewer tests"], 1,
         "Often an interactive prompt or unbounded wait.", "Add per-command timeouts, stage timeouts and job timeout")
scenario("python-in-ci", "Nightly failures all occurred on one Jenkins agent. What's your hypothesis?",
         ["A firmware regression", "Infrastructure: compare that agent's environment, network path and resources; rerun on another agent",
          "The tests are wrong", "Random chance"], 1,
         "Failures clustered on one agent point at the agent.", "Infra suspect: compare agent environment, network path, resources")
scenario("rca", "A test fails 1 in 50 runs. How do you prove your fix?",
         ["Run it once after the fix", "Measure the baseline rate, then run about 150 times after the fix (or stress to raise the failure rate)",
          "Ask the developer if it is fixed", "Run it 10 times"], 1,
         "0.98^150 ≈ 5%: 150 clean runs make a lucky pass unlikely.", "then run ~150 times after the fix")
scenario("rca", "Two hypotheses explain the data equally well. What next?",
         ["Pick the more likely one", "Design a discriminating experiment where predictions differ, one variable at a time",
          "Report both as root causes", "Wait for more failures"], 1,
         "Swap hardware, change version or alter timing.", "Design a discriminating experiment where predictions differ")
scenario("uart", "The serial console shows garbage right after a firmware update. Most likely cause?",
         ["The UART chip is broken", "The console baud rate changed or the console moved to a different UART in the new image",
          "Redfish is down", "The SSH key changed"], 1,
         "Try common baud rates and check the new image's boot arguments.", "Most likely the console baud rate changed")
scenario("ipmi", "An IPMI test fails on only one platform. What's the first distinction to make?",
         ["Whether to rerun", "Transport failure versus a completion code — then look up the code", "Which engineer wrote the test", "Whether Redfish works"], 1,
         "Then compare tool versions, cipher suites and user privileges.", "Determine whether it’s a transport failure or a completion code.")
scenario("redfish", "Why not put automatic retries on every Redfish client method?",
         ["Retries are slow", "POST and PATCH can change state; after a timeout you can't know if the server applied them",
          "Redfish forbids retries", "Retries break TLS"], 1,
         "Retrying could create two sessions, two users or two resets.", "POST and PATCH can change state.")
scenario("mctp", "What can't a PLDM/MCTP simulator tell you?",
         ["Whether your codec parses responses", "Whether a specific device conforms and real bus timing/behaviour",
          "Whether your retry logic works", "Whether your state machine handles errors"], 1,
         "Simulators are valuable for requester logic; hardware validates devices and timing.",
         "Whether a specific device conforms, real timing and bus-level behaviour")
scenario("linux-proc", "A BMC daemon gets killed overnight and the kernel log mentions the OOM killer. What happened?",
         ["A user ran kill -9", "Memory could not be reclaimed, so the kernel picked a process by OOM score and sent SIGKILL",
          "systemd stopped it for a timeout", "The watchdog reset the BMC"], 1,
         "Look for the leak that drove memory up.", "it selects a process by OOM score and kills it with SIGKILL")
scenario("test-design", "How do you test a reboot scenario properly?",
         ["Reboot once and check ping", "Define the reboot type, then per iteration: trigger, wait for readiness with a timeout, verify state",
          "Reboot until it fails", "Only test AC power cycles"], 1,
         "BMC reboot, host warm reboot, cold power cycle and AC cycle are different tests.", "Define the reboot type")
scenario("kernel", "A driver probe fails with error -121. What does it typically mean?",
         ["Out of memory", "EREMOTEIO: the I²C device did not acknowledge — absent, unpowered or wrong address", "Permission denied", "File not found"], 1,
         "errno 121 is EREMOTEIO.", "errno 121 (EREMOTEIO): typically the I²C device did not acknowledge")
scenario("boot", "How do you validate a BMC reboot automatically?",
         ["Sleep 300 seconds then run tests", "Record pre-state; trigger; confirm down; wait with timeout for ping, SSH, API; verify new boot ID, versions, health, logs",
          "Check that the web UI loads", "Ping it once"], 1,
         "Fixed sleeps hide slow boots and waste time.", "Record pre-state; trigger; confirm down; wait with timeout for ping, SSH, API")

# ----------------------------------------------------------------- Debugging
debug("py-functional", "These callbacks all print 2. Why?",
      "callbacks = [lambda: print(i) for i in range(3)]\nfor cb in callbacks:\n    cb()",
      ["range() is broken", "Closures capture variables, not values (late binding)", "lambda can't print", "The list is reversed"], 1,
      "Fix with a default argument (lambda i=i: ...) or a factory.", "Closures capture variables, not values; fix with default arguments or factories.")
debug("py-basics", "This check fails although the device printed PASS. Why?",
      'status = serial_line()   # returns "PASS\\r"\nif status == "PASS":\n    ok()',
      ["== is case-sensitive", "A hidden carriage return; inspect with repr() and fix with strip()", "Strings are mutable", "serial_line is async"], 1,
      "repr() shows the \\r that print() hides.", "Hidden carriage return; inspect with repr(), fix with strip().")
debug("py-datastructures", "Deduplicating a list of serial numbers with list(set(items)) broke a report. What's wrong?",
      "serials = list(set(serials))",
      ["set() raises on duplicates", "list(set(items)) removes duplicates but loses order — use list(dict.fromkeys(items))", "Sets can't hold strings", "Nothing"], 1,
      "dict preserves insertion order.", "list(dict.fromkeys(items)). list(set(items)) removes duplicates but loses order.")
debug("py-concurrency", "Two threads increment a shared counter and the total is sometimes too low. What's the bug and fix?",
      "count = 0\ndef work():\n    global count\n    for _ in range(100000):\n        count += 1",
      ["Integers overflow", "A race condition (lost updates); use a lock, a queue, or no shared state", "global is not allowed", "Threads run sequentially"], 1,
      "count += 1 is a read-modify-write.", "Lost updates on shared counters; locks, queues, no shared state.")
debug("bash-basics", "This pipeline reports success even when the test script crashed. Why?",
      "./run_tests.sh | tee results.log\necho \"exit: $?\"",
      ["tee hides errors by design", "Pipeline status is the last command's (tee) unless pipefail is set", "$? is not supported", "The script needs sudo"], 1,
      "Add set -o pipefail.", "Pipeline status is the last command’s unless pipefail is set.")
debug("py-exceptions", "Why does this function silently swallow the exception?",
      "def read():\n    try:\n        return sensor.read()\n    finally:\n        return None",
      ["sensor.read() can't raise", "A return in finally overrides any exception or earlier return", "finally only runs on success", "Exceptions can't propagate from functions"], 1,
      "Linters flag return in finally.", "It overrides any exception or earlier return; it silently swallows exceptions")
debug("requests", "This health check sometimes hangs the whole test run forever. What's missing?",
      'r = requests.get(f"https://{bmc}/redfish/v1/", verify=ca)',
      ["A User-Agent header", "A timeout — requests has no default timeout", "json=True", "A session object"], 1,
      "Always set connect and read timeouts.", "The default is no timeout; a hung server would block the test forever.")
debug("cli-automation", "Why is this dangerous in automation?",
      'subprocess.run(f"ipmitool -H {host} sel list", shell=True)',
      ["It is slower", "shell=True with external input allows command injection and quoting bugs — use an argument list", "ipmitool needs sudo", "It returns bytes"], 1,
      "Pass ['ipmitool', '-H', host, 'sel', 'list'].", "Command injection and quoting bugs; use argument lists.")

# ----------------------------------------------------------------- MCQ
mcq("nvme", "What is MTFA in NVMe Identify Controller?",
    ["Mean Time to Failure", "Maximum Time for Firmware Activation (100 ms units)", "Media Transfer Frame Alignment", "Max Transfer Size"], 1,
    "It is the longest I/O pause allowed during immediate activation.", "Maximum Time for Firmware Activation, in 100 ms units in Identify Controller.")
mcq("nvme", "What is write amplification?",
    ["Host writes divided by NAND writes", "NAND writes divided by host writes", "Read latency growth", "The number of firmware slots"], 1,
    "GC and wear-levelling make it greater than 1.", "NAND writes divided by host writes.")
mcq("systemd", "With Type=dbus, when does systemd consider the service started?",
    ["When the process forks", "Once it owns its configured BusName on D-Bus", "After 90 seconds", "When it writes to the journal"], 1,
    "Common for OpenBMC daemons.", "systemd considers the service started once it owns its configured BusName on D-Bus")
mcq("kernel", "What is pstore/ramoops?",
    ["A package manager", "Preserves kernel crash logs in reserved RAM across reboot", "A Redfish schema", "An I²C tool"], 1,
    "Readable in /sys/fs/pstore if configured.", "A mechanism to preserve kernel crash logs in reserved RAM across reboot")
mcq("openbmc-arch", "How does a Redfish sensor reading reach the client?",
    ["Hardware → bmcweb → kernel → D-Bus", "Hardware → kernel driver (hwmon) → sensor daemon → D-Bus Sensor.Value → bmcweb → JSON",
     "Hardware → IPMI → Redfish", "Hardware → U-Boot → bmcweb"], 1,
    "Each hop is a place to check when values disagree.", "Hardware → kernel driver (hwmon) → sensor daemon → D-Bus Sensor.Value")
mcq("ipmi", "Which IPMI NetFn is App (e.g. Get Device ID)?",
    ["0x00", "0x04", "0x06", "0x0A"], 2,
    "Chassis 0x00, Sensor/Event 0x04, App 0x06, Storage 0x0A, Transport 0x0C.", "NetFn groups commands by function (Chassis 0x00, Sensor/ Event 0x04, App 0x06")
mcq("pldm", "What is a PDR?",
    ["A PLDM Data Request", "A Platform Descriptor Record describing sensors, effecters and entities", "A Redfish property", "A firmware slot"], 1,
    "The BMC reads PDRs with GetPDR, much like IPMI SDRs.", "A Platform Descriptor Record (DSP0248) describing a sensor, effecter, entity")
mcq("redfish", "What does $expand do in Redfish?",
    ["Increases the session timeout", "Inlines referenced resources (e.g. collection members) in one response", "Expands JSON keys to full names", "Enables events"], 1,
    "Support is optional; tests need a non-expanded fallback.", "asks the service to inline referenced resources")
mcq("redfish", "Which DMTF tool checks Redfish responses against the published schemas?",
    ["Redfish Protocol Validator", "Redfish Service Validator", "Redfish Interop Validator", "ipmitool"], 1,
    "The Protocol Validator checks HTTP behaviour; the Interop Validator checks against a profile.",
    "the Redfish Service Validator (checks responses against the published schemas)")
mcq("uart", "Why should automation avoid sending a UART break accidentally?",
    ["It erases flash", "Some systems interpret it specially (e.g. Linux magic SysRq), which could reboot or halt the system", "It changes the baud rate", "It disables SSH"], 1,
    "A break is the line held low longer than a frame.", "Some systems interpret it specially")
mcq("ssh", "Paramiko exec_command versus invoke_shell — which is ideal for automation?",
    ["invoke_shell", "exec_command — one command on a new channel with its exit status", "Both are identical", "Neither"], 1,
    "invoke_shell needs prompt matching and makes exit codes harder to get.", "exec_command runs one command on a new channel and returns its exit status")
mcq("pytest", "skip versus xfail?",
    ["Same thing", "Skip: don't run (not applicable). Xfail: run, failure expected due to a known bug", "Xfail never runs", "Skip runs twice"], 1,
    "Use strict=True with xfail.", "Skip: do not run (not applicable here). Xfail: run, failure expected because of a known bug")
mcq("python-in-ci", "pytest exits with code 2 in CI. What do you check first?",
    ["Disk space", "The first ERROR in collection: import/syntax errors, conftest problems, unregistered markers, missing deps", "The Jenkins theme", "Network latency"], 1,
    "Exit 2 means the run was interrupted, often during collection.", "The first ERROR in collection")
mcq("jenkins-pipelines", "Why keep Groovy thin in Jenkins pipelines?",
    ["Groovy is slow", "Python logic can be tested and run locally; Groovy logic cannot easily", "Jenkins removed Groovy", "For security only"], 1,
    "Put logic in testable scripts; keep the Jenkinsfile as orchestration.", "Python logic can be tested and run locally; Groovy logic cannot easily.")
mcq("py-concurrency", "asyncio.gather versus TaskGroup?",
    ["Identical", "gather: independent results; TaskGroup: structured all-or-cancel behaviour", "TaskGroup is for threads", "gather cancels on first error by design"], 1,
    "TaskGroup cancels siblings when one fails.", "Independent results (optionally exceptions as values) versus structured all-or-cancel behaviour.")
mcq("py-oop", "ABC versus Protocol?",
    ["Both require inheritance", "ABC is nominal (must inherit); Protocol is structural (matching methods)", "Protocol is enforced at runtime always", "ABC can't share code"], 1,
    "Protocols are checked by type checkers.", "Nominal (must inherit, enforced at instantiation, can share code) versus structural")
mcq("py-functional", "With @timed above @retry, what does timed measure?",
    ["Each attempt", "All attempts together", "Nothing", "Only the last attempt"], 1,
    "The outer decorator wraps everything the inner one does.", "Outer timed measures all attempts; inner timed measures each attempt.")
mcq("linux-logs", "Why compare logs from different machines in UTC with offsets recorded?",
    ["UTC is faster", "Clocks and time zones differ; without a common base, event order is wrong", "Journals require UTC", "To save disk space"], 1,
    "Correlate before concluding.", "Clocks and time zones differ; without a common base, event order is wrong.")
mcq("test-design", "Fan PWM accepts 0–100. Which values does boundary value analysis test?",
    ["0 and 100 only", "−1, 0, 1, 99, 100, 101", "50 only", "Random values"], 1,
    "Just outside, at, and just inside each limit.", "Fan PWM 0–100: test −1, 0, 1, 99, 100, 101.")

if __name__ == "__main__":
    print(json.dumps({"quiz": Q}, ensure_ascii=False, indent=1))
