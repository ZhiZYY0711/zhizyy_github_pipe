#!/usr/bin/env bash
set -euo pipefail

TAIL_LINES="${TAIL_LINES:-200}"

usage() {
  cat <<'EOF'
Usage:
  scripts/dev-logs.sh                  # interactive switcher
  scripts/dev-logs.sh frontend         # follow frontend client logs
  scripts/dev-logs.sh server-web       # follow Spring backend logs
  scripts/dev-logs.sh agent            # follow agent logs
  scripts/dev-logs.sh all              # follow all three logs
  scripts/dev-logs.sh --list           # print service log paths
  scripts/dev-logs.sh --path SERVICE   # print one service log path

Services:
  frontend, front
  server-web, web, backend, server
  agent
  all

Environment:
  TAIL_LINES=200                       # initial lines shown by tail
  PIPELINE_DEV_LOG_DIR=/path/to/.logs  # override log directory
EOF
}

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if worktree_root="$(git -C "$script_dir" rev-parse --show-toplevel 2>/dev/null)"; then
  :
else
  worktree_root="$(cd "$script_dir/.." && pwd)"
fi
log_dir="${PIPELINE_DEV_LOG_DIR:-$worktree_root/.logs}"

frontend_log="$log_dir/frontend-client.log"
server_web_log="$log_dir/server-web.log"
agent_log="$log_dir/server-agent.log"

normalize_service() {
  case "${1:-}" in
    frontend|front)
      printf '%s\n' "frontend"
      ;;
    server-web|web|backend|server)
      printf '%s\n' "server-web"
      ;;
    agent)
      printf '%s\n' "agent"
      ;;
    all)
      printf '%s\n' "all"
      ;;
    *)
      return 1
      ;;
  esac
}

service_paths() {
  case "$(normalize_service "$1")" in
    frontend)
      printf '%s\n' "$frontend_log"
      ;;
    server-web)
      printf '%s\n' "$server_web_log"
      ;;
    agent)
      printf '%s\n' "$agent_log"
      ;;
    all)
      printf '%s\n' "$frontend_log" "$server_web_log" "$agent_log"
      ;;
  esac
}

list_paths() {
  printf 'frontend    %s\n' "$frontend_log"
  printf 'server-web  %s\n' "$server_web_log"
  printf 'agent       %s\n' "$agent_log"
}

ensure_files() {
  mkdir -p "$log_dir"
  local file
  for file in "$@"; do
    touch "$file"
  done
}

follow_service() {
  local service="$1"
  mapfile -t files < <(service_paths "$service")
  ensure_files "${files[@]}"

  printf '\nShowing %s logs from %s\n' "$service" "$log_dir"
  printf 'Press Ctrl-C to return to the menu.\n\n'
  tail -n "$TAIL_LINES" -F "${files[@]}"
}

interactive_menu() {
  trap 'printf "\n"' INT

  while true; do
    cat <<EOF

Dev logs: $log_dir
  1) frontend
  2) server-web
  3) agent
  4) all
  q) quit
EOF
    read -r -p "Select log stream: " choice

    case "$choice" in
      1|frontend|front)
        follow_service frontend || true
        ;;
      2|server-web|web|backend|server)
        follow_service server-web || true
        ;;
      3|agent)
        follow_service agent || true
        ;;
      4|all)
        follow_service all || true
        ;;
      q|quit|exit)
        exit 0
        ;;
      *)
        printf 'Unknown choice: %s\n' "$choice" >&2
        ;;
    esac
  done
}

case "${1:-}" in
  -h|--help)
    usage
    ;;
  --list)
    list_paths
    ;;
  --path)
    if [[ $# -lt 2 ]] || ! normalize_service "$2" >/dev/null; then
      printf 'Usage: %s --path SERVICE\n' "$0" >&2
      exit 2
    fi
    service_paths "$2"
    ;;
  "")
    interactive_menu
    ;;
  *)
    if ! normalize_service "$1" >/dev/null; then
      usage >&2
      exit 2
    fi
    follow_service "$1"
    ;;
esac
