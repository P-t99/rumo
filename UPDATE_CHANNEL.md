# 发布与更新

下载页：https://rumo.hirkond.online/

版本接口：https://rumo.hirkond.online/latest.json

2026-11-02 已在自有 VPS 上线 0.4.9 / versionCode 13。先发布版本化 APK 并验证匿名 HTTPS 下载，再切换版本接口和下载页；原 0.3.0 安装包继续保留。发布证据与回退步骤见 [PUBLISH_0.4.9.md](docs/PUBLISH_0.4.9.md)。

0.4.9 将空白更新源及原默认地址 `https://rumo-flomo-updates.thd20x.chatgpt.site/latest.json` 解析至上方自有域名，其他自定义源保持原样。尚未升级且仍使用原站的旧版，可直接下载安装 0.4.9，或在“设置 → 版本与更新”中将更新源保存为上方地址一次。不要先卸载，以保留 API、草稿和标签。

下载页源码和版本信息位于 [`download/`](download/README.md)，部署说明见该目录的 README。

## 手机内更新

保留旧版，在设置中检查、下载更新，再按 Android 系统提示确认覆盖安装。之后确认版本、API、草稿和标签保留。取消安装后复用已校验文件，下载中重开会恢复查询系统任务。在线版本不高于已安装版本时显示“当前已是最新版本”。

## 自行构建与签名

`assembleDebug` 使用本机默认 Android 调试签名，不需要维护者私钥；它不能覆盖签名不同的下载页版本。试验自有更新时可在应用内配置自有 HTTPS 版本源。

`assembleRelease` 默认产生未签名包。维护者在本机环境或 CI Secrets 中提供以下变量后启用 release 签名：

```text
RUMO_SIGNING_STORE
RUMO_SIGNING_STORE_PASSWORD
RUMO_SIGNING_KEY_ALIAS
RUMO_SIGNING_KEY_PASSWORD
```

第一个变量是仓库之外的密钥文件绝对路径，其余分别为存储密码、别名和密钥密码。实际值不写入源码和日志。

## 后续版本

递增 `versionCode` 与 `versionName`，更新 CHANGELOG，完成检查并用与现有分发渠道一致的签名构建。上传版本化 APK，计算实际 SHA-256，最后更新固定版本接口。

接口字段为 `versionCode`、`versionName`、`apkUrl`、`sha256`、`notes`、`minSdk`。APK 和版本接口均需 HTTPS。应用校验哈希、包名、版本和签名后才允许安装。

签名密钥由维护者私下保管；更新页不放 API、草稿或签名文件。
