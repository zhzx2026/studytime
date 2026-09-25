# 分支分工

| 分支 / 位置 | 谁写 | 放什么 |
|---|---|---|
| `main` | 合 PR | 正式线：只放 stable `X.0`；push 后 `release.yml` 自动打 tag + 发 Release |
| 预发布 Release `ci` | 只有 CI | 测试包聚合：根 `studytime.apk` / `update.json` = 最近一次构建；`studytime-<id>.apk` / `update-<id>.json` = 各分支坑位 |
| `arena/<id>-studytime` | Agent 会话 | 工作分支，代码只在这里改；每次 push → `staging.yml` 出测试包 |

分支 id：`bash scripts/branch_id.sh`（`arena/01a0d86e-studytime` → `arena01a0d86e`），
用于 artifact 名、ci 资产名、App 设置页脚的构建标识。
