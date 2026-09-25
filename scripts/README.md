# scripts

| 脚本 | 用途 |
|---|---|
| `run_tests.sh` | 主机侧测试（refcheck + 纯逻辑类 javac + PomoTest / StreakTest），本地与 CI 同一条命令 |
| `refcheck.py` | 无 JDK 时的粗筛：`R.*` 引用、manifest 类名 / 资源是否存在 |
| `version.sh` | 版本号唯一执行器（status / bump-dev / promote / set / check） |
| `branch_id.sh` | 分支短 id（artifact 名、ci 坑位、构建标识） |
| `make_update_json.sh` | 生成 App 检查更新读的 `update.json`（notes = RELEASE_NOTES.md 正文） |
