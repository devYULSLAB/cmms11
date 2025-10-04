#!/usr/bin/env bash

set -euo pipefail

# stop-dev.sh
# - Stops a Spring Boot process started by scripts/start-dev.sh
# - Uses /opt/cmms11/ as the working directory with consistent structure
# - Defaults to reading PID from /opt/cmms11/logs/app.pid

# Set consistent working directory
REPO_ROOT="/opt/cmms11"

# Defaults
LOG_DIR=${LOG_DIR:-"${REPO_ROOT}/logs"}
PID_FILE=${PID_FILE:-"${LOG_DIR}/app.pid"}
TIMEOUT=${TIMEOUT:-20}
SIGNAL=${SIGNAL:-TERM}
ESCALATE_KILL=${ESCALATE_KILL:-1} # 1 to escalate to SIGKILL after timeout
PID=""

usage() {
  cat <<EOF
Usage: $(basename "$0") [options]

Options:
  -l, --log-dir <path>     Log dir containing app.pid (default: ${LOG_DIR})
  -f, --pid-file <path>    Path to PID file (default: ${PID_FILE})
  -p, --pid <pid>          PID to stop (bypass PID file)
  -s, --signal <sig>       Initial signal (default: TERM)
  -t, --timeout <secs>     Seconds to wait before escalating (default: ${TIMEOUT})
  -K, --no-kill            Do not escalate to SIGKILL after timeout
  -h, --help               Show this help

Environment variables:
  LOG_DIR, PID_FILE, TIMEOUT, SIGNAL, ESCALATE_KILL
EOF
}

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    -l|--log-dir)
      LOG_DIR="$2"; shift 2;;
    -f|--pid-file)
      PID_FILE="$2"; shift 2;;
    -p|--pid)
      PID="$2"; shift 2;;
    -s|--signal)
      SIGNAL="$2"; shift 2;;
    -t|--timeout)
      TIMEOUT="$2"; shift 2;;
    -K|--no-kill)
      ESCALATE_KILL=0; shift;;
    -h|--help)
      usage; exit 0;;
    --)
      shift; break;;
    -*)
      echo "[ERROR] Unknown option: $1" >&2; usage; exit 2;;
    *)
      echo "[ERROR] Unexpected argument: $1" >&2; usage; exit 2;;
  esac
done

# Determine PID
if [[ -z "$PID" ]]; then
  # Use consistent PID file location
  if [[ ! -f "$PID_FILE" ]]; then
    echo "[INFO] No PID file found at $PID_FILE — nothing to stop."
    exit 0
  fi
  PID=$(cat "$PID_FILE" | tr -d '\n' || true)
fi

if [[ -z "$PID" ]]; then
  echo "[ERROR] PID is empty." >&2
  exit 1
fi

if ! [[ "$PID" =~ ^[0-9]+$ ]]; then
  echo "[ERROR] PID is not numeric: $PID" >&2
  exit 1
fi

# Check if process exists
if ! ps -p "$PID" >/dev/null 2>&1; then
  echo "[INFO] Process $PID is not running. Removing stale PID file if exists."
  [[ -f "$PID_FILE" ]] && rm -f "$PID_FILE"
  exit 0
fi

echo "[INFO] Stopping PID $PID with SIG${SIGNAL} (timeout: ${TIMEOUT}s)"
kill -s "$SIGNAL" "$PID" || true

# Wait until the process exits or timeout
elapsed=0
while ps -p "$PID" >/dev/null 2>&1; do
  if (( elapsed >= TIMEOUT )); then
    if (( ESCALATE_KILL == 1 )); then
      echo "[WARN] Timeout reached. Escalating to SIGKILL for PID $PID"
      kill -s KILL "$PID" || true
      break
    else
      echo "[WARN] Timeout reached and escalation disabled. Process may still be running (PID $PID)."
      break
    fi
  fi
  sleep 1
  ((elapsed++))
done

# Final check
if ps -p "$PID" >/dev/null 2>&1; then
  echo "[WARN] Process $PID may still be alive. Check manually."
  exit 1
fi

# Cleanup PID file
if [[ -f "$PID_FILE" ]]; then
  rm -f "$PID_FILE"
fi

echo "[INFO] Stopped process $PID"

