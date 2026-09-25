# AGENTS.md

本仓库的 AI 协作规范见同目录 [AGENT.md](AGENT.md)。结构与规矩沿用 [zhzx2026/wordsprint](https://github.com/zhzx2026/wordsprint)。

硬规矩：

1. **发版铁律**：新版本必须先装机测试；只有用户明确说「转正」（或合 PR 进 main）才发正式 Release。
2. **版本号只许 `bash scripts/version.sh` 改**（`status` / `bump-dev` / `promote` / `set X.Y` / `check`），规则见 [VERSIONING.md](VERSIONING.md)。
3. **`RELEASE_NOTES.md` 只写当前这一版**（它原样成为 Release 正文和 App「发现新版本」弹窗文字），历史挪 [CHANGELOG.md](CHANGELOG.md)。
4. **改完必跑 `bash scripts/run_tests.sh`**；沙箱没有 JDK/SDK 时至少跑 `python3 scripts/refcheck.py`，然后推分支让 CI 编译
   （失败日志看 `ci-diagnostics` check-run，见 AGENT.md）。
5. 分支分工见 [BRANCHING.md](BRANCHING.md)：代码只在 `arena/<id>-studytime` 工作分支改。
