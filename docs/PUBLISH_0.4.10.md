# 0.4.10 发布证据

2026-11-02（北京时间），发布键盘交接、正文缩放与可选直条样式更新。

| 项目 | 结果 |
| --- | --- |
| 下载页 | https://rumo.hirkond.online/ |
| 更新源 | https://rumo.hirkond.online/latest.json |
| APK | https://rumo.hirkond.online/releases/Rumo-Flomo-0.4.10.apk |
| 包名 / 版本 | `com.hirden.flomofloat` / 0.4.10 / code 14 |
| SDK | minSdk 26 / targetSdk 35 / compileSdk 36 |
| APK 字节数 | 2,283,511 |
| APK SHA-256 | `414d4e827e0cac5a5681992606d83f42a573cc17889623180d77ee10abae3b19` |
| 证书 SHA-256 | `227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c` |
| 已验证源码 | `52950766d00abe832f8200be76a7f44e57419197` |
| PR | https://github.com/P-t99/rumo/pull/3 |
| 成功 CI | https://github.com/P-t99/rumo/actions/runs/41383868123 |

## 自动化与分发检查

14 个测试套件，103 项测试，0 失败、错误或跳过；Lint 0 错误、20 警告、1 提示。检查覆盖：关闭拥有焦点的输入框时无主动隐藏 / 再显示键盘，× 的窗外触摸不提前释放焦点，正文随窗口增高、缩小可恢复，四种样式可选择并保存，以及原菜单、草稿、布局、输入交接和更新协议回归。

最终签名 APK 与成功 CI APK 的所有非 META-INF 文件内容逐项相同。签名在本地完成，私钥未发送到 VPS 或仓库；VPS 用 `apksigner verify --verbose --print-certs` 独立验证 v2 / v3 和原证书，用 `aapt dump badging` 验证包名、版本及 SDK。

先发布版本化 APK，通过匿名 AndroidDownloadManager UA 的 HTTPS GET 验证 HTTP 200、APK MIME、2,283,511 字节和完整 SHA-256；再备份并原子替换下载页及更新源。无查询参数的公开页面和 JSON 均 HTTP 200，响应字节与已发布文件相同，版本及哈希匹配。

HyperOS 实机尚未直接连接；自动化不代表厂商输入法动画已获实机确认。请重点复测：微信输入法已打开时展开入墨再点 ×、桌面未编辑时点 × 正常收键盘、正文窗口放大后新增区域可输入、四种入口切换与位置记忆。

## 回退

发布前 0.4.9 页面和更新源位于 `/opt/rumo/backups/20261102-0.4.10/`。静态目录 `/var/www/site-rumo/`，版本化 APK 保留在 `releases/`。

需要回退时，把该备份中的 `index.html` 和 `latest.json` 分别复制到静态目录临时文件，设置 0644 权限后原子替换。保留 0.4.10 APK，避免打断已开始的下载；恢复更新源只恢复向尚未升级设备发布的版本，不会降级已安装的手机。

覆盖安装原分发版本可保留数据；不要先卸载。应用内更新源继续是 `https://rumo.hirkond.online/latest.json`。
