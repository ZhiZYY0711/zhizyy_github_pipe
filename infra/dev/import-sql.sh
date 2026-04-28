#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
ENV_FILE="${1:-$SCRIPT_DIR/.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing env file: $ENV_FILE" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"
MYSQL_DATABASE="${MYSQL_DATABASE:-pipeline_management_system}"
POSTGRES_USER="${POSTGRES_USER:-agent}"
POSTGRES_DB="${POSTGRES_DB:-pipeline_agent}"

COMPOSE=(docker compose --env-file "$ENV_FILE" -f "$SCRIPT_DIR/docker-compose.yml")

import_mysql() {
  local file="$1"
  echo "Import MySQL: ${file#$PROJECT_ROOT/}"

  "${COMPOSE[@]}" exec -T mysql \
    mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < "$file"
}

import_postgres() {
  local file="$1"
  echo "Import PostgreSQL: ${file#$PROJECT_ROOT/}"

  "${COMPOSE[@]}" exec -T postgres \
    psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" < "$file"
}

add_sql_files() {
  local db_type="$1"
  local root="$2"

  [[ -d "$root" ]] || return 0

  while IFS= read -r file; do
    sql_types+=("$db_type")
    sql_files+=("$file")
  done < <(find "$root" -type f -name '*.sql' | sort)
}

select_sql_file() {
  if ((${#sql_files[@]} == 0)); then
    echo "No SQL files found under database/mysql or database/postgres" >&2
    exit 1
  fi

  echo "Available SQL files:"
  for i in "${!sql_files[@]}"; do
    printf '%2d) [%s] %s\n' "$((i + 1))" "${sql_types[$i]}" "${sql_files[$i]#$PROJECT_ROOT/}"
  done
  echo " 0) Cancel"

  local choice
  while true; do
    read -r -p "Select one SQL file to import: " choice
    if [[ "$choice" == "0" ]]; then
      echo "Canceled."
      exit 0
    fi
    if [[ "$choice" =~ ^[0-9]+$ ]] && ((choice >= 1 && choice <= ${#sql_files[@]})); then
      selected_index=$((choice - 1))
      return 0
    fi
    echo "Invalid choice: $choice" >&2
  done
}

sql_types=()
sql_files=()
selected_index=

add_sql_files "MySQL" "$PROJECT_ROOT/database/mysql"
add_sql_files "PostgreSQL" "$PROJECT_ROOT/database/postgres"
select_sql_file

case "${sql_types[$selected_index]}" in
  MySQL)
    import_mysql "${sql_files[$selected_index]}"
    ;;
  PostgreSQL)
    import_postgres "${sql_files[$selected_index]}"
    ;;
  *)
    echo "Unsupported SQL type: ${sql_types[$selected_index]}" >&2
    exit 1
    ;;
esac
