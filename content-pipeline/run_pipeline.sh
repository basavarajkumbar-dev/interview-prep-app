#!/usr/bin/env bash
# Rebuilds app/src/main/assets/content/content.json from the handbook PDFs.
#
#   ./run_pipeline.sh /path/to/pdfs
#
# The PDF folder must contain: python.pdf api.pdf linux.pdf cicd.pdf bmcssd.pdf
# (Python Automation Engineer, API & Protocol Automation, Linux/Bash/Embedded Debugging,
#  Git/Jenkins/CI-CD, and the BMC/SSD interview-prep handbook).
# Needs Python 3.12+ with: pip install pymupdf pdfplumber
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
RAW="$(cd "$1" && pwd)"
WORK="$HERE/out"
mkdir -p "$WORK"
cd "$HERE"
python3 extract.py "$RAW" "$WORK/blocks"          # 1. PDF -> line stream (fonts: code, bold, headings)
python3 structure.py "$WORK/blocks" "$WORK/tree"   # 2. chapters -> sections -> elements
python3 curated/quiz_core.py > curated/quiz_core.json   # 3a. hand-written quiz items (evidence-checked in 3b)
python3 curated/quiz_more.py > curated/quiz_more.json
python3 build_content.py "$WORK/tree" curated "$WORK/content.json" "$RAW"   # 3b. knowledge model
python3 verify.py "$WORK/content.json"             # 4. execute code outputs + coding solutions
cp "$WORK/content.json" "$HERE/../app/src/main/assets/content/content.json"
echo "Content written to app/src/main/assets/content/content.json"
echo "Remember to bump \"version\" in build_content.py so installed apps re-import it."
