#!/usr/bin/env bash
# =============================================================================
#  EFI Academy — WebSphere Application Lab
#  build-all.sh : compile, teste, empaquette et vérifie les 10 applications.
#
#  Usage :
#    ./scripts/build-all.sh [options]
#
#  Options :
#    --offline        Maven hors ligne (-o) : dépôt local déjà alimenté
#    --skip-tests     Ne pas exécuter les tests unitaires (déconseillé)
#    --only LISTE     Ne construire que certaines applications (ex. : 01,05,efi-shop)
#    --mvn CHEMIN     Exécutable Maven à utiliser
#    -h, --help       Afficher cette aide
#
#  Variables : JAVA_HOME (JDK 6, 7 ou 8 recommandé ; JDK 8 de préférence).
#
#  Étapes : 1) vérification Java/Maven  2) compatibilité des versions
#           3) compilation  4) tests JUnit + JavaScript  5) WAR  6) EAR
#           7) contrôle jar tf  8) bytecode major 50  9) copie dans dist/
#           10) rapport dist/build-report.md
#
#  Aucune action n'est effectuée sur WebSphere : ce script ne fait que construire.
# =============================================================================
set -u

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APPS_CONF="$SCRIPT_DIR/apps.conf"
DIST="$ROOT_DIR/dist"
LOGS="$DIST/logs"
REPORT="$DIST/build-report.md"

OFFLINE=""; SKIP_TESTS=0; ONLY=""; MVN=""
while [ $# -gt 0 ]; do
  case "$1" in
    --offline) OFFLINE="-o" ;;
    --skip-tests) SKIP_TESTS=1 ;;
    --only) shift; ONLY="${1:-}" ;;
    --mvn) shift; MVN="${1:-}" ;;
    -h|--help) sed -n '2,24p' "$0"; exit 0 ;;
    *) echo "Option inconnue : $1 (voir --help)" >&2; exit 2 ;;
  esac
  shift
done

if [ -t 1 ]; then G=$'\033[32m'; R=$'\033[31m'; Y=$'\033[33m'; C=$'\033[36m'; B=$'\033[1m'; N=$'\033[0m'; else G=; R=; Y=; C=; B=; N=; fi
step() { echo ""; echo "${B}${C}══ $* ══${N}"; }
ok()   { echo "  ${G}✔${N} $*"; }
ko()   { echo "  ${R}✘ $*${N}"; }
warn() { echo "  ${Y}⚠ $*${N}"; }
die()  { ko "$*"; exit 1; }

START_TS=$(date +%s)

# -----------------------------------------------------------------------------
step "1/10 Vérification de Java"
# -----------------------------------------------------------------------------
javac_version() { "$1/bin/javac" -version 2>&1 | awk '/javac/ {print $2; exit}'; }
java_major() {   # 1.8.0_x → 8 ; 1.6.0 → 6 ; 11.0.2 → 11
  case "$1" in 1.*) echo "$1" | cut -d. -f2 ;; *) echo "$1" | cut -d. -f1 ;; esac
}
is_supported() { local m; m=$(java_major "$1"); [ "$m" = 6 ] || [ "$m" = 7 ] || [ "$m" = 8 ]; }

candidates=()
[ -n "${JAVA_HOME:-}" ] && candidates+=("$JAVA_HOME")
if [ -x /usr/libexec/java_home ]; then
  for v in 1.8 1.7 1.6; do h=$(/usr/libexec/java_home -v "$v" 2>/dev/null) && candidates+=("$h"); done
fi
for h in /Library/Java/JavaVirtualMachines/*1.8*/Contents/Home /Library/Java/JavaVirtualMachines/*-8*/Contents/Home \
         /usr/lib/jvm/java-1.8.0* /usr/lib/jvm/java-8* /usr/lib/jvm/jdk1.8* /usr/lib/jvm/temurin-8* \
         /opt/IBM/WebSphere/AppServer*/java/8.0 /opt/IBM/WebSphere/AppServer*/java_1.7_64 /opt/IBM/WebSphere/AppServer*/java; do
  [ -d "$h" ] && candidates+=("$h")
done

JDK=""
for h in "${candidates[@]}"; do
  [ -x "$h/bin/javac" ] || continue
  v=$(javac_version "$h")
  if [ -n "$v" ] && is_supported "$v"; then JDK="$h"; JAVAC_VERSION="$v"; break; fi
done
if [ -z "$JDK" ]; then
  ko "Aucun JDK 6, 7 ou 8 (avec javac) n'a été trouvé."
  cat <<'EOF'
     Pour préparer la chaîne de compilation :
       • macOS  : installer un JDK 8 (ex. Eclipse Temurin 8) puis
                  export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
       • RHEL   : sudo dnf install java-1.8.0-openjdk-devel
                  export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk
       • Ubuntu : sudo apt install openjdk-8-jdk
       • Sur le serveur WebSphere : le SDK IBM fourni avec WAS 8.5.5 contient javac
                  export JAVA_HOME=/opt/IBM/WebSphere/AppServer_2/java/8.0   (ou .../java)
     Un JRE seul (sans javac) ne suffit pas. Les JDK ≥ 12 ne savent plus cibler Java 6.
EOF
  exit 1
fi
export JAVA_HOME="$JDK"
export PATH="$JAVA_HOME/bin:$PATH"
JAVA_RUNTIME="$("$JAVA_HOME/bin/java" -version 2>&1 | head -1)"
ok "JDK retenu : $JAVA_HOME"
ok "javac $JAVAC_VERSION — $JAVA_RUNTIME"
command -v jar >/dev/null 2>&1 || die "commande 'jar' introuvable dans $JAVA_HOME/bin"

# -----------------------------------------------------------------------------
step "2/10 Vérification de Maven et compatibilité"
# -----------------------------------------------------------------------------
if [ -z "$MVN" ]; then
  if command -v mvn >/dev/null 2>&1; then MVN="$(command -v mvn)"
  else MVN="$(ls -d "$ROOT_DIR"/tools/apache-maven-*/bin/mvn 2>/dev/null | tail -1)"; fi
fi
[ -n "$MVN" ] && [ -x "$MVN" ] || die "Maven introuvable. Installez Maven 3.6+ ou placez-le dans tools/apache-maven-x.y.z/"
MVN_VERSION="$("$MVN" -v 2>/dev/null | awk '/Apache Maven/ {print $3; exit}')"
[ -n "$MVN_VERSION" ] || die "Impossible d'exécuter $MVN (Maven 3.9 nécessite Java 8 pour fonctionner)."
ok "Maven $MVN_VERSION ($MVN)"
JM=$(java_major "$JAVAC_VERSION")
if [ "$JM" -lt 8 ]; then
  case "$MVN_VERSION" in 3.9*|4.*) die "Maven $MVN_VERSION requiert Java 8 : utilisez Maven 3.2.5 avec un JDK $JM, ou un JDK 8." ;; esac
fi
ok "Cible de compilation : source/target 1.6 (bytecode major 50), Servlet 3.0, Java EE 6, API javax.*"
[ "$JM" = 8 ] && ok "Contrôle des API Java 6 par animal-sniffer (signature java16) pendant la compilation"
[ -n "$OFFLINE" ] && ok "Mode hors ligne activé (-o)"
[ $SKIP_TESTS -eq 1 ] && warn "Tests unitaires désactivés (--skip-tests)"

# -----------------------------------------------------------------------------
# Liste des applications
# -----------------------------------------------------------------------------
APPS=()
while IFS='|' read -r dir app ctx label; do
  case "$dir" in ''|\#*) continue ;; esac
  if [ -n "$ONLY" ]; then
    match=0
    for o in $(echo "$ONLY" | tr ',' ' '); do
      case "$dir" in "$o"|"$o"-*) match=1 ;; esac
      [ "$app" = "$o" ] && match=1
    done
    [ $match -eq 1 ] || continue
  fi
  APPS+=("$dir|$app|$ctx|$label")
done < "$APPS_CONF"
[ ${#APPS[@]} -gt 0 ] || die "Aucune application sélectionnée."

mkdir -p "$DIST/ear" "$DIST/war" "$LOGS"

# -----------------------------------------------------------------------------
step "3-6/10 Compilation, tests, WAR et EAR (${#APPS[@]} application(s))"
# -----------------------------------------------------------------------------
RESULTS=()
FAILED=0
for entry in "${APPS[@]}"; do
  IFS='|' read -r dir app ctx label <<EOF
$entry
EOF
  adir="$ROOT_DIR/applications/$dir"
  log="$LOGS/$dir.log"
  printf '  %-28s ' "$dir"
  t0=$(date +%s)
  mvn_args=(-B $OFFLINE clean package)
  [ $SKIP_TESTS -eq 1 ] && mvn_args+=(-DskipTests)
  (cd "$adir" && "$MVN" "${mvn_args[@]}") > "$log" 2>&1
  rc=$?
  dt=$(( $(date +%s) - t0 ))

  tests=0; tfail=0
  for r in "$adir"/web/target/surefire-reports/*.txt; do
    [ -f "$r" ] || continue
    line=$(grep -m1 'Tests run:' "$r")
    n=$(echo "$line" | sed -n 's/.*Tests run: \([0-9]*\).*/\1/p')
    f=$(echo "$line" | sed -n 's/.*Failures: \([0-9]*\).*/\1/p')
    e=$(echo "$line" | sed -n 's/.*Errors: \([0-9]*\).*/\1/p')
    tests=$((tests + ${n:-0})); tfail=$((tfail + ${f:-0} + ${e:-0}))
  done
  if [ $SKIP_TESTS -eq 1 ]; then tinfo="NOT TESTED"
  elif [ $tests -eq 0 ]; then tinfo="0 (JS uniquement)"
  else tinfo="$tests ($tfail échec)"; fi

  war="$adir/web/target/$app.war"; ear="$adir/ear/target/$app.ear"
  if [ $rc -eq 0 ] && [ -s "$war" ] && [ -s "$ear" ]; then
    cp "$war" "$DIST/war/"; cp "$ear" "$DIST/ear/"
    echo "${G}BUILD SUCCESS${N}  ${dt}s  tests Java : $tinfo"
    RESULTS+=("$dir|$app|$ctx|$label|BUILD SUCCESS|$tinfo|$dt")
  else
    echo "${R}BUILD FAILED${N}   ${dt}s  (journal : ${log#$ROOT_DIR/})"
    grep -E '\[ERROR\]' "$log" | head -5 | sed 's/^/        /'
    RESULTS+=("$dir|$app|$ctx|$label|BUILD FAILED|$tinfo|$dt")
    FAILED=$((FAILED+1))
  fi
done

# -----------------------------------------------------------------------------
step "4bis/10 Tests JavaScript (Nashorn)"
# -----------------------------------------------------------------------------
JS_STATUS="NOT TESTED"; JS_DETAIL=""
if [ $SKIP_TESTS -eq 1 ]; then
  warn "ignorés (--skip-tests)"
else
  js_out="$("$ROOT_DIR/tests/js/run-js-tests.sh" 2>&1)"; js_rc=$?
  printf '%s\n' "$js_out" > "$LOGS/js-tests.log"
  pass_n=$(printf '%s\n' "$js_out" | sed -n 's/.* : \([0-9]*\) réussi(s), \([0-9]*\) échec(s).*/\1/p' | awk '{s+=$1} END {print s+0}')
  fail_n=$(printf '%s\n' "$js_out" | sed -n 's/.* : \([0-9]*\) réussi(s), \([0-9]*\) échec(s).*/\2/p' | awk '{s+=$1} END {print s+0}')
  JS_DETAIL="$pass_n réussi(s), $fail_n échec(s)"
  case $js_rc in
    0) JS_STATUS="PASS"; ok "Tests JavaScript : $JS_DETAIL" ;;
    2) JS_STATUS="NOT TESTED"; JS_DETAIL="jjs (Nashorn) indisponible dans ce JDK"; warn "$JS_DETAIL" ;;
    *) JS_STATUS="FAIL"; ko "Tests JavaScript : $JS_DETAIL (journal : dist/logs/js-tests.log)"; FAILED=$((FAILED+1)) ;;
  esac
fi

# -----------------------------------------------------------------------------
step "7-8/10 Contrôle des archives (jar tf) et du bytecode"
# -----------------------------------------------------------------------------
VERIFY_FILE="$LOGS/verify.result"; : > "$VERIFY_FILE"
ears=()
for entry in "${RESULTS[@]}"; do
  IFS='|' read -r dir app ctx label st rest <<EOF
$entry
EOF
  [ "$st" = "BUILD SUCCESS" ] && ears+=("$DIST/ear/$app.ear")
done
if [ ${#ears[@]} -gt 0 ]; then
  VERIFY_RESULT_FILE="$VERIFY_FILE" "$SCRIPT_DIR/verify-ear.sh" --quiet "${ears[@]}" | tee "$LOGS/verify-ear.log" | sed -n '/Résumé/,$p'
  [ "${PIPESTATUS[0]}" -eq 0 ] || FAILED=$((FAILED+1))
  # Contrôle complémentaire des WAR autonomes
  for w in "$DIST"/war/*.war; do
    jar tf "$w" | grep -q '^WEB-INF/web.xml$' || { ko "$(basename "$w") : WEB-INF/web.xml absent"; FAILED=$((FAILED+1)); }
  done
  ok "WAR autonomes : WEB-INF/web.xml présent dans chaque archive de dist/war"
fi

# -----------------------------------------------------------------------------
step "9/10 Livrables et empreintes"
# -----------------------------------------------------------------------------
if command -v shasum >/dev/null 2>&1; then SHA="shasum -a 256"; else SHA="sha256sum"; fi
(cd "$DIST" && $SHA ear/*.ear war/*.war > SHA256SUMS 2>/dev/null)
ls -l "$DIST"/ear/*.ear 2>/dev/null | awk '{printf "  %8d o  %s\n", $5, $NF}' | sed "s:$ROOT_DIR/::"
ok "Empreintes SHA-256 : dist/SHA256SUMS"

# -----------------------------------------------------------------------------
step "10/10 Rapport de build"
# -----------------------------------------------------------------------------
size_of() { [ -f "$1" ] && wc -c < "$1" | awk '{printf "%.1f Ko", $1/1024}' || echo "—"; }
verify_of() { awk -F'|' -v n="$1" '$2 == n { printf "%s (%s/%s)", $3, $4, $5 }' "$VERIFY_FILE"; }
{
  echo "# Rapport de build — EFI Academy WebSphere Application Lab"
  echo ""
  echo "- **Date** : $(date '+%Y-%m-%d %H:%M:%S %Z')"
  echo "- **Hôte de build** : $(uname -sm)"
  echo "- **JDK** : javac $JAVAC_VERSION — \`$JAVA_HOME\`"
  echo "- **Maven** : $MVN_VERSION"
  echo "- **Cible** : Java EE 6 / Servlet 3.0 / bytecode Java 6 (major 50) — IBM WebSphere ND 8.5.5"
  echo "- **Options** : ${OFFLINE:-en ligne}$( [ $SKIP_TESTS -eq 1 ] && echo ', tests désactivés')"
  echo "- **Durée totale** : $(( $(date +%s) - START_TS )) s"
  echo ""
  echo "| # | Application | Context root | Build | Tests JUnit | WAR | EAR | Vérification EAR |"
  echo "|---|---|---|---|---|---|---|---|"
  for entry in "${RESULTS[@]}"; do
    IFS='|' read -r dir app ctx label st tinfo dt <<EOF
$entry
EOF
    echo "| ${dir%%-*} | $label (\`$app.ear\`) | \`$ctx\` | **$st** | $tinfo | $(size_of "$DIST/war/$app.war") | $(size_of "$DIST/ear/$app.ear") | $(verify_of "$app") |"
  done
  echo ""
  echo "**Tests JavaScript (Nashorn)** : $JS_STATUS${JS_DETAIL:+ — $JS_DETAIL}"
  echo ""
  echo "Contrôles de \`verify-ear.sh\` : application.xml Java EE 6, context root, WAR embarqué, web.xml 3.0,"
  echo "JSP d'accueil et pages 404/500, CSS/JS, bytecode major 50, absence de \`jakarta.*\`, d'API conteneur embarquée et de secrets."
  echo ""
  echo "Journaux détaillés : \`dist/logs/\`. Empreintes : \`dist/SHA256SUMS\`."
  echo ""
  echo "> Ce rapport couvre la construction et les tests hors conteneur. Le déploiement réel sur WebSphere"
  echo "> (scripts/deploy-all.sh) n'est jamais lancé par ce script."
} > "$REPORT"
ok "Rapport : ${REPORT#$ROOT_DIR/}"

echo ""
if [ $FAILED -eq 0 ]; then
  echo "${G}${B}✔ BUILD GLOBAL RÉUSSI — ${#RESULTS[@]} application(s) construites et vérifiées.${N}"
  exit 0
fi
echo "${R}${B}✘ $FAILED problème(s) détecté(s) — consultez dist/build-report.md et dist/logs/.${N}"
exit 1
