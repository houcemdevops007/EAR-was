#!/usr/bin/env bash
# =============================================================================
#  EFI Academy — WebSphere Application Lab
#  verify-ear.sh : contrôle structurel et sécurité des archives EAR produites.
#
#  Usage :
#    ./scripts/verify-ear.sh                    # vérifie toutes les EAR de dist/ear
#    ./scripts/verify-ear.sh dist/ear/efi-chat.ear [autre.ear ...]
#    ./scripts/verify-ear.sh --quiet ...        # n'affiche que le résumé
#
#  Contrôles par EAR :
#    archive lisible (jar tf), META-INF/application.xml (Java EE 6), context root
#    attendu (scripts/apps.conf), WAR embarqué, web.xml Servlet 3.0, JSP d'accueil,
#    pages d'erreur 404/500, ressources CSS/JS, bytecode Java 6 (major 50),
#    aucune référence jakarta.*, aucune API servlet embarquée, aucun secret apparent.
#
#  Code retour : 0 si toutes les EAR sont conformes, 1 sinon, 2 si erreur d'usage.
# =============================================================================
set -u

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
APPS_CONF="$SCRIPT_DIR/apps.conf"
QUIET=0

if [ -t 1 ]; then G=$'\033[32m'; R=$'\033[31m'; Y=$'\033[33m'; B=$'\033[1m'; N=$'\033[0m'; else G=; R=; Y=; B=; N=; fi

EARS=()
while [ $# -gt 0 ]; do
  case "$1" in
    -q|--quiet) QUIET=1 ;;
    -h|--help) sed -n '2,20p' "$0"; exit 0 ;;
    -*) echo "Option inconnue : $1" >&2; exit 2 ;;
    *) EARS+=("$1") ;;
  esac
  shift
done

command -v jar >/dev/null 2>&1 || { echo "${R}ERREUR${N} : la commande 'jar' (JDK) est introuvable dans le PATH." >&2; exit 2; }

if [ ${#EARS[@]} -eq 0 ]; then
  while IFS='|' read -r dir app ctx label; do
    case "$dir" in ''|\#*) continue ;; esac
    EARS+=("$ROOT_DIR/dist/ear/$app.ear")
  done < "$APPS_CONF"
fi

expected_context() {
  local name="$1"
  awk -F'|' -v n="$name" '$0 !~ /^#/ && $2 == n { print $3 }' "$APPS_CONF"
}

TOTAL_FAIL=0
SUMMARY=()

for ear in "${EARS[@]}"; do
  name="$(basename "$ear" .ear)"
  fails=0; checks=0
  say()  { [ $QUIET -eq 1 ] || printf '%s\n' "$*"; }
  pass() { checks=$((checks+1)); say "   ${G}✔${N} $*"; }
  fail() { checks=$((checks+1)); fails=$((fails+1)); say "   ${R}✘ $*${N}"; }

  say ""
  say "${B}▶ $name${N}  ($ear)"
  if [ ! -s "$ear" ]; then
    fail "archive absente ou vide"
    SUMMARY+=("$name|FAIL|$checks|$fails|archive absente")
    TOTAL_FAIL=$((TOTAL_FAIL+1)); continue
  fi

  tmp="$(mktemp -d "${TMPDIR:-/tmp}/efi-verify.XXXXXX")"
  mkdir -p "$tmp/ear" "$tmp/war"
  ear_abs="$(cd "$(dirname "$ear")" && pwd)/$(basename "$ear")"

  # 1. Lisibilité et listing
  if listing="$(jar tf "$ear_abs" 2>/dev/null)"; then pass "archive lisible (jar tf : $(printf '%s\n' "$listing" | wc -l | tr -d ' ') entrées)"; else fail "archive illisible"; fi
  (cd "$tmp/ear" && jar xf "$ear_abs")

  # 2. Descripteur application.xml
  appxml="$tmp/ear/META-INF/application.xml"
  if [ -f "$appxml" ]; then pass "META-INF/application.xml présent"; else fail "META-INF/application.xml manquant"; fi
  if [ -f "$appxml" ] && grep -q 'version="6"' "$appxml" && grep -q 'java.sun.com/xml/ns/javaee' "$appxml"; then
    pass "application.xml au schéma Java EE 6"
  else
    fail "application.xml n'est pas au schéma Java EE 6"
  fi

  # 3. Context root
  want="$(expected_context "$name")"
  got="$(sed -n 's:.*<context-root>\(.*\)</context-root>.*:\1:p' "$appxml" 2>/dev/null | head -1)"
  if [ -z "$want" ]; then
    fail "application inconnue de scripts/apps.conf"
  elif [ "$got" = "$want" ]; then
    pass "context root $got"
  else
    fail "context root '$got' (attendu '$want')"
  fi

  # 4. WAR embarqué
  weburi="$(sed -n 's:.*<web-uri>\(.*\)</web-uri>.*:\1:p' "$appxml" 2>/dev/null | head -1)"
  war="$tmp/ear/$weburi"
  if [ -n "$weburi" ] && [ -s "$war" ]; then pass "module web $weburi présent"; else fail "module web '$weburi' absent de l'EAR"; fi
  [ -s "$war" ] && (cd "$tmp/war" && jar xf "$war")
  W="$tmp/war"

  # 5. web.xml Servlet 3.0
  if [ -f "$W/WEB-INF/web.xml" ] && grep -q 'version="3.0"' "$W/WEB-INF/web.xml"; then
    pass "WEB-INF/web.xml (Servlet 3.0)"
  else
    fail "WEB-INF/web.xml manquant ou pas en version 3.0"
  fi

  # 6. Pages et ressources
  for f in index.jsp erreur/404.jsp erreur/500.jsp assets/css/efi-base.css assets/css/app.css assets/js/efi-common.js; do
    if [ -s "$W/$f" ]; then pass "$f"; else fail "$f manquant"; fi
  done
  njs=$(find "$W/assets/js" -name '*.js' 2>/dev/null | wc -l | tr -d ' ')
  if [ "$njs" -ge 2 ]; then pass "$njs fichiers JavaScript applicatifs"; else fail "aucun JavaScript applicatif (hors efi-common.js)"; fi

  # 7. Bytecode Java 6
  nclass=0; bad=""
  while IFS= read -r c; do
    nclass=$((nclass+1))
    major=$(od -An -j6 -N2 -tu1 "$c" | awk '{print $1*256+$2}')
    [ "$major" = "50" ] || bad="$bad ${c#$W/}($major)"
  done < <(find "$W" -name '*.class')
  if [ $nclass -eq 0 ]; then fail "aucune classe Java compilée"
  elif [ -z "$bad" ]; then pass "$nclass classes en bytecode Java 6 (major 50)"
  else fail "classes hors Java 6 :$bad"; fi

  # 8. Dépendances : pas de jakarta, pas d'API servlet embarquée
  if grep -rIl 'jakarta\.' "$W" "$tmp/ear/META-INF" >/dev/null 2>&1 || grep -rl 'jakarta/servlet' "$W" >/dev/null 2>&1; then
    fail "référence jakarta.* détectée : $(grep -rl 'jakarta[./]' "$W" | head -3 | tr '\n' ' ')"
  else
    pass "aucune référence jakarta.*"
  fi
  libs="$(ls "$W/WEB-INF/lib" 2>/dev/null | tr '\n' ' ')"
  if printf '%s' "$libs" | grep -Eiq 'servlet-api|jsp-api|javaee-api|jakarta'; then
    fail "API du conteneur embarquée dans WEB-INF/lib : $libs"
  else
    pass "WEB-INF/lib sans API du conteneur${libs:+ ($libs)}"
  fi

  # 9. Recherche de secrets (fichiers texte uniquement)
  secret_re='(-----BEGIN [A-Z ]*PRIVATE KEY-----|AKIA[0-9A-Z]{16}|ghp_[A-Za-z0-9]{36}|xox[baprs]-[A-Za-z0-9-]{10,}|(password|passwd|pwd|secret|api[_-]?key|access[_-]?token)["'"'"']?[[:space:]]*[:=][[:space:]]*["'"'"'][^"'"'"'$<{[:space:]]{4,}["'"'"'])'
  hits="$(grep -rIEin "$secret_re" "$W" "$tmp/ear/META-INF" 2>/dev/null | grep -v '\.class:' | head -5)"
  if [ -z "$hits" ]; then pass "aucun secret apparent (clés, mots de passe, jetons)"; else fail "secret potentiel : $(printf '%s' "$hits" | sed "s:$tmp/::g" | head -3)"; fi

  rm -rf "$tmp"
  if [ $fails -eq 0 ]; then
    SUMMARY+=("$name|PASS|$checks|0|")
  else
    SUMMARY+=("$name|FAIL|$checks|$fails|")
    TOTAL_FAIL=$((TOTAL_FAIL+1))
  fi
done

echo ""
echo "${B}Résumé de la vérification des EAR${N}"
printf '  %-24s %-6s %s\n' "Application" "État" "Contrôles"
for s in "${SUMMARY[@]}"; do
  IFS='|' read -r n st ck fl why <<EOF
$s
EOF
  if [ "$st" = PASS ]; then col=$G; else col=$R; fi
  printf '  %-24s %s%-6s%s %s/%s réussis %s\n' "$n" "$col" "$st" "$N" "$((ck-fl))" "$ck" "$why"
  echo "VERIFY|$n|$st|$((ck-fl))|$ck" >> "${VERIFY_RESULT_FILE:-/dev/null}"
done
[ $TOTAL_FAIL -eq 0 ] && { echo "${G}Toutes les archives sont conformes.${N}"; exit 0; }
echo "${R}$TOTAL_FAIL archive(s) non conforme(s).${N}"; exit 1
