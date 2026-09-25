# 学习时光 StudyTime

**当前版本：v0.1（dev）** <!-- CURRENT-VERSION -->

一个**纯离线**的 Android 学习效率 App：**番茄钟 + 待办 TODO + 坚持打卡 + 日历**四合一。
结构照搬 [zhzx2026/wordsprint](https://github.com/zhzx2026/wordsprint)：无 Gradle、无第三方库，
`bash build.sh`（aapt2 → javac → d8 → zipalign → apksigner）直接出签名 APK；CI 自动构建，装机包在 GitHub Releases。

## 下载

| 通道 | 地址 | 说明 |
|---|---|---|
| 正式版 | [Releases → Latest](https://github.com/zhzx2026/studytime/releases/latest) | 只有转正（stable `X.0`）才更新 |
| 测试版 | [Release `ci`](https://github.com/zhzx2026/studytime/releases/tag/ci) | 每次推工作分支 CI 自动覆盖；`studytime-<分支id>.apk` 是各分支坑位 |

App 内：设置 → 关于与更新 → 选通道 →「检查更新」。

## 四个页签

### 🍅 专注（番茄钟）
- 专注 25 / 短休 5 / 长休 15 分钟，每 4 个番茄长休一次（设置里都能改）
- 圆环倒计时；开始 / 暂停 / 继续 / 重置（放弃要确认）/ 跳过
- **关联待办**：选一件事再专注，完成的番茄自动记到那件事上（🍅 已完成/预估）
- **后台 / 锁屏 / 被杀都不丢**：运行中只存「到点时刻」，到点由 AlarmManager 精确唤醒 → 结算 + 提醒通知；
  重开 App 会把离开期间错过的阶段补结算（开了自动续就按时间线接力，不漂移）
- 常驻通知显示倒计时；可选：专注结束自动休息、休息结束自动开始下一个、振动、提示音、计时时屏幕常亮
- 今日番茄数 / 今日专注时长 / 连续专注天数 + 今日专注记录

### ✅ 待办
- 今天（到期 + 过期 + 今天已完成）/ 全部待办 / 已完成 三个筛选
- 回车快速添加；详情里设优先级（普通/重要/紧急）、截止日（今天/明天/任意日期）、预估番茄数、备注
- 点圆圈完成，点行编辑，长按删除，▶ 一键去专注这件事；过期标红

### 🔥 坚持（习惯打卡）
- 每个习惯：图标 + 名称 + 目标天数（7/21/30/66/100/365）
- 一键打卡；🔥 当前连续 / 最佳连续 / 累计天数；最近 7 天小圆点可**补卡**
- 目标进度条，达成弹 🏆；今日打卡总进度

### 📅 日历
- 月视图：格子底色 = 当天专注热力（5 档），红点 = 有待办截止/完成，绿点 = 有习惯打卡
- 本月汇总：专注时长 / 番茄数 / 完成待办 / 打卡次数
- 点任意一天看明细：专注记录（时间段 + 做的事）、当天待办（可勾完成）、习惯（今天及以前可补卡）；
  「＋ 待办」直接往那天加任务

### 其它
- 跟随系统深色模式
- 数据只存本机；设置 → 数据：复制备份到剪贴板 / 从剪贴板恢复（换机用）

## 构建

```bash
bash scripts/run_tests.sh     # 主机侧测试（refcheck + PomoTest + StreakTest），本地与 CI 同一条命令
SDK_ROOT=/path/to/android-sdk bash build.sh   # 需要 build-tools 34 + platform 34 + JDK 17
```
推工作分支（`arena/**`）→ `staging.yml` 出测试包；合进 main 且版本是 `X.0` → `release.yml` 打 tag 发正式版。
签名说明见 [signing/README.md](signing/README.md)。

## 目录
```
AndroidManifest.xml     版本号唯一来源（只许 scripts/version.sh 改）
build.sh                无 Gradle 构建脚本
src/com/zhzx/studytime/ 全部 Java 源码（界面纯代码搭建，无 XML 布局）
  Pomo / Streak / Day     纯 Java 逻辑（主机侧可测，不许 import android.*）
  PomoCtl / AlarmReceiver / Notify   计时的 Android 胶水：落盘、闹钟、通知
  Store                   SharedPreferences + JSON 数据层
  MainActivity + FocusPage / TodoPage / HabitPage / CalendarPage   四个页签
  TaskDialog / SettingsActivity / Update / RingView / MonthView / Ui
res/                    配色（values-night 为深色）、图标
test/                   主机侧 JVM 测试（T.java 极简断言）
scripts/                版本 / 测试 / 分支 id / update.json / refcheck
signing/                公开测试签名钥匙（见其 README）
.github/workflows/      staging.yml（测试包）· release.yml（正式版）
```
文档：[AGENTS.md](AGENTS.md) · [AGENT.md](AGENT.md) · [VERSIONING.md](VERSIONING.md) · [BRANCHING.md](BRANCHING.md) · [CHANGELOG.md](CHANGELOG.md)
