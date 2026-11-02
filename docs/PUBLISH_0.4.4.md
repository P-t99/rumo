# 0.4.4 应用内更新发布记录

2026-10-22，文字编辑修复版已通过原 Sites 更新通道发布，保留公开访问。

- 下载页：https://rumo-flomo-updates.thd20x.chatgpt.site/
- 更新接口：https://rumo-flomo-updates.thd20x.chatgpt.site/latest.json
- APK：https://rumo-flomo-updates.thd20x.chatgpt.site/releases/Rumo-Flomo-0.4.4.apk
- versionCode 8，Android 8.0+，包名 `com.hirden.flomofloat`。
- 文件大小：2,263,039 字节；SHA-256 `25115b95975bb0d781f71c97895badd8f9907058fb6c6ae53fd1bd3d77def213`。
- 原分发证书：`227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`，已验证，可以覆盖安装保留数据。

## 发布证据

复用项目 `appgprj_6ad83e497adc81919dc33fcaa561fc82` 及其站点源仓库。先发布 APK 并通过公开地址匿名下载核对字节，再切换接口和页面，保留历史 0.3.0 / 0.4.3 APK。

| Sites 版本 | 站点源提交 | 内容 |
| --- | --- | --- |
| 4 | `c51961a1eb0247c34df0f03da641edfaf951d1ce` | 增加 0.4.4 APK，接口保留 0.4.3 |
| 5 | `23d44ee94a0fcf10642b1594aa052ca7e5dd2897` | 接口、页面切换为 0.4.4 |

最终部署 `appgdep_6ad99f7a8de4819189d922e6939d3f1d` 成功。公开无查询参数接口返回 0.4.4 / code 8，模拟 Android HttpURLConnection / DownloadManager 的请求以及下载页均为 HTTP 200。实际下载文件与本地签名包的大小、SHA-256 完全一致；页面的链接、版本、更新源和哈希相符。接口缓存为 `public, max-age=0, must-revalidate`，APK 返回 `application/octet-stream`，应用下载器指定 APK MIME。

69 项自动化测试通过，Lint 0 错误、15 条非阻断警告；具体覆盖见 [BUILD_REPORT.md](../BUILD_REPORT.md)。手机内升级、厂商选区手柄和真实微信仍待实测，见 [TESTING.md](../TESTING.md)。

## 手机内使用与回退

APP 内检查更新、下载、按系统提示覆盖安装，不要先卸载。若仍保存自有域名地址，请将更新源保存为 `https://rumo-flomo-updates.thd20x.chatgpt.site/latest.json` 一次。0.4.4 新构建默认使用原站，已有自定义源继续优先保留。

需要回退版本接口时，在同一 Sites 项目重新部署已保存的版本 4。它恢复 0.4.3 接口与页面，同时保留 0.4.4 APK，避免中断正在下载的请求。回退接口不会降低手机上已经安装的应用版本。
