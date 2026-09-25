#!/usr/bin/env bash
# 主机侧测试：本地与 CI 同一条命令。被测源码就是发版用的那一份（src/），不存副本。
#   bash scripts/run_tests.sh
# 编译器：优先 javac（系统或 tools/jdk）；都没有就用 tools/ecj.jar（Eclipse 编译器）。
set -e
cd "$(dirname "$0")/.."
export LANG=C.UTF-8 LC_ALL=C.UTF-8

S=src/com/aidemo/studytime
PURE="$S/Cfg.java $S/Pomo.java $S/Todo.java $S/Recs.java $S/Cal.java $S/Streak.java"
TESTS="test/T.java test/PomoTest.java test/TodoTest.java test/RecsTest.java test/StreakTest.java test/CalTest.java test/CfgTest.java"

# ── 找编译器 ────────────────────────────────────────────────────────────────
J=""
for cand in tools/jdk/bin /var/tmp/jdk17/bin "${JAVA_HOME:-}/bin"; do
  if [ -n "$cand" ] && [ -x "$cand/java" ]; then J="$cand/java"; break; fi
done
[ -z "$J" ] && command -v java >/dev/null 2>&1 && J=$(command -v java)

JAVAC=""
for cand in tools/jdk/bin /var/tmp/jdk17/bin "${JAVA_HOME:-}/bin"; do
  if [ -n "$cand" ] && [ -x "$cand/javac" ]; then JAVAC="$cand/javac"; break; fi
done
[ -z "$JAVAC" ] && command -v javac >/dev/null 2>&1 && JAVAC=$(command -v javac)

rm -rf test/out && mkdir -p test/out

if [ -n "$JAVAC" ]; then
  echo "== javac（宿主 JDK）"
  "$JAVAC" -encoding UTF-8 -nowarn -d test/out $PURE $TESTS
else
  [ -n "$J" ] || { echo "!! 没有 java。跑：bash scripts/setup_tools.sh"; exit 1; }
  [ -f tools/ecj.jar ] || { echo "!! 没有 javac 也没有 tools/ecj.jar。跑：bash scripts/setup_tools.sh"; exit 1; }
  AJ=tools/android-sdk/platforms/android-34/android.jar
  [ -f "$AJ" ] || { echo "!! 缺 $AJ。跑：bash scripts/setup_tools.sh"; exit 1; }
  echo "== ecj（无 javac 环境；bootclasspath 指 android.jar，java.* 运行时由宿主 JVM 提供）"
  "$J" -jar tools/ecj.jar -1.8 -nowarn -encoding UTF-8 -bootclasspath "$AJ" -d test/out $PURE $TESTS
fi

[ -n "$J" ] || J=java
run() { echo "== $1"; "$J" -cp test/out "$1"; }

run PomoTest
run TodoTest
run RecsTest
run StreakTest
run CalTest
run CfgTest
echo "ALL HOST TESTS PASS"
