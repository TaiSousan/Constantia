#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

fail() { echo "ERROR: $*" >&2; exit 1; }

command -v java >/dev/null 2>&1 || fail "Java não encontrado. Use JDK 17."
JAVA_MAJOR="$(java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p')"
[[ "$JAVA_MAJOR" == "17" ]] || fail "JDK 17 é obrigatório; encontrado Java $JAVA_MAJOR."

if command -v gradle >/dev/null 2>&1; then
  GRADLE=(gradle)
elif [[ -f gradle/wrapper/gradle-wrapper.jar ]]; then
  GRADLE=(./gradlew)
else
  fail "Gradle 9.6.0 não encontrado e gradle-wrapper.jar ausente. Instale Gradle 9.6.0 ou gere o wrapper."
fi

GRADLE_VERSION="$(${GRADLE[@]} --version | sed -n 's/^Gradle \([0-9.]*\)$/\1/p' | head -1)"
[[ "$GRADLE_VERSION" == "9.6.0" ]] || fail "Gradle 9.6.0 é obrigatório; encontrado ${GRADLE_VERSION:-desconhecido}."

[[ -n "${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}" ]] || fail "ANDROID_HOME/ANDROID_SDK_ROOT não definido."
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT}}"
[[ -d "$SDK/platforms/android-37" ]] || fail "Android SDK Platform 37 não instalada."

python3 tools/preflight.py
"${GRADLE[@]}" --no-daemon :app:assembleDebug --stacktrace
python3 tools/verify_apk.py app/build/outputs/apk/debug/app-debug.apk
