#!/usr/bin/env bash
# Exported-component gate: every exported component must declare a permission
# or signature guard. Threat model section 1 (malicious app calling the broker).
# One platform-required exception: the single MAIN/LAUNCHER activity, which
# Android forces to be exported.
set -euo pipefail

MANIFEST="${1:-app/src/main/AndroidManifest.xml}"

python3 - "$MANIFEST" <<'PY'
import sys, xml.etree.ElementTree as ET

A = "{http://schemas.android.com/apk/res/android}"
tree = ET.parse(sys.argv[1])
bad = []
launchers = 0
for el in tree.getroot().iter():
    if el.tag not in ("activity", "activity-alias", "service", "receiver", "provider"):
        continue
    if el.get(A + "exported") != "true":
        continue
    if el.get(A + "permission"):
        continue
    filters = el.findall("intent-filter")
    is_launcher = any(
        any(a.get(A + "name") == "android.intent.action.MAIN" for a in f.findall("action"))
        and any(c.get(A + "name") == "android.intent.category.LAUNCHER" for c in f.findall("category"))
        for f in filters
    )
    if el.tag == "activity" and is_launcher:
        launchers += 1
        continue
    bad.append(f"{el.tag}: {el.get(A + 'name', '?')}")
if launchers > 1:
    bad.append(f"more than one MAIN/LAUNCHER activity exported ({launchers})")
if bad:
    print("FAIL: exported component without android:permission guard:")
    print("\n".join(bad))
    sys.exit(1)
print("PASS: no unguarded exported components (launcher exception: %d)" % launchers)
PY
