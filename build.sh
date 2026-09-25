#!/usr/bin/env bash
# 学习时光 APK builder（无 Gradle）：aapt2 → javac → d8 → zipalign → apksigner
# 与 zhzx2026/wordsprint 同一套结构。需要：Android build-tools 34 + platform 34 + JDK 17。
#   本地：SDK_ROOT=/path/to/sdk bash build.sh
#   CI  ：.github/workflows/staging.yml / release.yml 自动准备工具链
set -e
cd "$(dirname "$0")"
export LANG=C.UTF-8 LC_ALL=C.UTF-8
if [ -z "${SDK_ROOT:-}" ] && [ -d tools/android-sdk ]; then SDK_ROOT="$PWD/tools/android-sdk"; fi
SDK=${SDK_ROOT:-/var/tmp/android-sdk}
BT=$SDK/build-tools/34.0.0
AJ=$SDK/platforms/android-34/android.jar
if [ -n "${JDK_HOME:-}" ]; then export JAVA_HOME=$JDK_HOME; export PATH=$JAVA_HOME/bin:$PATH
elif [ -x tools/jdk17/bin/javac ]; then export JAVA_HOME="$PWD/tools/jdk17"; export PATH=$JAVA_HOME/bin:$PATH
fi
export PATH=$BT:$PATH
[ -x "$BT/aapt2" ] || { echo "✗ 找不到 build-tools：$BT（设置 SDK_ROOT）"; exit 2; }
[ -f "$AJ" ] || { echo "✗ 找不到 android.jar：$AJ"; exit 2; }

VER=$(grep -oE 'versionName="[^"]*"' AndroidManifest.xml | sed 's/versionName="//;s/"//')
VC=$(grep -oE 'versionCode="[0-9]*"' AndroidManifest.xml | sed 's/versionCode="//;s/"//')
B=build
rm -rf $B && mkdir -p $B/gen $B/classes $B/dex

# 构建标识（设置页脚显示，一眼认出装的是哪条分支的哪次构建）
BID=$(bash scripts/branch_id.sh 2>/dev/null || echo local)
SHA7=$(git rev-parse --short=7 HEAD 2>/dev/null || echo unknown)
STAMP="$BID·$SHA7"
BI=src/com/zhzx/studytime/BuildInfo.java
cp $BI $B/BuildInfo.java.bak
cat > $BI <<JAVA
package com.zhzx.studytime;

/** 构建标识：本文件由 build.sh 在编译前自动生成（分支id·短sha），手改无效、构建后自动还原。 */
public final class BuildInfo {
    public static final String STAMP = "$STAMP";

    private BuildInfo() {}
}
JAVA
restore() { cp $B/BuildInfo.java.bak $BI 2>/dev/null || true; }
trap restore EXIT

echo "== aapt2 compile"
aapt2 compile --dir res -o $B/res.zip
echo "== aapt2 link (v$VER / code $VC)"
aapt2 link -o $B/app.unsigned.apk -I "$AJ" --manifest AndroidManifest.xml \
   -R $B/res.zip --java $B/gen --min-sdk-version 26 --target-sdk-version 34 \
   --version-code "$VC" --version-name "$VER" --auto-add-overlay
echo "== javac"
find src $B/gen -name '*.java' > $B/srcs.txt
# 注意：android.jar 里没有 java.lang.invoke.LambdaMetafactory，用 -bootclasspath android.jar 时 lambda 编不过。
# 所以 java.* 取 JDK 的 Java 8 API（--release 8），android.* / org.json 从 classpath 的 android.jar 取；d8 负责把 lambda 脱糖。
javac -encoding UTF-8 --release 8 -nowarn -cp "$AJ" -d $B/classes @$B/srcs.txt \
  2> $B/javac.log || { cat $B/javac.log; exit 1; }
echo "== d8"
find $B/classes -name '*.class' > $B/cls.txt
d8 --release --lib "$AJ" --min-api 26 --output $B/dex @$B/cls.txt
echo "== pack dex"
(cd $B && zip -q -X app.unsigned.apk -j dex/classes.dex)
echo "== zipalign"
zipalign -f 4 $B/app.unsigned.apk $B/app.aligned.apk

echo "== sign"
# 钥匙优先级：$KS_FILE > studytime.keystore（CI 从 Secret KEYSTORE_B64 恢复，不入库）> signing/debug.keystore（公开测试钥匙）
KS_PASS=${KS_PASS:-studytime}
KS=${KS_FILE:-}
if [ -z "$KS" ] && [ -f studytime.keystore ]; then KS=studytime.keystore; fi
if [ -z "$KS" ]; then
  KS=signing/debug.keystore; KS_PASS=studytime
  echo "   (使用仓库内公开测试钥匙 signing/debug.keystore —— 见 signing/README.md)"
fi
apksigner sign --ks "$KS" --ks-pass pass:$KS_PASS --key-pass pass:$KS_PASS \
  --v1-signing-enabled true --v2-signing-enabled true --out $B/studytime-signed.apk $B/app.aligned.apk
cp $B/studytime-signed.apk studytime-v$VER.apk
apksigner verify --print-certs $B/studytime-signed.apk | head -3
ls -la studytime-v$VER.apk
echo "BUILD OK（v$VER / code $VC / $STAMP）"
