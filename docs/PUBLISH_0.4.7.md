# 0.4.7 应用内更新发布记录

2026-10-22，长按触摸兼容修复通过原更新站发布，保留现有公开访问。

- 下载页：https://rumo-flomo-updates.thd20x.chatgpt.site/
- 更新接口：https://rumo-flomo-updates.thd20x.chatgpt.site/latest.json
- APK：https://rumo-flomo-updates.thd20x.chatgpt.site/releases/Rumo-Flomo-0.4.7.apk
- versionCode 11，Android 8.0+，包名 `com.hirden.flomofloat`。
- 2,279,423 字节，SHA-256 `3eae5806b830f2b6e022b0f118dc46fa2b196f9c0868acf9648cbd65b6389c30`。
- 原分发签名 `227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`，可覆盖安装保留数据。

## 发布与校验

复用项目 `appgprj_6ad83e497adc81919dc33fcaa561fc82` 和同一源仓库。先部署 APK，再以匿名 Android DownloadManager 请求下载实际字节，核对大小、SHA-256 与签名测试包一致；随后切换版本接口和页面，旧 APK 保留。

| Sites 版本 | 站点源提交 | 内容 |
| --- | --- | --- |
| 9 | `4c7856d8c5f6922bbe5dc65eb37a9ae8b7890af3` | 新增 0.4.7 APK，保留 0.4.6 接口和页面 |
| 10 | `2324a0fc78548cff4fa49943000b6e5ca59b211a` | 接口、下载页切换为 0.4.7 |

部署 `appgdep_6ad9c39ae5a081918e094da23e994eaa` 成功。最终公开无查询参数接口返回 0.4.7 / code 11，模拟 Android HttpURLConnection 及下载页匿名 GET 均为 HTTP 200；APK 匿名 GET 已验证 HTTP 200 和实际哈希。接口与页面、下载链接、APK 哈希匹配本地文件。缓存为 `public, max-age=0, must-revalidate`。

92 项测试通过，Lint 0 错误、17 条非阻断警告与 1 条提示，见 [BUILD_REPORT.md](../BUILD_REPORT.md)。新测试从真实触摸事件流触发，不直接调用长按；实际手机厂商手柄、放大镜、输入法和微信仍待实测，见 [TESTING.md](../TESTING.md)。

## 使用与回退

APP 内检查更新并直接覆盖安装，不要先卸载。默认按需显示文字编辑菜单；设置 → 文字编辑 → 始终显示编辑工具栏提供常驻兼容入口，默认关闭、选择自动保存。

用户保存的自定义更新源仍优先。如果仍指向旧自有域名，可手动保存上述原站接口地址。

需回退版本接口时，重新部署同一项目已保存的版本 9，恢复 0.4.6 接口与页面，继续保留新版 APK，不中断已经开始的下载。回退接口不会降低手机上已安装的版本。
