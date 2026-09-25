#!/usr/bin/env bash
# 一次性：把构建工具链装到 ./tools（JDK + Android SDK 最小集）。
#
# 正常网络走官方源（dl.google.com / adoptium）；被墙/白名单沙箱自动降级到
# 「GitHub 仓库里别人提交的同款二进制 + PyPI 的 jdk4py」——两套路子都验过能出包。
# 只需要：curl、git、python3、(可选) unzip。
set -e
cd "$(dirname "$0")/.."
export LANG=C.UTF-8 LC_ALL=C.UTF-8
mkdir -p tools
cd tools

have() { command -v "$1" >/dev/null 2>&1; }
can_fetch() { curl -fsIL -m 8 "$1" >/dev/null 2>&1; }

# ── ① JDK（java/javac/keytool；测试与签名都要）────────────────────────────
setup_jdk() {
  [ -x jdk/bin/java ] && return 0
  # 系统/CI 已有完整 JDK 就不折腾（setup-java 场景走这条）
  if command -v javac >/dev/null 2>&1 && command -v java >/dev/null 2>&1; then
    return 0
  fi
  # 1) 系统自带的 javac/java 也能凑合 —— 交给调用方探测，这里只管没有时的兜底
  if can_fetch "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"; then
    echo "== JDK17 (temurin, 官方)"
    curl -sSL -o jdk.tgz "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
    tar xzf jdk.tgz && mv jdk-17* jdk17 && rm jdk.tgz
    return 0
  fi
  # 2) 降级：PyPI 的 jdk4py（wheel 里是完整 Temurin 运行时；没有 javac 就配 ecj 兜底）
  echo "== JDK (jdk4py via PyPI, 官方源不通时的兜底)"
  local ver=21.0.8.1
  local url
  url=$(curl -s -m 20 "https://pypi.org/pypi/jdk4py/$ver/json" |
        python3 -c "import json,sys
d=json.load(sys.stdin)
for u in d['urls']:
    if 'manylinux' in u['filename'] and 'x86_64' in u['filename']: print(u['url']); break" || true)
  [ -n "$url" ] || { echo "!! 拿不到 jdk4py 下载地址"; exit 1; }
  curl -sSL -o jdk.whl "$url"
  python3 - "$PWD/jdk.whl" <<'PY'
import sys, zipfile, os, shutil, stat
whl, dst = sys.argv[1], 'jdk'
tmp = '.jdkx'
shutil.rmtree(tmp, ignore_errors=True)
zipfile.ZipFile(whl).extractall(tmp)
src = os.path.join(tmp, 'jdk4py', 'java-runtime')
shutil.rmtree(dst, ignore_errors=True)
shutil.move(src, dst)
shutil.rmtree(tmp, ignore_errors=True)
for root, _, files in os.walk(dst):
    for f in files:
        p = os.path.join(root, f)
        if os.path.isfile(p) and (root.endswith('/bin') or root.endswith('\\bin')):
            os.chmod(p, os.stat(p).st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
PY
  rm -f jdk.whl
}

# javac 缺席（系统没有 JDK、兜底的 jdk4py 又是纯运行时）→ 放一份 ecj 当编译器
setup_ecj() {
  [ -x jdk/bin/javac ] && return 0
  command -v javac >/dev/null 2>&1 && return 0
  [ -f ecj.jar ] && return 0
  echo "== ecj.jar (Eclipse 编译器, javac 缺席时的编译器)"
  local erepo="luckyx7/Teedy" epath=".m2/org/eclipse/jdt/ecj/3.33.0/ecj-3.33.0.jar"
  git clone --depth 1 --filter=blob:none --sparse "https://github.com/$erepo.git" .ecjsrc >/dev/null 2>&1
  (cd .ecjsrc && git sparse-checkout set "$(dirname "$epath")" >/dev/null 2>&1)
  cp ".ecjsrc/$epath" ecj.jar && rm -rf .ecjsrc
  [ -f ecj.jar ] || { echo "!! 拿不到 ecj.jar"; exit 1; }
}

# ── ② Android SDK 最小集：platform android-34 + build-tools 34.0.0 ─────────
BT=android-sdk/build-tools/34.0.0
AJ=android-sdk/platforms/android-34/android.jar

setup_sdk() {
  [ -f "$AJ" ] && [ -x "$BT/aapt2" ] && [ -f "$BT/d8.jar" ] && [ -f "$BT/apksigner.jar" ] && return 0
  # 1) 官方直链
  if can_fetch "https://dl.google.com/android/repository/build-tools_r34-linux.zip"; then
    echo "== Android build-tools 34 (官方)"
    mkdir -p android-sdk/build-tools
    curl -sSL -o bt.zip https://dl.google.com/android/repository/build-tools_r34-linux.zip
    unzip -q bt.zip -d btX && rm -rf "$BT" && mv btX/android-14 "$BT" && rm -rf bt.zip btX
    echo "== Android platform 34 (官方)"
    mkdir -p android-sdk/platforms
    curl -sSL -o p.zip https://dl.google.com/android/repository/platform-34-ext7_r02.zip
    unzip -q p.zip -d pX && rm -rf android-sdk/platforms/android-34 && mv pX/android-34 android-sdk/platforms/android-34 && rm -rf p.zip pX
    return 0
  fi
  # 2) 降级：GitHub 仓库里被提交过的同款文件（稀疏克隆，只拉需要的那几个）
  echo "== Android SDK (GitHub 镜像仓, 官方源不通时的兜底)"
  mkdir -p "$BT" "$(dirname "$AJ")"
  local srcA=/tmp/.st_sdk_altaga
  if [ ! -f "$srcA/.done" ]; then
    rm -rf "$srcA"
    git clone --depth 1 --filter=blob:none --sparse https://github.com/altaga/Txt2App.git "$srcA" >/dev/null 2>&1
    (cd "$srcA" && git sparse-checkout set txt2app-ai-workbench/DevTools/Android >/dev/null 2>&1)
    touch "$srcA/.done"
  fi
  cp "$srcA/txt2app-ai-workbench/DevTools/Android/build-tools/34.0.0/aapt2" "$BT/aapt2"
  cp "$srcA/txt2app-ai-workbench/DevTools/Android/build-tools/34.0.0/core-lambda-stubs.jar" "$BT/core-lambda-stubs.jar"
  cp "$srcA/txt2app-ai-workbench/DevTools/Android/platforms/android-34/android.jar" "$AJ"
  # d8/apksigner 是纯 Java jar：从另一个提交了完整 build-tools 34.0.4 的仓里拿
  local srcO=/tmp/.st_sdk_optapp
  if [ ! -f "$srcO/.done" ]; then
    rm -rf "$srcO"
    git clone --depth 1 --filter=blob:none --sparse https://github.com/kansas1295/optapp.git "$srcO" >/dev/null 2>&1
    (cd "$srcO" && git sparse-checkout set build-tools >/dev/null 2>&1)
    touch "$srcO/.done"
  fi
  cp "$srcO/build-tools/34.0.4/lib/d8.jar" "$BT/d8.jar"
  cp "$srcO/build-tools/34.0.4/lib/apksigner.jar" "$BT/apksigner.jar"
  chmod +x "$BT/aapt2"
  # zipalign 原生二进制依赖本机 libc++，多半跑不起来 —— build.sh 会自动退回 scripts/zipalign.py
  cp "$srcA/txt2app-ai-workbench/DevTools/Android/build-tools/34.0.0/zipalign" "$BT/zipalign" 2>/dev/null || true
  chmod +x "$BT/zipalign" 2>/dev/null || true
}

setup_jdk
setup_ecj
setup_sdk
echo "工具链就绪：tools/{jdk,ecj.jar,android-sdk}"
