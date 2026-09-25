# 签名

`debug.keystore` 是**公开的测试钥匙**（PKCS12，alias `studytime`，密码 `studytime`），
故意放进仓库：这样沙箱 / CI 在没有任何 Secret 的情况下，每次构建出来的 APK 证书都相同，**可以直接覆盖安装、数据不丢**。

证书 SHA-256：`06:DA:1D:41:C0:68:ED:08:34:13:83:01:FA:5B:B5:4A:2F:F7:B9:F6:23:DE:BE:94:69:EE:47:30:54:25:77:C8`

代价：任何人都能用它签一个「看起来是同一个 App」的包。个人自用问题不大；想正式分发时：

1. 本地生成私有钥匙：`keytool -genkeypair -keystore studytime.keystore -alias studytime -keyalg RSA -keysize 2048 -validity 12000`
2. 仓库 Settings → Secrets → Actions 新建 `KEYSTORE_B64`（`base64 -w0 studytime.keystore`）和 `KS_PASS`
3. **异地备份这把钥匙**（丢了 = 以后所有版本都没法覆盖安装）
4. 切换后手机上的旧包要先卸载一次（证书不同）—— 记得先在 App 里「复制备份到剪贴板」
