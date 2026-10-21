# 技术分工

入墨使用 Kotlin 和原生 Android View。`CaptureService` 作为前台服务管理悬浮入口与编辑窗口，FloatingX 负责系统窗口承载、拖动、吸附和位置保存。

`LocalStore` 保存本地草稿与设置，API 地址单独使用 Keystore 密钥进行 AES-GCM 加密。`SendManager` 在后台发送草稿快照；成功后仅清除与快照仍相同的草稿，避免覆盖发送期间的新编辑。

`FlomoClient` 经 HTTPS 直连个人 flomo webhook，`NoteProtocol` 处理正文、标签和业务结果。`SettingsContent` 供主界面和悬浮设置共用，`SwipeLayout` 处理收起手势并保留输入框与滑块操作。

`AppUpdates` 读取 HTTPS 版本信息，使用 Android DownloadManager 下载，校验哈希、包名、版本和签名，再交给系统安装器确认。

FloatingX 3.0.0 的 core 与 system 模块放在 vendor，保留上游源码和许可，构建文件按应用简化。自动测试采用 Robolectric，系统服务和厂商后台策略仍需手机验证。
