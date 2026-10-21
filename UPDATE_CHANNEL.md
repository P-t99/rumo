# 发布与更新

下载页：https://rumo.hirkond.online

版本接口：https://rumo.hirkond.online/latest.json

下载页、版本接口和 APK 均由自有域名直接提供。本次域名迁移保留原 0.3.0 安装包、包名、签名和哈希。

已安装的 0.2.0 / 0.3.0 用户需在“设置 → 版本与更新”中，将更新源保存为上方的新地址。原始 0.3.0 安装包内置旧站地址；本仓库后续构建默认使用新域名。无需为了切换更新源卸载或重新安装应用。

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
