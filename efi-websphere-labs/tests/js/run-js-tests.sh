#!/usr/bin/env bash
# Exécute les tests unitaires JavaScript avec Nashorn (jjs, fourni par le JDK 8).
# Aucun Node.js requis. Usage : tests/js/run-js-tests.sh
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"

JJS=""
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/jjs" ]; then
  JJS="$JAVA_HOME/bin/jjs"
elif command -v jjs >/dev/null 2>&1; then
  JJS="$(command -v jjs)"
fi
if [ -z "$JJS" ]; then
  echo "NOT TESTED : jjs (Nashorn, JDK 8 à 14) introuvable — tests JavaScript ignorés."
  exit 2
fi

status=0
for t in "$HERE"/test-*.js; do
  echo "▶ $(basename "$t")"
  out="$("$JJS" -J-Dfile.encoding=UTF-8 -J-Droot="$ROOT" "$HERE/lib/assert.js" "$t" 2>&1)"
  rc=$?
  printf '%s\n' "$out" | grep -v -i "deprecat"
  if [ $rc -ne 0 ]; then status=1; fi
done
exit $status
