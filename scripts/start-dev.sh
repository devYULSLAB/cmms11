#!/usr/bin/env bash

set -euo pipefail

# start-dev.sh
# - Runs a Spring Boot fat JAR on Ubuntu in "dev" mode
# - Uses /opt/cmms11/ as the working directory with consistent structure
# - JAR files, scripts, logs, storage, and yml files are all under /opt/cmms11/

# Set consistent working directory
REPO_ROOT="/opt/cmms11"

# Defaults (override via env or flags)
SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-dev}
JAVA_OPTS=${JAVA_OPTS:--Xms256m -Xmx512m}
LOG_DIR=${LOG_DIR:-"${REPO_ROOT}/logs"}
BACKGROUND=-1  # -1 means ask user
JAR_PATH_ENV=${JAR_PATH:-}
DEBUG_PORT=${DEBUG_PORT:-}

usage() {
  cat <<EOF
Usage: $(basename "$0") [options] [JAR_PATH]

Options:
  -p, --profile <name>     Spring profile to activate (default: dev)
  -b, --background         Run in background with nohup
  -j, --java-opts <opts>   Extra JAVA_OPTS (quoted)
  -l, --log-dir <path>     Log directory (default: ${LOG_DIR})
  -d, --debug <port>       Enable remote debug on given port
  -h, --help               Show this help

Arguments:
  JAR_PATH                 Explicit path to JAR (default: newest in /opt/cmms11/*.jar or env JAR_PATH)

Environment variables:
  SPRING_PROFILES_ACTIVE   Profile name (same as --profile)
  JAVA_OPTS                Java opts (same as --java-opts)
  LOG_DIR                  Log directory path
  JAR_PATH                 Path to JAR (fallback if no arg given)
  DEBUG_PORT               Remote debug port (same as --debug)

Behavior:
  - Uses /opt/cmms11/ as working directory with consistent structure.
  - JAR files, scripts, logs, storage, and yml files are all under /opt/cmms11/.
  - If --background, logs are redirected to "+${LOG_DIR}/app.out".
EOF
}

# Parse args
ARGS=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    -p|--profile)
      SPRING_PROFILES_ACTIVE="$2"; shift 2;;
    -b|--background)
      BACKGROUND=1; shift;;
    -j|--java-opts)
      JAVA_OPTS="$2"; shift 2;;
    -l|--log-dir)
      LOG_DIR="$2"; shift 2;;
    -d|--debug)
      DEBUG_PORT="$2"; shift 2;;
    -h|--help)
      usage; exit 0;;
    --)
      shift; while [[ $# -gt 0 ]]; do ARGS+=("$1"); shift; done; break;;
    -*)
      echo "[ERROR] Unknown option: $1" >&2; usage; exit 2;;
    *)
      ARGS+=("$1"); shift;;
  esac
done

# Determine JAR path
JAR_PATH=""
if [[ ${#ARGS[@]} -ge 1 ]]; then
  JAR_PATH="${ARGS[0]}"
elif [[ -n "$JAR_PATH_ENV" ]]; then
  JAR_PATH="$JAR_PATH_ENV"
else
  # Pick the newest jar under /opt/cmms11
  JAR_CANDIDATE=$(ls -1t /opt/cmms11/*.jar 2>/dev/null | head -n1 || true)
  if [[ -n "$JAR_CANDIDATE" ]]; then
    JAR_PATH="$JAR_CANDIDATE"
  fi
fi

if [[ -z "$JAR_PATH" ]]; then
  echo "[ERROR] No JAR found. Provide JAR_PATH or place jar under /opt/cmms11" >&2
  exit 1
fi

if [[ ! -f "$JAR_PATH" ]]; then
  echo "[ERROR] JAR not found: $JAR_PATH" >&2
  exit 1
fi

# Create necessary directories
mkdir -p "$LOG_DIR"
mkdir -p "$REPO_ROOT/storage/uploads"

# Remote debug if requested
if [[ -n "$DEBUG_PORT" ]]; then
  JAVA_OPTS+=" -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:${DEBUG_PORT}"
fi

echo "[INFO] Working dir:    $REPO_ROOT"
echo "[INFO] Using JAR:      $JAR_PATH"
echo "[INFO] Profile:        $SPRING_PROFILES_ACTIVE"
echo "[INFO] Java opts:      $JAVA_OPTS"
echo "[INFO] Log dir:        $LOG_DIR"
if [[ -n "$DEBUG_PORT" ]]; then echo "[INFO] Debug port:     $DEBUG_PORT"; fi

# Ask user for foreground/background if not specified
if [[ $BACKGROUND -eq -1 ]]; then
  echo ""
  echo "Select execution mode:"
  echo "  1) Foreground (logs to console, Ctrl+C to stop)"
  echo "  2) Background (nohup, logs to $LOG_DIR/app.out)"
  read -p "Enter choice [1-2]: " choice
  case $choice in
    1)
      BACKGROUND=0
      echo "[INFO] Mode:           foreground"
      ;;
    2)
      BACKGROUND=1
      echo "[INFO] Mode:           background"
      ;;
    *)
      echo "[ERROR] Invalid choice. Using foreground mode."
      BACKGROUND=0
      ;;
  esac
else
  if [[ $BACKGROUND -eq 1 ]]; then echo "[INFO] Mode:           background"; else echo "[INFO] Mode:           foreground"; fi
fi
echo ""

# Change to working directory
cd "$REPO_ROOT"

# Use JAR internal configuration only
echo "[INFO] Using JAR internal configuration for profile: ${SPRING_PROFILES_ACTIVE}"
CMD=(java ${JAVA_OPTS} -jar "$JAR_PATH" \
  --spring.profiles.active="${SPRING_PROFILES_ACTIVE}")

if [[ $BACKGROUND -eq 1 ]]; then
  # Background with nohup, redirect combined output to log file
  OUT_FILE="$LOG_DIR/app.out"
  echo "[INFO] Writing output to $OUT_FILE"
  nohup "${CMD[@]}" >>"$OUT_FILE" 2>&1 &
  echo $! > "$LOG_DIR/app.pid"
  echo "[INFO] Started PID $(cat "$LOG_DIR/app.pid")"
else
  # Foreground execution
  exec "${CMD[@]}"
fi

