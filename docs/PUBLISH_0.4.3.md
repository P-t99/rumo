# 0.4.3 应用内更新发布记录

2026-10-22，按维护者要求复用原 Sites 更新站，0.4.3 已上线。

- 下载页：https://rumo-flomo-updates.thd20x.chatgpt.site/
- 版本接口：https://rumo-flomo-updates.thd20x.chatgpt.site/latest.json
- 安装包：https://rumo-flomo-updates.thd20x.chatgpt.site/releases/Rumo-Flomo-0.4.3.apk
- versionCode `7`，minSdk `26`，包名 `com.hirden.flomofloat`。
- APK：2,258,943 字节，SHA-256 `74f86f1fc3deb425d2c1561c5e7269a50cb285c6b6950ff390ace028be2e9793`。
- 签名证书 SHA-256：`227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`，与原测试渠道一致。

## 发布和验证

复用项目 `appgprj_6ad83e497adc81919dc33fcaa561fc82`，保持公开访问。先发布 APK，匿名下载并用 SHA-256、文件大小与 apksigner 验证；然后将接口和下载页切换为 0.4.3。原 0.3.0 APK 保留。

| Sites 版本 | 站点源提交 | 内容 |
| --- | --- | --- |
| 1 | `64ad42b268878b6ef2609a908e3e60743465e3c2` | 原 0.3.0 |
| 2 | `8064b0a520e602ad4665976e6ca85b0ff5198b7d` | 增加 0.4.3 APK，接口仍为 0.3.0 |
| 3 | `6e911a0ce6ff0b03cf4b16c9e3d2bf7e35785b91` | 接口与下载页切换为 0.4.3 |

最终部署 `appgdep_6ad953041cb08191b16eb8e551a31242` 成功。公开无查询参数接口返回 0.4.3 / code 7；使用 Android HttpURLConnection 和 DownloadManager 的 User-Agent 分别请求接口与 APK，均为 HTTP 200，匿名下载的实际 APK 大小及哈希一致。下载页的安装包链接、更新源和 SHA-256 与接口相符。接口响应为 `application/json`、`Cache-Control: public, max-age=0, must-revalidate`；APK 返回 `application/octet-stream`，应用下载器按 APK MIME 处理。

未进行真实手机安装、微信或厂商输入法验证。61 项自动化测试和 Lint 结果见 [BUILD_REPORT.md](../BUILD_REPORT.md)。

## 已安装 APP 的更新源

如 APP 内仍保存自有域名，请在“设置 → 版本与更新”中保存 `https://rumo-flomo-updates.thd20x.chatgpt.site/latest.json` 一次，再检查更新。原站地址未变的用户直接检查即可。低于 code 7 的版本会提示更新；已安装 0.4.3 时显示已是最新版本。覆盖安装保留 API、草稿、标签和已保存的更新源，不要先卸载。

0.4.3 内置默认值仍是构建时的 `https://rumo.hirkond.online/latest.json`；本次只更新分发通道，未重打包或变更同名 APK。已保存的更新源覆盖该默认值。此前准备的自有域名 ZIP 不等于当前站点快照，请勿直接用于回填此站。

## 回退

需要回退更新接口时，可在原 Sites 项目重新部署已保存的版本 2：它恢复原 0.3.0 页面和接口，同时保留 0.4.3 APK，避免中断已有下载。回退接口不会降级手机上已安装的 APP。后续发布继续复用同一项目、站点源仓库与公开访问配置，不要创建重复站点。
