#!/usr/bin/env bash
# 生成 update.json（App「检查更新」读它）。
#   bash scripts/make_update_json.sh <apk 下载直链> <输出路径>
# notes = RELEASE_NOTES.md 去掉首行标题（只写当前这一版，历史挪 CHANGELOG.md）。
set -e
cd "$(dirname "$0")/.."
URL=$1; OUT=${2:-dist/update.json}
mkdir -p "$(dirname "$OUT")"
VER=$(grep -oE 'versionName="[^"]*"' AndroidManifest.xml | sed 's/versionName="//;s/"//')
VC=$(grep -oE 'versionCode="[0-9]*"' AndroidManifest.xml | sed 's/versionCode="//;s/"//')
STAMP="$(bash scripts/branch_id.sh 2>/dev/null || echo local)·$(git rev-parse --short=7 HEAD 2>/dev/null || echo unknown)"
python3 - "$VER" "$VC" "$URL" "$OUT" "$STAMP" <<'PY'
import json, sys
ver, vc, url, out, stamp = sys.argv[1:]
lines = open("RELEASE_NOTES.md", encoding="utf-8").read().splitlines()
notes = "\n".join(lines[1:]).strip()
json.dump({"versionName": ver, "versionCode": int(vc), "apk": url, "notes": notes, "build": stamp},
          open(out, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
print(open(out, encoding="utf-8").read())
PY
