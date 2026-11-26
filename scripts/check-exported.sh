#!/usr/bin/env bash
# Exported-component gate: every exported component must declare a permission
# or signature guard. Threat model section 1 (malicious app calling the broker).
set -euo pipefail

MANIFEST="${1:-app/src/main/AndroidManifest.xml}"

python3 - "$MANIFEST" <<'PY'
import sys, xml.etree.ElementTree as ET

A = "{http://schemas.android.com/apk/res/android}"
tree = ET.parse(sys.argv[1])
bad = []
for el in tree.getroot().iter():
    if el.tag not in ("activity", "activity-alias", "service", "receiver", "provider"):
        continue
    if el.get(A + "exported") == "true" and not el.get(A + "permission"):
        bad.append(f"{el.tag}: {el.get(A + 'name', '?')}")
if bad:
    print("FAIL: exported component without android:permission guard:")
    print("\n".join(bad))
    sys.exit(1)
print("PASS: no unguarded exported components")
PY
