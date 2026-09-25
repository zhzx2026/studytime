#!/usr/bin/env bash
# 版本号唯一执行器（规则见 VERSIONING.md）。不许手写 sed 改 AndroidManifest.xml。
#   bash scripts/version.sh status      看当前版本 / 通道
#   bash scripts/version.sh bump-dev    dev 迭代：X.Y → X.(Y+1)；stable X.0 → X.1；code+1
#   bash scripts/version.sh promote     转正：dev X.Y → stable (X+1).0；code+1
#   bash scripts/version.sh set X.Y [code]
#   bash scripts/version.sh check       CI 门禁：格式必须是 X.Y（整数），code 为正整数
set -e
cd "$(dirname "$0")/.."
export LANG=C.UTF-8 LC_ALL=C.UTF-8
M=AndroidManifest.xml
VER=$(grep -oE 'versionName="[^"]*"' $M | sed 's/versionName="//;s/"//')
VC=$(grep -oE 'versionCode="[0-9]*"' $M | sed 's/versionCode="//;s/"//')
MAJ=${VER%%.*}; MIN=${VER#*.}
chan() { [ "${1#*.}" = "0" ] && echo stable || echo dev; }

# code 取 max(本地, 远端各分支 manifest) + 1，防多分支撞号（拿不到远端就只看本地）
next_code() {
  local max=$VC
  if git remote get-url origin >/dev/null 2>&1; then
    for ref in $(git for-each-ref --format='%(refname)' refs/remotes/origin 2>/dev/null); do
      c=$(git show "$ref:$M" 2>/dev/null | grep -oE 'versionCode="[0-9]*"' | grep -oE '[0-9]+' || true)
      [ -n "$c" ] && [ "$c" -gt "$max" ] && max=$c
    done
  fi
  echo $((max + 1))
}

write() { # $1=name $2=code
  local n=$1 c=$2 ch
  ch=$(chan "$n")
  sed -i -E "s/versionName=\"[^\"]*\"/versionName=\"$n\"/; s/versionCode=\"[0-9]*\"/versionCode=\"$c\"/" $M
  if [ -f RELEASE_NOTES.md ]; then sed -i -E "1s/.*/# 学习时光 v$n/" RELEASE_NOTES.md; fi
  if [ -f README.md ]; then
    sed -i -E "s/^\*\*当前版本：.*<!-- CURRENT-VERSION -->/**当前版本：v$n（$ch）** <!-- CURRENT-VERSION -->/" README.md
  fi
  echo "✓ v$VER (code $VC) → v$n (code $c) [$ch]"
}

case "${1:-status}" in
  status)
    echo "当前：v$VER（code $VC）· $(chan "$VER")"
    if [ "$(chan "$VER")" = dev ]; then echo "下一步：继续迭代 bump-dev → v$MAJ.$((MIN + 1))；转正 promote → v$((MAJ + 1)).0"
    else echo "下一步：新一轮 bump-dev → v$MAJ.1"; fi ;;
  bump-dev) write "$MAJ.$((MIN + 1))" "$(next_code)" ;;
  promote)
    [ "$(chan "$VER")" = dev ] || { echo "✗ 已经是 stable v$VER，没东西可转正"; exit 1; }
    write "$((MAJ + 1)).0" "$(next_code)" ;;
  set)
    [[ "${2:-}" =~ ^[0-9]+\.[0-9]+$ ]] || { echo "用法：set X.Y [code]"; exit 1; }
    write "$2" "${3:-$(next_code)}" ;;
  check)
    [[ "$VER" =~ ^[0-9]+\.[0-9]+$ ]] || { echo "✗ versionName=$VER 不是 X.Y"; exit 1; }
    [[ "$VC" =~ ^[1-9][0-9]*$ ]] || { echo "✗ versionCode=$VC 非法"; exit 1; }
    head -1 RELEASE_NOTES.md | grep -q "v$VER\$" || { echo "✗ RELEASE_NOTES.md 首行不是「# 学习时光 v$VER」（用 version.sh 改版本号）"; exit 1; }
    echo "✓ v$VER（code $VC）· $(chan "$VER")" ;;
  *) echo "未知命令：$1"; exit 1 ;;
esac
