#!/usr/bin/env bash
# 主机侧测试：本地与 CI 同一条命令。被测源码就是发版用的那份 src/（纯 Java 的逻辑类）。
#   bash scripts/run_tests.sh
set -e
cd "$(dirname "$0")/.."
export LANG=C.UTF-8 LC_ALL=C.UTF-8

echo "== refcheck（R.* 引用 / manifest 类名是否都存在）"
python3 scripts/refcheck.py

for cand in "$PWD/tools/jdk17/bin" "${JAVA_HOME:-}/bin"; do
  if [ -n "$cand" ] && [ -x "$cand/javac" ]; then export PATH="$cand:$PATH"; break; fi
done
command -v javac >/dev/null 2>&1 || { echo "!! 找不到 javac（需要 JDK 11+）"; exit 1; }

S=src/com/zhzx/studytime
PURE="$S/Day.java $S/Streak.java $S/Pomo.java"
# 纯逻辑类不许 import android.*（否则主机侧编不过，也说明逻辑和界面缠在一起了）
if grep -l '^import android\.' $PURE; then echo "!! 上面这些纯逻辑类 import 了 android.*"; exit 1; fi
rm -rf test/out && mkdir -p test/out
echo "== javac"
javac -encoding UTF-8 -nowarn -d test/out $PURE test/T.java test/PomoTest.java test/StreakTest.java
echo "== PomoTest（番茄钟状态机：暂停不计时 / 长休轮次 / 离开期间补结算 / 编码容错）"
java -cp test/out PomoTest
echo "== StreakTest（连续天数 + 日期工具：跨月 / 跨年 / 闰年 / 月历网格）"
java -cp test/out StreakTest
echo "ALL TESTS PASSED"
