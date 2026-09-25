#!/usr/bin/env bash
# 版本号唯一入口：改 manifest + 同步 README 的 CURRENT-VERSION 标记。
#   bash scripts/version.sh set 1.1 2     # versionName=1.1 versionCode=2
#   bash scripts/version.sh show          # 读当前
#   bash scripts/version.sh check         # manifest ↔ README 对账（CI 用）
set -e
cd "$(dirname "$0")/.."
MAN=AndroidManifest.xml

get_name() { grep -oE 'versionName="[^"]*"' $MAN | sed 's/versionName="//;s/"//'; }
get_code() { grep -oE 'versionCode="[0-9]*"' $MAN | sed 's/versionCode="//;s/"//'; }

cmd=${1:-show}
case "$cmd" in
  show)
    echo "versionName=$(get_name) versionCode=$(get_code)"
    ;;
  set)
    name=${2:?用法: version.sh set <versionName> <versionCode>}
    code=${3:?用法: version.sh set <versionName> <versionCode>}
    echo "$code" | grep -qE '^[0-9]+$' || { echo "!! versionCode 必须是整数"; exit 1; }
    sed -i "s/versionCode=\"[0-9]*\"/versionCode=\"$code\"/" $MAN
    sed -i "s/versionName=\"[^\"]*\"/versionName=\"$name\"/" $MAN
    # 同步 README 当前版本行（两种写法都认）
    if grep -q "CURRENT-VERSION" README.md; then
      sed -i -E "s/(\*\*当前版本：)v?[0-9.]+/\1v$name/" README.md
    fi
    echo "set → versionName=$name versionCode=$code（README 已同步）"
    ;;
  check)
    name=$(get_name); code=$(get_code)
    rd=$(grep -oE '\*\*当前版本：v?[0-9.]+' README.md | head -1 | grep -oE '[0-9.]+$' || true)
    if [ "$rd" != "$name" ]; then
      echo "!! README 版本($rd) ≠ manifest($name) —— 跑 version.sh set 同步"
      exit 1
    fi
    echo "version check OK: v$name / code $code"
    ;;
  *)
    echo "用法: version.sh {show|set <name> <code>|check}"; exit 2;;
esac
