# 番茄时光 StudyTime

**当前版本：v1.0** <!-- CURRENT-VERSION -->

一个精致的**番茄学习法 Android 应用**：番茄钟 · 待办 · 连续打卡 · 月历热力图，四件事一体、完全离线。
无 Gradle、无第三方 UI 库，`bash build.sh` 直出签名 APK；仓库结构与 [刷单词 WordsPrint](https://github.com/zhzx2026/wordsprint) 同款（aapt2 + javac + d8 + apksigner 纯 SDK 工具链）。

## 功能

1. **番茄钟**：专注 / 短休 / 长休三段自动轮转（默认 25/5/15，每 4 轮一次长休，全部可调）；
   自绘圆环倒计时、正点提示音 + 振动、到点自动进入下一段（可关，关了就停下等你按「继续」）
2. **杀后台续存**：计时走墙钟（`endAt - now`），锁屏、闪退、被系统杀掉重开都接着走；
   意外退出的补账规则见 `AGENT.md`——最多只承认一个番茄，绝不凭空刷统计
3. **待办清单**：加/勾/删、预计番茄数、按「全部 / 未完成 / 已完成」过滤；
   长按可设为**当前专注任务**（🎯 绑定后专注页显示它，做完一个番茄自动 +1 进度）
4. **连续打卡（坚持）**：每日目标（默认 4 个番茄）达标即续签；今天还没达标**不断签**；
   当前连续 / 历史最高 / 今日进度条 / 本周七列柱状图 / 累计四项，配分档激励文案
5. **月历热力图**：周一开头的月视图，格子颜色 = 当天番茄离目标的距离（5 档），
   点格子看当天明细（番茄数 · 专注分钟 · 完成任务数），‹ › 翻月、一键回今天
6. **设置**：时长档位、每日目标、自动续、提示音/振动、清空数据、关于（版本 + 构建标识）
7. **深浅色**：跟随系统的浅色（暖纸）/ 深色（navy）双主题，热力图两套配色都看得见格子

数据全部落在本机 SharedPreferences，**零联网、零账号**；权限只要 `VIBRATE`。

## 构建与测试（无 Gradle，纯 SDK 工具链）

```bash
bash scripts/setup_tools.sh      # ① 装工具链到 ./tools（官方源不通时自动走 GitHub 镜像兜底）
bash scripts/run_tests.sh        # ② 主机侧测试（本地与 CI 同一条命令，编译的就是发版那份源码）
python3 scripts/refcheck.py      # ③ 静态体检：资源引用 / Activity 注册 / 括号配平 / 版本号
bash build.sh                    # ④ aapt2 → javac/ecj → d8 → zipalign → apksigner，出 studytime-vX.Y.apk
ALLOW_FRESH_KEY=1 bash build.sh  #    第一次本地出包（现造测试钥匙，正式发版前必须换成备份钥匙）
```

`run_tests.sh` 覆盖：`PomoTest`（状态机 + 补账上限 + 快照恢复）、`TodoTest`（转义落盘 + 脏行容错）、
`RecsTest`（每日记录）、`StreakTest`（连续打卡边界）、`CalTest`（闰年 / 跨月 / 周一起算 / 热力分档）、
`CfgTest`（配置容错夹取）。**发版前必过。**

产物：minSdk 26 / targetSdk 34，自适应桌面图标（vector 番茄表盘），APK 约 1MB 量级。

## 目录速览

```
build.sh               # 出包流水线（aapt2 → javac/ecj → d8 → zipalign → apksigner）
AndroidManifest.xml    # 包名 com.aidemo.studytime，两个 Activity
src/com/aidemo/studytime/
  Pomo/Todo/Recs/Cal/Streak/Cfg   # 纯逻辑六件套：零 Android 依赖，主机直接可测
  MainActivity/SettingsActivity   # 单 Activity 四页签 + 设置页（UI 全代码拼）
  RingView/CalView/WeekBarsView/MeterView  # 自绘视图
  Prefs/Ui/SoundFx/App/BuildInfo  # 落盘 / 手搓 UI 工具 / 音效 / 入口 / 构建标识
res/                   # 只有主题、颜色、图标（浅/深两套）
test/                  # 主机测试（T.java 小断言 + 六个 Test）
scripts/               # setup_tools / run_tests / refcheck / version / zipalign(py)
.github/workflows/ci.yml  # push 即测即建，产物挂 artifact
```

## 版本与发布

- 版本号只许 `bash scripts/version.sh set <name> <code>` 改（同步 manifest 与 README）；
  `version.sh check` 对账，CI 里红了就是有人手改了版本号
- `RELEASE_NOTES.md` 只写当前这一版（发布正文就用它），历史进 `CHANGELOG.md`
- 签名钥匙 `studytime.keystore` 不入库（.gitignore）；**务必备份**——丢了老设备就只能卸载重装

## 说明

- 计时在后台不弹通知（v1.0 范围）：回到 App 会按墙钟把账一次补齐；
  番茄正点时若 App 在前台，有提示音 + 振动
- 「完成任务数」按天计，反复勾选不会重复刷数（`Todo.Task.counted` 去重）
