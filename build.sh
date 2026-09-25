#!/usr/bin/env bash
# 番茄时光 APK builder（无 Gradle）：aapt2 + javac/ecj + d8 + zipalign + apksigner
# 工具链由 scripts/setup_tools.sh 装到 ./tools（官方源不通时自动走 GitHub 镜像兜底）。
set -e
cd "$(dirname "$0")"
export LANG=C.UTF-8 LC_ALL=C.UTF-8

SDK=tools/android-sdk
BT=$SDK/build-tools/34.0.0
AJ=$SDK/platforms/android-34/android.jar

# ── java / javac（tools/jdk 优先，其次系统）────────────────────────────────
J=""
for cand in tools/jdk/bin /var/tmp/jdk17/bin "${JAVA_HOME:-}/bin"; do
  [ -n "$cand" ] && [ -x "$cand/java" ] && { J="$cand/java"; break; }
done
[ -z "$J" ] && command -v java >/dev/null 2>&1 && J=$(command -v java)
[ -n "$J" ] || { echo "!! 没有 java。跑：bash scripts/setup_tools.sh"; exit 1; }
JH=$(dirname "$(dirname "$J")")
JAVAC=""
for cand in "$JH/bin" /var/tmp/jdk17/bin "${JAVA_HOME:-}/bin"; do
  [ -n "$cand" ] && [ -x "$cand/javac" ] && { JAVAC="$cand/javac"; break; }
done
[ -z "$JAVAC" ] && command -v javac >/dev/null 2>&1 && JAVAC=$(command -v javac)
KEYTOOL=""
[ -x "$JH/bin/keytool" ] && KEYTOOL="$JH/bin/keytool"
[ -n "$KEYTOOL" ] || KEYTOOL=$(command -v keytool 2>/dev/null || true)

[ -x "$BT/aapt2" ] || { echo "!! 缺 $BT/aapt2。跑：bash scripts/setup_tools.sh"; exit 1; }
[ -f "$AJ" ] || { echo "!! 缺 $AJ。跑：bash scripts/setup_tools.sh"; exit 1; }
[ -f "$BT/d8.jar" ] || { echo "!! 缺 $BT/d8.jar。跑：bash scripts/setup_tools.sh"; exit 1; }

VER=$(grep -oE 'versionName="[^"]*"' AndroidManifest.xml | sed 's/versionName="//;s/"//')
VC=$(grep -oE 'versionCode="[0-9]*"' AndroidManifest.xml | sed 's/versionCode="//;s/"//')
B=build
rm -rf $B && mkdir -p $B/gen $B/classes $B/dex

# ── 构建标识（签完自动还原，见 AGENT.md）───────────────────────────────────
SHA7=$(git rev-parse --short=7 HEAD 2>/dev/null || echo unknown)
STAMP="${STAMP_OVERRIDE:-$SHA7}"
cp src/com/aidemo/studytime/BuildInfo.java $B/BuildInfo.java.bak
cat > src/com/aidemo/studytime/BuildInfo.java <<EOF
package com.aidemo.studytime;

/** 构建标识：本文件由 build.sh 编译前自动生成，签完自动还原，别手改。 */
public final class BuildInfo {
    public static final String STAMP = "$STAMP";

    private BuildInfo() {}
}
EOF
restore_buildinfo() { cp $B/BuildInfo.java.bak src/com/aidemo/studytime/BuildInfo.java; }
trap restore_buildinfo EXIT

echo "== aapt2 compile (v$VER / code $VC)"
"$BT/aapt2" compile --dir res -o $B/res.zip
echo "== aapt2 link"
"$BT/aapt2" link -o $B/app.unsigned.apk -I "$AJ" --manifest AndroidManifest.xml \
   -R $B/res.zip --java $B/gen --min-sdk-version 26 --target-sdk-version 34 \
   --version-code "$VC" --version-name "$VER" --auto-add-overlay

echo "== javac / ecj"
find src $B/gen -name '*.java' > $B/srcs.txt
# 编 Java 8 的 lambda 要能解析 java.lang.invoke.LambdaMetafactory —— android.jar 里没有，
# 必须把 build-tools 的 core-lambda-stubs.jar 挂上（AGP 同款做法）
LSTUB=$BT/core-lambda-stubs.jar
[ -f "$LSTUB" ] || { echo "!! 缺 $LSTUB。跑：bash scripts/setup_tools.sh"; exit 1; }
if [ -n "$JAVAC" ]; then
  "$JAVAC" -encoding UTF-8 -source 8 -target 8 -nowarn -bootclasspath "$AJ" \
    -cp "$LSTUB" -d $B/classes @$B/srcs.txt 2> $B/compile.log || { cat $B/compile.log; exit 1; }
  grep -E 'error|warning' $B/compile.log && true || true
else
  [ -f tools/ecj.jar ] || { echo "!! 无 javac 也无 tools/ecj.jar"; exit 1; }
  "$J" -jar tools/ecj.jar -1.8 -nowarn -encoding UTF-8 -bootclasspath "$AJ" \
    -classpath "$LSTUB" -d $B/classes @$B/srcs.txt 2> $B/compile.log || { cat $B/compile.log; exit 1; }
fi

echo "== d8"
find $B/classes -name '*.class' > $B/cls.txt
"$J" -cp "$BT/d8.jar" com.android.tools.r8.D8 --release --lib "$AJ" \
  --min-api 26 --output $B/dex @$B/cls.txt

echo "== pack dex"
(cd $B && zip -q -X app.unsigned.apk -j dex/classes.dex)

echo "== zipalign"
rm -f $B/app.aligned.apk
if [ -x "$BT/zipalign" ] && "$BT/zipalign" -f 4 $B/app.unsigned.apk $B/app.aligned.apk >/dev/null 2>&1; then
  echo "   (原生 zipalign)"
else
  # 镜像仓捞的原生二进制缺 libc++ → 退回纯 Python 实现（对齐语义相同）
  python3 scripts/zipalign.py -f 4 $B/app.unsigned.apk $B/app.aligned.apk
fi

echo "== sign"
KS=${KS_FILE:-studytime.keystore}
if [ ! -f "$KS" ]; then
  if [ "${ALLOW_FRESH_KEY:-0}" = "1" ]; then
    [ -n "$KEYTOOL" ] || { echo "!! 没有 keytool"; exit 1; }
    echo "  没有 $KS —— 现造一把测试钥匙（放在仓库根、已 gitignore；务必备份）"
    "$KEYTOOL" -genkeypair -keystore "$KS" -storepass "${KS_PASS:-studytime}" \
      -keypass "${KS_PASS:-studytime}" -alias studytime -keyalg RSA -keysize 2048 \
      -validity 10000 -dname "CN=StudyTime Dev, OU=Demo, O=StudyTime, L=Beijing, C=CN" >/dev/null 2>&1
  else
    cat <<HELP
✗ 缺 $KS —— 拒绝用新钥匙签名（新证书 = 老设备无法覆盖安装，用户进度会丢）。
  · 正常发版：把备份的 studytime.keystore 放回仓库根（已 gitignore，别提交）；
  · 只想本地出包验证：ALLOW_FRESH_KEY=1 bash build.sh
HELP
    exit 3
  fi
fi
"$J" -cp "$BT/apksigner.jar" com.android.apksigner.ApkSignerTool sign \
  --ks "$KS" --ks-pass pass:"${KS_PASS:-studytime}" --key-pass pass:"${KS_PASS:-studytime}" \
  --out $B/studytime-signed.apk $B/app.aligned.apk
"$J" -cp "$BT/apksigner.jar" com.android.apksigner.ApkSignerTool verify \
  $B/studytime-signed.apk

cp $B/studytime-signed.apk studytime-v$VER.apk
SIZE=$(stat -c%s studytime-v$VER.apk 2>/dev/null || stat -f%z studytime-v$VER.apk)
echo "OK → studytime-v$VER.apk （$SIZE 字节，code $VC，stamp $STAMP）"
