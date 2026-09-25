# arena01a0d86e（arena/01a0d86e-studytime）

## 2026-09-25 · v0.1 / code 1 — 初版
- 需求：模仿 wordsprint 结构，做一个番茄学习法 + TODO + 坚持 + 日历于一体的 APK。
- 结构：无 Gradle `build.sh`、`scripts/`（version / run_tests / branch_id / refcheck / make_update_json）、
  `staging.yml`（测试包 → artifact + 预发布 Release `ci`）、`release.yml`（main 上 X.0 → tag + Release）、
  AGENTS / AGENT / VERSIONING / BRANCHING / CHANGELOG / RELEASE_NOTES。
- 沙箱无 JDK / SDK；本地只跑 refcheck，编译与测试交给 CI（`ci-diagnostics` check-run 回读日志）。
- 签名：无法设置仓库 Secret，采用公开测试钥匙 `signing/debug.keystore`（证书固定 → 可覆盖安装）。
