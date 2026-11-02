# 0.4.6 应用内更新发布记录

2026-10-22，按需文字编辑菜单已通过原 Sites 更新通道上线，保留原公开访问。

- 下载页：https://rumo-flomo-updates.thd20x.chatgpt.site/
- 更新接口：https://rumo-flomo-updates.thd20x.chatgpt.site/latest.json
- APK：https://rumo-flomo-updates.thd20x.chatgpt.site/releases/Rumo-Flomo-0.4.6.apk
- versionCode 10，Android 8.0+，包名 `com.hirden.flomofloat`。
- 2,275,327 字节；SHA-256 `ec980cac1d69e9beb9218d19e79357d9cfd91293fbbd2858ad5f387abb56ebd3`。
- 原签名证书 `227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`，可覆盖安装保留数据。

## 发布与校验

复用项目 `appgprj_6ad83e497adc81919dc33fcaa561fc82` 和原站点源仓库，先发布 APK 并匿名下载核对字节，再切换接口与页面。历史 APK 继续保留。

| Sites 版本 | 站点源提交 | 内容 |
| --- | --- | --- |
| 7 | `0870315ed71bd3eab84a4cc5831687b3efc1df48` | 增加最终 APK，接口与页面保留 0.4.4 |
| 8 | `3a86771d7627d384006eea0082bf07e16388fd39` | 接口、页面切换为 0.4.6 |

部署 `appgdep_6ad9b31a104c81919e8d0a3efee78013` 成功。公开无查询参数接口返回 0.4.6 / code 10，下载页及模拟 Android HttpURLConnection / DownloadManager 请求均返回 HTTP 200。实际下载的 SHA-256、大小与本地签名包完全一致，页面链接、版本、更新源和哈希匹配。接口缓存为 `public, max-age=0, must-revalidate`；APK 返回 `application/octet-stream`，应用下载器指定 APK MIME。

83 项测试通过，Lint 0 错误、17 条非阻断警告与 1 条提示；覆盖见 [BUILD_REPORT.md](../BUILD_REPORT.md)。厂商选区手柄、系统剪贴板、输入法及微信仍待实机验证，见 [TESTING.md](../TESTING.md)。

## 使用与回退

APP 内检查更新并覆盖安装，不要先卸载。默认长按正文选词或粘贴；“文字编辑 → 始终显示编辑工具栏”提供常驻兼容模式，默认关闭，选择自动保存。

默认更新源为原站地址，用户保存的自定义源继续优先；若仍使用自有域名，请保存 `https://rumo-flomo-updates.thd20x.chatgpt.site/latest.json` 一次。

需要回退更新接口时，在同一 Sites 项目重新部署已保存的版本 7，恢复 0.4.4 接口和页面，同时保留最终 APK，避免中断已有下载。回退接口不会降低手机上已经安装的版本。
