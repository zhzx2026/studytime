#!/usr/bin/env bash
# 分支短 id（与 wordsprint 同规则）：arena/01a0d86e-studytime → arena01a0d86e；main → main
# CI 里 checkout 可能是 detached HEAD，由 workflow 传 GIT_BRANCH=github.ref_name。
set -e
cd "$(dirname "$0")/.."
b="${GIT_BRANCH:-}"
[ -n "$b" ] || b="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || true)"
[ -n "$b" ] || b="local"
case "$b" in
  arena/*) t="${b#arena/}"; echo "arena${t%%-*}" ;;
  *)       echo "$b" | tr '/_' '--' | tr -cd 'A-Za-z0-9-' | sed 's/-\{2,\}/-/g; s/^-//; s/-$//' ;;
esac
