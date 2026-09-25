# AGENT.md — 给接手本仓库的 AI 的交接说明

## 🚨 发版铁律
**每个新版本必须先测试，只有用户明确说「可以转正」才允许发正式 Release。**
dev 版只走 staging（artifact + 预发布 Release `ci`），不打 tag。

## 项目一句话
「学习时光」：纯离线 Android 番茄钟 + 待办 + 坚持打卡 + 日历。无 Gradle、无第三方库，`bash build.sh` 出签名 APK。
界面**全部纯代码搭建**（`Ui.java` 工具 + 自绘 `RingView` / `MonthView`），`res/` 只有配色、主题、图标。

## 沙箱环境（与 wordsprint 相同的限制）
- Arena 沙箱出网白名单：`dl.google.com`、`api.adoptium.net`、`repo1.maven.org`、release 资产域都不通 →
  **沙箱里装不出 JDK / Android SDK**，本地编不了 APK。
- 能做的：`python3 scripts/refcheck.py`（R 引用 / manifest 类名粗筛）、`bash -n` 语法检查；
  `pip install jdk4py`（venv 里）能拿到 JRE + keytool（没有 javac）。
- **把 CI 当 javac 用**：推分支 → `staging.yml` 跑测试 + 构建。读结果：
  ```bash
  gh run list -R zhzx2026/studytime --branch <branch> -L 3
  SHA=$(git rev-parse HEAD)
  gh api repos/zhzx2026/studytime/commits/$SHA/check-runs \
    --jq '.check_runs[]|select(.name=="ci-diagnostics")|.output.text' | tail -80
  ```
  （Actions 日志下载链接走 blob 域，沙箱读不到；所以 staging 会把日志尾部写进 `ci-diagnostics` check-run。）
- 测试包直链：`https://github.com/zhzx2026/studytime/releases/download/ci/studytime-<分支id>.apk`

## 架构要点 / 踩坑预防
1. **计时只存「到点时刻」**（`Pomo.endAt`），不靠 Handler 计数：锁屏、被杀、Doze 都不影响；
   `Pomo.settle(now)` 会把离开期间错过的阶段按时间线补结算（自动续的下一段从上一段 endAt 起算）。
2. **进程内只有一份 Pomo**（`PomoCtl.get`）：页面 ticker 和 `AlarmReceiver` 都走它，避免两边各结算一次、专注记录重复。
3. 每次改计时状态都要 `PomoCtl.save()`：落盘 + 重排闹钟 + 刷新常驻通知，三件事绑在一起不许拆。
4. 精确闹钟：Android 13+ 用 `USE_EXACT_ALARM`（自动授予），12/12L 用 `SCHEDULE_EXACT_ALARM`；
   `canScheduleExactAlarms()` 为 false 时降级 `setAndAllowWhileIdle`（可能晚几分钟），重开 App 仍会补结算。
5. 纯逻辑类（`Day` / `Streak` / `Pomo`）**不许 import android.\***，`run_tests.sh` 有检查；新增纯逻辑类要加进 `PURE` 列表并写测试。
6. 日期统一用 int 键 `yyyyMMdd`（`Day.key`），别混 `Calendar` / 时间戳做「哪一天」。
7. 签名：没配 Secret `KEYSTORE_B64` 时用仓库里的公开测试钥匙 `signing/debug.keystore`。
   **一旦改用私有钥匙，已装的旧包要先卸载**（证书不同无法覆盖安装）—— 换钥匙前先问用户。
8. `BuildInfo.java` 由 build.sh 在编译前改写、构建后还原，别手改。

## 当前状态
- 会话流水账写 `docs/logs/<分支id>.md`（只写自己的文件）。
