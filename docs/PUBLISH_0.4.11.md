# 0.4.11 线上推送

2026-11-02（北京时间），按用户“帮我直接推送线上更新”的明确指示，将已通过验证的 0.4.11 安装包直接推送默认更新源。没有重新构建或修改 APK，不代表 HyperOS 闪动已获实机修复确认。

| 项目 | 结果 |
| --- | --- |
| 下载页 | https://rumo.hirkond.online/ |
| 更新源 | https://rumo.hirkond.online/latest.json |
| 默认版本 | 0.4.11 / versionCode 15 |
| APK | https://rumo.hirkond.online/releases/Rumo-Flomo-0.4.11.apk |
| 大小 | 2,283,511 字节 |
| SHA-256 | `11675c4a840c69b74b29a30528ba86e559723badc17e23be6909fab5b282eec9` |
| 原证书 SHA-256 | `227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c` |
| 已验证构建源码 | `5f6ce6792d99353c1692ca3a09c2e0c51575a9be` |
| 成功 CI | https://github.com/P-t99/rumo/actions/runs/41385462569 |

复用此前签名及验证证据：14 套件、106 测试全部通过，Lint 无错误；APK 应用内容与成功 CI 逐项相同，v2/v3 及原签名已独立验证。完整构建证据见 [PUBLISH_0.4.11_TEST.md](PUBLISH_0.4.11_TEST.md)。

先将同一 APK 放到标准版本化 URL，匿名 AndroidDownloadManager UA 的 HTTPS 下载返回 HTTP 200、APK MIME、2,283,511 字节和一致的 SHA-256。备份 0.4.10 页面及更新源后，原子替换 index.html 与 latest.json；不带查询参数的公开页面与 JSON 返回 HTTP 200，响应字节与发布文件匹配。接口为 code 15、0.4.11、新 URL 和同一 SHA-256。页面及更新说明均注明输入法闪动仍需小米实机复测。

推送前 0.4.10 的页面与接口备份在 `/opt/rumo/backups/20261102-0.4.11/`。回退时分别把 index.html / latest.json 复制至 `/var/www/site-rumo/` 临时文件，设为 0644 后原子替换。保留新 APK 供已开始的下载完成；回退更新源不会降级已经安装的手机。

用户可在 App 设置 → 版本与更新 → 检查更新中直接升级。已安装同一 0.4.11 测试包的设备无需重复安装；本次推送仍是同一 code 15 和相同字节。
