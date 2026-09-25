# AGENT — 工程约定与坑（给接手的 AI / 人）

一句话：**纯逻辑与 Android 严格分层，测试编译的就是发版那份源码，版本号只走 version.sh。**

## 硬规矩

1. `Pomo/Todo/Recs/Cal/Streak/Cfg` 六个类**零 Android 依赖**（不 import android.\\*）。
   它们被 `run_tests.sh` 直接编译进主机测试——混进一个 `Context` 就把测试链路弄断了。
2. 测试**不复制源码**：`run_tests.sh` 从 `src/` 现编。test/ 下永远不存被测类副本
   （参考仓就是被副本坑过：改了 src 忘了副本，测试绿的是旧逻辑）。
3. 时间一律**参数化**（`tick(now)` / `Cal.todayKey(epochMs)`），测试固定 epoch，不许在纯逻辑里
   裸调 `System.currentTimeMillis()`——否则跨时区/跨零点的用例没法写。
4. 版本号只许 `bash scripts/version.sh set <name> <code>` 改；README 的当前版本行由它同步，
   `version.sh check` + `refcheck.py` 双对账。
5. 签名钥匙 `studytime.keystore` 绝不入库；新钥匙 = 老设备无法覆盖安装。首次出包
   `ALLOW_FRESH_KEY=1`，之后必须换回备份钥匙再发版。

## 关键设计（别「顺手优化」掉）

### 番茄补账（Pomo.tick）

杀后台/闪退回来时，到点的账要补，但**只承认一个番茄**：

- 当前阶段到点 → 结算；休息阶段可以顺着链继续补完（休息不记账），时间轴按原 `endAt` 串；
- 一旦又要完成**第二个 WORK** → 停表、剩余拉满，等人回来手动「继续」。

否则离开三小时回来凭空多出一排番茄，统计就成了玩具。`PomoTest` 第 8 组就是这条的回归锁。

### 任务完成计数（Todo.Task.counted）

勾选完成计入当天 `Recs.tasks`，`counted` 标志保证一条任务**只记一次**；
取消勾选当天回退（`recs.tasksOn(today) > 0` 才减）。防止反复勾选刷日历。

### 墙钟计时（不是 countDownTimer）

`running` 时剩余 = `endAt - now`，落盘只存 `endAt`；锁屏/后台都在走，onResume 一次补账。
不要改成 `Handler` 递减——锁屏就停了，参考仓的用户明确要「锁屏接着走」。

### 资源 ID 不连续

`R.color.heat0 + 1` **不是** `heat1`（aapt2 overlay 不保证连号）。要用就 `int[]` 显式列
（`MainActivity` 日历图例就是这么写的，`refcheck.py` 查不出这种错，靠 review）。

## 构建链的坑

- **aapt2 link 必须 `--auto-add-overlay`**，否则「resource does not override」直接红。
- **zipalign 原生二进制**（镜像仓捞的）依赖 `libc++.so`，多数沙箱跑不起来 → `build.sh`
  自动退回 `scripts/zipalign.py`（纯 Python，垫 local header extra 对齐 STORED 条目）。
  对齐必须在签名**之前**（apksigner 的 v2 块插在数据后）。
- **无 javac 环境**走 `tools/ecj.jar`（Eclipse 编译器）：`-1.8 -bootclasspath android.jar`。
  java.\* 由 android.jar 提供签名、宿主 JVM 提供实现，测试照常跑。
- `d8.jar` / `apksigner.jar` 是纯 Java，**平台无关**——Mac 仓里捞的也能在 Linux 用；
  `aapt2` / `zipalign` 这类原生件才分平台。
- 工具链来源（`scripts/setup_tools.sh`）：官方 dl.google/adoptium 优先，不通自动降级
  「GitHub 镜像仓 + PyPI jdk4py」。改网络环境先重跑它。

## 文档地图

- `README.md`：功能、构建、目录速览（CURRENT-VERSION 标记别删，refcheck 要用）
- `RELEASE_NOTES.md`：只写当前版本的发布文案
- `CHANGELOG.md`：历史版本归档
- `scripts/` 里每个脚本头部注释写了「干什么、什么时候用」

## 发版 checklist

1. `bash scripts/run_tests.sh` 全绿
2. `python3 scripts/refcheck.py` + `bash scripts/version.sh check` 过
3. `bash scripts/version.sh set X.Y <code>`（code 只增不减）
4. 更新 `RELEASE_NOTES.md`（只留这一版），上一版挪进 `CHANGELOG.md`
5. `bash build.sh`（确认用的是备份钥匙，不是 fresh）
6. tag + Release，正文 = RELEASE_NOTES.md
