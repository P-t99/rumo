# 0.4.11 实机验证包

2026-11-02（北京时间）。本包恢复贴边竖条并继续处理收起闪动，尚未确认 HyperOS 手机上的问题已解决。

| 项目 | 结果 |
| --- | --- |
| APK | https://rumo.hirkond.online/releases/Rumo-Flomo-0.4.11-test.apk |
| 包名 / 版本 | `com.hirden.flomofloat` / 0.4.11 / code 15 |
| 大小 | 2,283,511 字节 |
| SHA-256 | `11675c4a840c69b74b29a30528ba86e559723badc17e23be6909fab5b282eec9` |
| 原证书 SHA-256 | `227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c` |
| 已验证源码 | `5f6ce6792d99353c1692ca3a09c2e0c51575a9be` |
| CI | https://github.com/P-t99/rumo/actions/runs/41385462569 |
| PR | https://github.com/P-t99/rumo/pull/4 |

14 套件、106 测试均通过，0 失败 / 错误 / 跳过；Lint 无错误，20 警告、1 提示。新增回归验证正常窗口移除时输入框保留到真实摘除、左右边缘竖条的实际像素、旧横条偏好迁移；保留草稿、布局、输入交接及更新协议等原回归。

最终 APK 在本地沿用原密钥签名，非 META-INF 应用内容与成功 CI APK 一致。服务器通过 apksigner 独立验证原证书及 v2 / v3，用 aapt 验证 code 15 / 0.4.11。AndroidDownloadManager UA 的匿名公开下载返回 HTTP 200、APK MIME、2,283,511 字节与匹配 SHA-256。

仅增加上述版本化测试 APK。默认 index.html / latest.json 未切换，接口仍为 0.4.10，更新源字节保持不变。测试安装需直接使用 APK 链接；覆盖安装保留用户数据。用户实机确认后再考虑推送默认源，不能把自动化结果写成手机动画已修复。
