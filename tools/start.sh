#!/usr/bin/env bash
set -euo pipefail
command -v python3 >/dev/null || { echo "Python 3 is required"; exit 1; }
python3 "$(dirname "$0")/manage.py" start
