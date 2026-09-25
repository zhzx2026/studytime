# 版本规则（沿用 wordsprint）

| 通道 | 格式 | 示例 | 说明 |
|---|---|---|---|
| dev | `X.Y`（Y ≥ 1） | 0.1、0.2 … | 所有迭代在这里做，每轮交付 +0.1 |
| stable | `X.0` | 1.0、2.0 | 用户确认后转正：dev `X.Y` → `(X+1).0` |

- 新一轮：stable `X.0` → dev `X.1`。
- `versionCode` 只增不减：每次 bump 取 `max(本地, 各远端分支) + 1`。App「检查更新」只认 code 严格变大。
- `AndroidManifest.xml` 是唯一来源；`RELEASE_NOTES.md` 首行、`README.md` 当前版本行由 `version.sh` 同步。

```bash
bash scripts/version.sh status
bash scripts/version.sh bump-dev   # 每轮交付测试包前
bash scripts/version.sh promote    # 用户说转正时 → 提交 → 合 PR 进 main → release.yml 打 tag 发版
bash scripts/version.sh check      # CI 门禁
```

铁律：版本号只用 `version.sh` 改；dev 不打 tag；`RELEASE_NOTES.md` 只写当前这一版。
