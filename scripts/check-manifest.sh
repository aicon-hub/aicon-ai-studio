#!/usr/bin/env bash
# Network and permission gate for AiCoN AI Studio.
# Owner decision 2026-11-27: INTERNET is allowed for user-started downloads only
# (models, engine, skill packs). No other permission is allowed in the default
# build until the owner approves it. Fails on anything outside the allowlist.
set -euo pipefail

MANIFEST="${1:-app/src/main/AndroidManifest.xml}"
ALLOWED=("android.permission.INTERNET")

mapfile -t FOUND < <(grep -oE 'android:name="android\.permission\.[A-Z_]+"' "$MANIFEST" | sed 's/android:name="//; s/"//' | sort -u)

fail=0
for p in "${FOUND[@]}"; do
    ok=""
    for a in "${ALLOWED[@]}"; do [ "$p" = "$a" ] && ok=1; done
    if [ -z "$ok" ]; then
        echo "FAIL: permission not allowlisted: $p"
        fail=1
    fi
done

# Dependencies must not smuggle permissions in: strip them with tools:node="remove".
if grep -q 'tools:node="remove"' "$MANIFEST"; then
    echo "note: manifest strips dependency permissions via tools:node=remove"
fi

[ "$fail" -eq 0 ] && echo "PASS: manifest permissions within allowlist (${ALLOWED[*]})"
exit "$fail"
