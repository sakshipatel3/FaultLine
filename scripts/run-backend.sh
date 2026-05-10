#!/usr/bin/env bash
# Run the Spring Boot JAR with JDK 17+.
# On macOS, Homebrew's `java` may point to 11 while Maven built with a newer JDK.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
JAR="target/repo-risk-analyzer-backend.jar"

if [[ ! -f "$JAR" ]]; then
  echo "Missing $JAR — build first: mvn -B -DskipTests package" >&2
  exit 1
fi

if [[ "$(uname -s)" == "Darwin" ]] && [[ -x /usr/libexec/java_home ]]; then
  for v in 25 24 23 22 21 20 19 18 17; do
    if jh="$(/usr/libexec/java_home -v "$v" 2>/dev/null)"; then
      export JAVA_HOME="$jh"
      break
    fi
  done
fi

if [[ -z "${JAVA_HOME:-}" ]] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -qE 'version "(17|1[8-9]|[2-9][0-9])'; then
  echo "JDK 17+ required to run this JAR. Set JAVA_HOME to a JDK 17+ install, or on macOS install one so java_home can find it." >&2
  exit 1
fi

exec "$JAVA_HOME/bin/java" -jar "$JAR" "$@"
