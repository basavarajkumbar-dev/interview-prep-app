"""Consolidated topic map: which handbook chapters feed which app topic.

Each topic lists (book, chapter-number) pairs. Overlapping chapters from
different handbooks are merged into one topic; every lesson keeps its own
source reference so the app can show where it came from.
"""

ROLES = {
    "PY": "Python Automation",
    "TA": "Test Automation",
    "EMB": "Embedded Testing",
    "BMC": "BMC/OpenBMC",
    "SSD": "SSD/NVMe",
    "SR": "Senior Test Engineer",
    "SW": "Software Test Engineer",
}

TRACKS = [
    # id, title, emoji, description, roles
    ("python", "Python Core", "🐍", "Language fundamentals to advanced Python, as used in automation.", ["PY", "TA", "SR", "SW"]),
    ("automation", "Test Automation", "🧪", "pytest, mocking, framework design, logging, reliability.", ["TA", "PY", "SR", "SW"]),
    ("api", "REST & Redfish", "🌐", "HTTP, REST, requests and the Redfish management API.", ["PY", "TA", "BMC"]),
    ("protocols", "Embedded Protocols", "🔌", "IPMI, RMCP+, PLDM, MCTP, UART, SSH and I²C.", ["EMB", "BMC"]),
    ("openbmc", "BMC / OpenBMC", "🖥️", "OpenBMC architecture, D-Bus, firmware update and health.", ["BMC", "EMB"]),
    ("ssd", "SSD / NVMe", "💾", "NVMe architecture, commands, SMART and SSD validation.", ["SSD", "EMB"]),
    ("linux", "Linux & Bash", "🐧", "Linux internals, logs, systemd and Bash automation.", ["EMB", "BMC", "SSD", "PY"]),
    ("cicd", "Git & CI/CD", "🔁", "Git workflows, Jenkins pipelines and embedded CI.", ["TA", "SR", "PY"]),
    ("debugging", "Debugging & RCA", "🔍", "Root-cause analysis, failure classification and case studies.", ["EMB", "BMC", "SSD", "SR"]),
    ("coding", "Coding Interview", "💻", "Data structures, algorithms and 105 practice problems.", ["PY", "TA", "SW"]),
    ("career", "Interview & Career", "🎯", "Your profile, likely questions, HR, mock interviews and plans.", ["PY", "TA", "EMB", "BMC", "SSD", "SR", "SW"]),
]

# topic id -> (track, title, [(book, chapter)], keywords for question classification)
TOPICS = [
    # ---------------- Python
    ("py-basics", "python", "Python Basics & Data Types", [("python", "1"), ("python", "2"), ("python", "3"), ("python", "4")],
     ["mutable", "immutable", "f-string", "string", "int(", "float", "== ", " is ", "bitwise", "operator", "data type", "None"]),
    ("py-control", "python", "Conditions & Loops", [("python", "5"), ("python", "6")],
     ["if/elif", "truthy", "falsy", "for loop", "while", "enumerate", "zip(", "range(", "break", "guard clause"]),
    ("py-datastructures", "python", "Lists, Tuples, Sets & Dicts", [("python", "7")],
     ["list", "tuple", "dict", "set(", "dictionary", "append", "extend", "shallow copy", "deep copy"]),
    ("py-functions", "python", "Functions", [("python", "8")],
     ["*args", "**kwargs", "default argument", "LEGB", "scope", "lambda", "recursion", "closure", "docstring"]),
    ("py-iteration", "python", "Comprehensions, Iterators & Generators", [("python", "9"), ("python", "15")],
     ["generator", "yield", "iterator", "comprehension", "lazy", "context manager", "__enter__", "contextlib", "with statement"]),
    ("py-modules", "python", "Modules, Packages & Environments", [("python", "10"), ("python", "23")],
     ["import", "module", "package", "__main__", "venv", "pip", "pyproject", "virtual environment", "sys.path", "requirements"]),
    ("py-exceptions", "python", "Exception Handling", [("python", "11")],
     ["exception", "try", "except", "finally", "raise", "traceback", "error handling"]),
    ("py-files", "python", "Files & Structured Data", [("python", "12"), ("python", "13")],
     ["file", "pathlib", "open(", "encoding", "json", "csv", "xml", "yaml"]),
    ("py-oop", "python", "Object-Oriented Programming", [("python", "14")],
     ["class", "inheritance", "composition", "self", "dataclass", "abstract", "polymorphism", "property", "SOLID", "__init__", "MRO", "object-oriented", "OOP"]),
    ("py-functional", "python", "Decorators & Functional Programming", [("python", "16")],
     ["decorator", "functools", "wraps", "partial", "map(", "filter(", "reduce", "higher-order"]),
    ("py-regex", "python", "Regular Expressions", [("python", "17")],
     ["regex", "regular expression", "re.", "greedy", "match", "findall", "named group"]),
    ("py-stdlib", "python", "Standard Library", [("python", "18")],
     ["subprocess", "argparse", "collections", "Counter", "datetime", "logging module", "socket", "defaultdict"]),
    ("py-advanced", "python", "Advanced Language Features", [("python", "19")],
     ["metaclass", "descriptor", "pattern matching", "match statement", "walrus", "dunder", "__slots__", "data model"]),
    ("py-performance", "python", "Memory & Performance", [("python", "20")],
     ["reference counting", "garbage", "memory", "profil", "timeit", "cProfile", "tracemalloc", "performance"]),
    ("py-concurrency", "python", "Concurrency: Threads, Processes & asyncio", [("python", "21")],
     ["thread", "GIL", "multiprocessing", "asyncio", "async", "await", "concurrent.futures", "race condition", "lock", "concurrency", "parallel"]),
    ("py-quality", "python", "Type Hints & Code Quality", [("python", "22"), ("python", "37")],
     ["type hint", "mypy", "typing", "Protocol", "TypedDict", "ruff", "lint", "PEP 8", "pre-commit", "code review"]),
    ("py-debugging", "python", "Debugging & Troubleshooting Python", [("python", "36"), ("python", "38")],
     ["pdb", "breakpoint", "debug", "minimal reproduction", "traceback"]),
    ("py-interview", "python", "Python Interview Quick Answers", [("python", "39"), ("bmcssd", "10")],
     []),
    # ---------------- Automation
    ("pytest", "automation", "pytest", [("python", "25"), ("api", "19"), ("cicd", "15")],
     ["pytest", "fixture", "conftest", "parametrize", "marker", "yield fixture", "plugin", "JUnit XML", "junitxml"]),
    ("mocking", "automation", "unittest & Mocking", [("python", "26")],
     ["mock", "patch", "unittest", "MagicMock", "dependency injection", "stub", "fake"]),
    ("test-design", "automation", "Test Strategy & Design", [("api", "3"), ("bmcssd", "6")],
     ["test plan", "boundary value", "equivalence", "smoke", "sanity", "regression", "severity", "priority", "defect lifecycle", "test coverage", "negative test", "traceability", "risk-based"]),
    ("framework", "automation", "Automation Framework Architecture", [("python", "31")],
     ["framework", "layer", "adapter", "page object", "configuration", "design a framework", "architecture"]),
    ("logging-reporting", "automation", "Logging, Reporting & Observability", [("python", "30"), ("cicd", "22")],
     ["logging", "logger", "report", "allure", "html report", "correlation id", "structured log", "notification"]),
    ("browser", "automation", "Browser Automation (Playwright)", [("python", "29")],
     ["playwright", "selenium", "locator", "browser", "page object", "ui test", "web ui"]),
    ("reliability", "automation", "Security & Reliability in Automation", [("python", "34"), ("api", "21")],
     ["secret", "injection", "idempotent", "fault injection", "destructive", "security", "credential", "flaky"]),
    ("cli-automation", "automation", "Linux Command-Line Automation with Python", [("python", "28"), ("linux", "23")],
     ["subprocess", "exit code", "shell=True", "paramiko", "command line", "bash and python"]),
    ("automation-interview", "automation", "Automation Engineer Interview Questions", [("python", "40")], []),
    # ---------------- API
    ("api-basics", "api", "APIs, Protocols & Networking", [("api", "1"), ("api", "2")],
     ["TCP", "UDP", "port", "DNS", "TLS", "network", "protocol", "OSI", "socket"]),
    ("http-rest", "api", "HTTP & REST Fundamentals", [("api", "4")],
     ["HTTP", "REST", "status code", "PUT", "PATCH", "GET", "POST", "idempot", "ETag", "401", "403", "header"]),
    ("requests", "api", "Python requests & API Testing", [("api", "5"), ("python", "27")],
     ["requests", "session", "timeout", "retry", "schema", "jsonschema", "response"]),
    ("advanced-rest", "api", "Advanced REST Test Automation", [("api", "6")],
     ["contract", "pagination", "rate limit", "concurrency", "negative", "api client"]),
    ("redfish", "api", "Redfish", [("api", "7"), ("api", "8"), ("api", "22")],
     ["redfish", "odata", "/redfish/v1", "bmcweb", "x-auth-token", "service root", "taskmonitor", "updateservice", "ComputerSystem", "@Message"]),
    # ---------------- Protocols
    ("protocol-briefing", "protocols", "Protocols: Interview Briefing", [("bmcssd", "8")], []),
    ("ipmi", "protocols", "IPMI", [("api", "9"), ("api", "10")],
     ["ipmi", "ipmitool", "netfn", "completion code", "SEL", "SDR", "FRU", "KCS", "chassis"]),
    ("rmcp", "protocols", "IPMI LAN, RMCP+ & Security", [("api", "11")],
     ["RMCP", "lanplus", "cipher suite", "RAKP", "UDP 623", "SOL"]),
    ("pldm", "protocols", "PLDM", [("api", "12"), ("api", "13")],
     ["PLDM", "PDR", "pldmtool", "firmware update over PLDM", "type 5", "type 2"]),
    ("mctp", "protocols", "MCTP", [("api", "14"), ("api", "15")],
     ["MCTP", "EID", "endpoint", "mctpd", "AF_MCTP", "SMBus", "PCIe VDM"]),
    ("uart", "protocols", "UART & Serial Console", [("api", "16"), ("linux", "21"), ("cicd", "25")],
     ["UART", "serial", "baud", "pyserial", "console", "SOL", "/dev/tty"]),
    ("ssh", "protocols", "SSH Automation", [("api", "17")],
     ["SSH", "paramiko", "scp", "known_hosts", "host key"]),
    ("i2c", "protocols", "I²C", [("api", "18")],
     ["I2C", "I²C", "i2cget", "i2cdetect", "SMBus", "ACK", "SDA", "SCL", "EEPROM"]),
    ("hw-automation", "protocols", "Hardware & Embedded Test Automation", [("python", "33")],
     ["hardware-in-the-loop", "HIL", "power cycle", "power-cycle", "DUT", "bench"]),
    ("embedded-interview", "protocols", "Embedded Automation Interview Questions", [("python", "41")], []),
    # ---------------- OpenBMC
    ("openbmc-arch", "openbmc", "OpenBMC Architecture", [("linux", "17"), ("bmcssd", "7")],
     ["OpenBMC", "phosphor", "obmcutil", "BMC", "Yocto", "U-Boot"]),
    ("dbus", "openbmc", "D-Bus & Service Interaction", [("linux", "18")],
     ["D-Bus", "busctl", "ObjectMapper", "mapper", "dbus"]),
    ("fw-update", "openbmc", "Firmware Update", [("linux", "19"), ("cicd", "24")],
     ["firmware update", "image", "activation", "rollback", "A/B", "flash", "downgrade"]),
    ("bmc-health", "openbmc", "BMC Health & Platform Validation", [("linux", "20")],
     ["health", "sensor", "hwmon", "watchdog", "event log", "platform validation"]),
    ("bmc-projects", "openbmc", "Projects: OpenBMC Diagnostics & Firmware Validation", [("api", "23"), ("api", "24")], []),
    # ---------------- SSD
    ("nvme", "ssd", "NVMe & SSD Validation", [("bmcssd", "9")],
     ["NVMe", "SSD", "nvme-cli", "SMART", "fio", "namespace", "fw-commit", "PCIe", "submission queue", "doorbell"]),
    # ---------------- Linux
    ("linux-intro", "linux", "Linux & Embedded Linux", [("linux", "1")], ["kernel", "distribution", "PID 1", "embedded Linux"]),
    ("linux-fs", "linux", "Filesystem, Users & Permissions", [("linux", "2"), ("linux", "3")],
     ["permission", "chmod", "chown", "/proc", "/sys", "inode", "symlink", "sudo", "umask"]),
    ("linux-proc", "linux", "Processes & Resources", [("linux", "4")],
     ["process", "signal", "kill", "zombie", "OOM", "load average", "file descriptor", "top", "ps "]),
    ("linux-net", "linux", "Linux Networking", [("linux", "5")], ["ping", "ss -", "ip addr", "route", "firewall", "curl", "nc "]),
    ("linux-storage", "linux", "Storage & Mounts", [("linux", "6")], ["mount", "df", "du ", "disk full", "filesystem", "read-only", "lsblk"]),
    ("linux-logs", "linux", "Logs & journald", [("linux", "7")], ["journalctl", "journal", "syslog", "log rotation", "rotated"]),
    ("systemd", "linux", "systemd & Services", [("linux", "8")], ["systemd", "systemctl", "unit", "service", "After=", "Restart="]),
    ("kernel", "linux", "Kernel & Hardware Diagnostics", [("linux", "9")], ["dmesg", "kernel", "driver", "lspci", "probe", "module"]),
    ("boot", "linux", "Boot, Reboot & Recovery", [("linux", "10")], ["boot", "reboot", "U-Boot", "pstore", "recovery", "panic"]),
    ("bash-basics", "linux", "Bash Fundamentals", [("linux", "11"), ("linux", "12")], ["bash", "shell", "variable", "quoting", "$?", "if [", "[[ "]),
    ("bash-structure", "linux", "Bash Functions, Arrays & Structure", [("linux", "13")], ["function", "array", "getopts", "local "]),
    ("text-processing", "linux", "Text Processing & Log Parsing", [("linux", "14")], ["grep", "awk", "sed", "cut", "sort", "uniq", "log parsing"]),
    ("bash-reliable", "linux", "Reliable Bash & Error Handling", [("linux", "15")], ["set -e", "pipefail", "trap", "set -u", "error handling"]),
    ("scheduling", "linux", "Scheduling & Long-Running Diagnostics", [("linux", "16")], ["cron", "timer", "nohup", "tmux", "long-running"]),
    ("diag-framework", "linux", "Automated Linux Diagnostic Framework", [("linux", "24"), ("linux", "25")], ["diagnostic", "framework", "Jenkins"]),
    # ---------------- CI/CD
    ("git-basics", "cicd", "Git Fundamentals", [("cicd", "1"), ("python", "24")], ["git", "commit", "staging", "HEAD", ".gitignore"]),
    ("git-branching", "cicd", "Branching, Merging & Rebase", [("cicd", "2")], ["branch", "merge", "rebase", "conflict", "fast-forward"]),
    ("git-workflows", "cicd", "Git Workflows & GitHub", [("cicd", "3"), ("cicd", "4")], ["pull request", "cherry-pick", "revert", "reset", "tag", "code review", "GitHub"]),
    ("git-troubleshooting", "cicd", "Git Troubleshooting & Security", [("cicd", "5")], ["reflog", "bisect", "detached HEAD", "secret", "force push"]),
    ("cicd-concepts", "cicd", "CI/CD Concepts & Architecture", [("cicd", "6"), ("cicd", "7"), ("cicd", "8")], ["continuous integration", "continuous delivery", "CI/CD", "pipeline", "artifact", "build"]),
    ("jenkins-basics", "cicd", "Jenkins Architecture & Jobs", [("cicd", "9"), ("cicd", "10")], ["jenkins", "controller", "freestyle", "plugin", "job"]),
    ("jenkins-pipelines", "cicd", "Jenkins Pipelines & Jenkinsfile", [("cicd", "11"), ("cicd", "12"), ("python", "32")], ["Jenkinsfile", "declarative", "stage", "post {", "pipeline", "parameters"]),
    ("jenkins-scale", "cicd", "Agents, Parallelism & Containers", [("cicd", "13"), ("cicd", "20"), ("cicd", "21")], ["agent", "executor", "label", "parallel", "docker", "container", "lockable"]),
    ("python-in-ci", "cicd", "Python Tests in CI & Regression Strategy", [("cicd", "14"), ("cicd", "16"), ("cicd", "17")], ["exit code", "test selection", "regression", "retry", "flaky", "quarantine"]),
    ("jenkins-security", "cicd", "Git-to-Jenkins Integration & Credentials", [("cicd", "18"), ("cicd", "19")], ["webhook", "credentials", "withCredentials", "multibranch", "secret"]),
    ("embedded-ci", "cicd", "Embedded CI/CD & OpenBMC Pipelines", [("cicd", "23"), ("cicd", "26"), ("bmcssd", "11")], ["OpenBMC pipeline", "bench", "firmware pipeline", "nightly"]),
    ("cicd-projects", "cicd", "CI/CD Projects", [("cicd", "27"), ("cicd", "28"), ("cicd", "29"), ("cicd", "30")], []),
    # ---------------- Debugging
    ("rca", "debugging", "Root-Cause Analysis & Failure Classification", [("linux", "26")], ["root cause", "RCA", "hypothesis", "classification", "triage", "discriminating"]),
    ("case-studies", "debugging", "Real-World Debugging Case Studies", [("linux", "27")], []),
    ("hw-fw-debug", "debugging", "Embedded Hardware & Firmware Failures", [("linux", "22")], ["hardware", "intermittent", "firmware failure"]),
    ("protocol-failures", "debugging", "Protocol Failure Analysis", [("api", "20")], ["failure analysis", "timeout", "packet capture", "tcpdump"]),
    ("linux-projects", "debugging", "Linux Diagnostic Projects", [("linux", "28")], []),
    # ---------------- Coding
    ("dsa", "coding", "Data Structures & Algorithms", [("python", "35")], ["Big-O", "complexity", "two pointers", "sliding window", "heap", "graph", "binary search", "algorithm"]),
    ("coding-sets", "coding", "Coding Interview Practice", [("python", "42")], []),
    ("py-projects", "coding", "Python Portfolio Projects", [("python", f"P{i}") for i in range(1, 13)], []),
    # ---------------- Career
    ("my-profile", "career", "My Profile & Resume Defense", [("bmcssd", "1"), ("bmcssd", "2"), ("bmcssd", "3"), ("bmcssd", "17")], []),
    ("likely-questions", "career", "Most Likely Interview Questions", [("bmcssd", "4"), ("bmcssd", "5")], []),
    ("project-deep-dive", "career", "Project Deep Dive", [("bmcssd", "13")], []),
    ("self-intro", "career", "Self Introduction", [("bmcssd", "14")], []),
    ("hr", "career", "HR & Behavioural", [("bmcssd", "15"), ("bmcssd", "18")], ["tell me about a time", "weakness", "strength", "conflict", "salary", "why should we hire"]),
    ("cross-questioning", "career", "Cross-Questioning", [("bmcssd", "16")], []),
    ("mock-interviews", "career", "Mock Interviews", [("python", "43"), ("bmcssd", "22"), ("bmcssd", "23")], []),
    ("final-revision", "career", "Final Revision & Assessments", [("api", "27"), ("linux", "31"), ("cicd", "34"), ("bmcssd", "19")], []),
    ("prep-plans", "career", "Preparation Plans & Checklists", [("bmcssd", "20"), ("bmcssd", "21"), ("bmcssd", "24")], []),
    ("question-banks", "career", "Technical Question Banks", [("api", "25"), ("linux", "29"), ("cicd", "31"), ("cicd", "32")], []),
    ("scenario-banks", "career", "Troubleshooting Scenario Banks", [("api", "26"), ("linux", "30"), ("cicd", "33"), ("bmcssd", "12")], []),
]

# Chapters that are pure question banks: questions get re-classified by keywords
BANK_CHAPTERS = {("api", "25"), ("linux", "29"), ("cicd", "31"), ("cicd", "32"),
                 ("python", "39"), ("python", "40"), ("python", "41"), ("bmcssd", "4"),
                 ("bmcssd", "10"), ("bmcssd", "16"), ("bmcssd", "19")}

# Linux ch29 tag -> topic
LINUX_TAGS = {
    "LX": "linux-intro", "FS": "linux-fs", "PR": "linux-proc", "NET": "linux-net", "ST": "linux-storage",
    "LOG": "linux-logs", "SD": "systemd", "KR": "kernel", "KH": "kernel", "BT": "boot", "BS": "bash-basics",
    "SH": "bash-basics", "TP": "text-processing", "TX": "text-processing", "OB": "openbmc-arch", "DB": "dbus",
    "FW": "fw-update", "UART": "uart", "UA": "uart", "PY": "cli-automation", "CI": "python-in-ci",
    "RCA": "rca", "HW": "hw-fw-debug", "SEC": "linux-fs", "PERM": "linux-fs", "SC": "scheduling",
}

# API ch25 group letter -> default topic
API_GROUPS = {"25.1": "redfish", "25.2": "pytest", "25.3": "ipmi", "25.4": "mctp", "25.5": "uart",
              "25.6": "hw-fw-debug", "25.7": "cicd-concepts", "25.8": "protocol-failures"}

# Resume tags per topic, from the BMC/SSD prep handbook §2.7 "Core skills and technical skills"
# (PDF pages 4–5): ON_RESUME = must defend, IMPLIED = likely asked, revise,
# NOT_ON_RESUME = learn basics, answer honestly.
RESUME_TAG_SOURCE = {"book": "bmcssd", "chapter": "2 Resume Analysis", "section": "2.7 Core skills and technical skills", "page": 4}
RESUME_TAGS = {
    # Python Scripting & Test Automation
    **{t: "ON_RESUME" for t in ["py-basics", "py-control", "py-datastructures", "py-functions", "py-iteration", "py-modules",
                                "py-exceptions", "py-files", "py-oop", "py-functional", "py-regex", "py-stdlib",
                                "py-interview", "framework", "cli-automation", "logging-reporting"]},
    # Linux Environment & Debugging; Log Analysis & Failure Debugging
    **{t: "ON_RESUME" for t in ["linux-intro", "linux-fs", "linux-proc", "linux-net", "linux-storage", "linux-logs", "systemd",
                                "kernel", "boot", "bash-basics", "text-processing", "rca", "case-studies", "hw-fw-debug"]},
    # Test Case Design; Regression & Functional Testing; Firmware Validation & Execution
    "test-design": "ON_RESUME", "fw-update": "ON_RESUME",
    # CI/CD (Jenkins, Git/GitHub)
    **{t: "ON_RESUME" for t in ["git-basics", "git-branching", "git-workflows", "jenkins-basics", "jenkins-pipelines",
                                "python-in-ci", "embedded-ci", "cicd-concepts"]},
    # Storage (SSD) domain; Server Management (OpenBMC)
    "nvme": "ON_RESUME", "openbmc-arch": "ON_RESUME", "bmc-health": "ON_RESUME",
    # Implied by role
    "redfish": "IMPLIED", "http-rest": "IMPLIED", "requests": "IMPLIED", "advanced-rest": "IMPLIED",
    "ipmi": "IMPLIED", "pytest": "IMPLIED", "mocking": "IMPLIED", "uart": "IMPLIED", "dbus": "IMPLIED",
    # Not on resume
    "mctp": "NOT_ON_RESUME", "pldm": "NOT_ON_RESUME", "i2c": "NOT_ON_RESUME", "rmcp": "NOT_ON_RESUME",
}
