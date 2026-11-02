# 0.4.8 应用内更新发布记录

2026-10-22，修复选词后编辑菜单被白色卡片遮挡的问题，通过原更新通道发布，保留原公开访问。

- 下载页：https://rumo-flomo-updates.thd20x.chatgpt.site/
- 更新接口：https://rumo-flomo-updates.thd20x.chatgpt.site/latest.json
- APK：https://rumo-flomo-updates.thd20x.chatgpt.site/releases/Rumo-Flomo-0.4.8.apk
- versionCode 12，Android 8.0+，包名 `com.hirden.flomofloat`。
- 2,279,423 字节，SHA-256 `73fb2ae6f15627db9e84de2f5f4558263fcec3706ac2e07f8c107fe6dcac5020`。
- 原签名证书 `227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`，可覆盖安装保留数据。

## 发布与校验

复用项目 `appgprj_6ad83e497adc81919dc33fcaa561fc82` 及同一源仓库，先部署 APK 并以匿名 Android DownloadManager 请求核对实际字节与 SHA-256，再切换版本接口和下载页；旧 APK 保留。

| Sites 版本 | 站点源提交 | 内容 |
| --- | --- | --- |
| 11 | `db627d95624764d8ca2486202dad4e95f6032268` | 增加 0.4.8 APK，接口和页面仍为 0.4.7 |
| 12 | `1bf96f381982da2b557efa3b06d0a595baa3818b` | 接口和页面切换为 0.4.8 |

部署 `appgdep_6ad9c876a7dc819186851e5e61cc3c26` 成功。最终无查询参数的公开接口返回 0.4.8 / code 12。模拟 Android HttpURLConnection 的匿名 GET、下载页 GET 和 APK 下载均为 HTTP 200；APK 大小与 SHA-256、页面与版本接口均匹配本地发布文件。接口缓存为 `public, max-age=0, must-revalidate`。

94 项测试通过，Lint 0 错误、17 条非阻断警告和 1 条提示。新增完整窗口实际绘制和实际点击测试，在旧代码上均失败、修改后通过。测试详情见 [BUILD_REPORT.md](../BUILD_REPORT.md)，手机步骤见 [TESTING.md](../TESTING.md)。小米实机仍由用户验证，未声称已经操作该手机。

## 使用与回退

APP 内检查更新并覆盖安装。用户先前开启的常驻栏选项会保留；测试按需菜单时，请关闭“设置 → 文字编辑 → 始终显示编辑工具栏”，再长按正文。没有新增权限。

已保存的自定义更新源继续优先；默认地址为上述原站接口。

需回退版本接口时，可重新部署同一项目已保存的版本 11，恢复 0.4.7 接口与页面，继续保留新版 APK，不中断已经开始的下载。回退接口不会降低手机已安装的版本。
