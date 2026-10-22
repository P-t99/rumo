# 入墨 Rumo

**随时记录，回到 flomo。**

我做了一个安卓悬浮速记工具，希望在使用其他 APP 时，也能随手写下几句话，直接存入自己的 flomo。记录入口留在屏幕边缘，整理和回顾继续在 flomo 完成。

Rumo is an Android floating quick-note app that sends text and tags directly to your personal flomo incoming webhook.

- 作者：[Hirden Tao / P-t99](https://github.com/P-t99)
- 当前源码版本：**0.4.7 测试版**，支持 Android 8.0 及以上；下载页和应用内更新接口已发布 0.4.7
- 许可：[MIT](LICENSE)
- [下载入墨](https://rumo-flomo-updates.thd20x.chatgpt.site) · [手机验证](TESTING.md) · [版本记录](CHANGELOG.md)

<img src="docs/images/features.png" alt="入墨功能示意图" width="560" />

*功能插画，实际界面以安装后的应用为准。*

## 功能

| 功能 | 行为 |
| --- | --- |
| 悬浮速记 | 在其他 APP 上方记录文字与标签，一键存入 flomo |
| 文字编辑 | 长按选词后显示菜单，长按光标处粘贴；常驻工具栏为可选兼容模式，默认关闭 |
| 两种入口 | 贴边竖条 / 悬浮球，分别记住位置 |
| 入口透明度 | 0–80%，即时保存；编辑窗口保持清晰 |
| 快速收起 | 按钮、轻左滑、双击标题或空白；正文保留双击选词 |
| 标签记忆 | 三个快捷标签可改，默认标签自动填入新笔记 |
| 本地草稿 | 自动保存，发送失败保留；成功后清除对应草稿 |
| 悬浮设置 | 在原窗口修改设置，返回继续编辑 |
| 窗口布局 | 顶部圆点拖动位置、右下角拖动缩放，笔记 / 设置与横竖屏分别记忆 |
| 稳定布局 | 默认窗口位于上方，为底部键盘预留空间；已经放得下的布局在键盘开合时保持不动，放不下才临时调整 |
| 外观主题 | 浅色、深色、跟随系统三种模式，即时生效并保存；切换不重建编辑窗口 |
| 输入焦点 | 展开笔记自动聚焦正文并弹出键盘，点击窗外释放焦点；再点正文即可继续记笔记 |
| 重置布局 | 标题栏“重置”或设置中的“重置窗口布局”，恢复尺寸和位置，保留草稿、API 和标签 |
| 应用内更新 | 检查、下载和系统确认安装；复用任务和已校验文件 |

当前支持文字和标签，暂未提供图片、录音、历史笔记查询或离线自动补发。

## 安装使用

1. 从[下载页](https://rumo-flomo-updates.thd20x.chatgpt.site)下载安装包。
2. 从 [flomo API 页面](https://flomoapp.com/mine?source=incoming_webhook)复制个人 incoming webhook。官方 API 需要相应会员权限，详见[官方说明](https://help.flomoapp.com/advance/api.html)。
3. 在入墨中粘贴地址并保存，点击“开始悬浮速记”，授权系统悬浮窗。
4. 返回正在使用的 APP，点击竖条或悬浮球，写下内容并点击“存入 flomo”。

已有维护者分发的测试版时，直接覆盖安装同一渠道的新版本可保留配置和草稿，不要先卸载。部分手机还需允许后台运行。

更新通道现使用原站地址 `https://rumo-flomo-updates.thd20x.chatgpt.site/latest.json`。如 APP 内仍填有自有域名，请在“设置 → 版本与更新”保存此地址一次，再检查更新；已使用原站地址的用户无需修改。0.4.4 起内置默认更新源恢复为原站；用户已保存的自定义源优先，覆盖安装保留该设置。

展开后拖动左上方圆点可移动窗口，拖动右下角斜线可改变宽高，松手后自动保存。输入法避让不会覆盖记住的大小；浮动 / 分离式输入法可能不提供占用区域信息，可手动移动或缩放窗口。点击标题栏“重置”会清除笔记、设置两类窗口的横竖屏布局记忆。

在正文长按选词并拖动选区，在附近出现的菜单中全选、复制、剪切或粘贴；空白正文或光标处长按可粘贴。默认没有常驻工具栏，不占正文高度。主界面或悬浮设置中的“文字编辑 → 始终显示编辑工具栏”可开启兼容模式，选择自动保存。API 遮挡时需先勾选“显示地址”才能复制或剪切。

在主界面或悬浮设置的“外观主题”中选择浅色、深色或跟随系统。默认跟随系统；主界面、悬浮笔记、设置和入口一起适配。切换保留正在编辑的内容、光标、窗口布局和输入焦点。

0.4.2 展开笔记后自动聚焦正文并弹出输入法，有草稿时光标位于末尾；点到窗外会释放输入焦点，窗口和草稿保留。新默认窗口更紧凑并位于上方，通常无需随键盘开合调整。旧版手动保存的布局会保留，点击一次“重置”可应用新默认布局。

## 数据与网络

API 使用 Android Keystore 密钥进行 AES-GCM 加密后保存在应用私有空间。草稿、标签、窗口位置和显示偏好留在本机，卸载或清除数据会移除这些信息。

手机通过 HTTPS 直接连接个人 flomo webhook，没有额外的笔记中转服务器。仅接受官方 flomo incoming webhook；HTTP 成功且响应业务代码为 `0` 后才确认保存。请求超时或连接中断时，先在 flomo 确认是否已收到，再决定重试，以免重复记录。

更新页只提供安装包和版本信息，不接收笔记或 API。

## 构建

需要 JDK 17、Android SDK Platform 36、Build Tools 35.0.0。构建工具为 Gradle 8.13、AGP 8.13.2、Kotlin 2.2.21。

```bash
git clone https://github.com/P-t99/rumo.git
cd rumo
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

可用 Android Studio 打开项目；也可设置本机 `ANDROID_HOME`，或在不提交的 `local.properties` 中填写 `sdk.dir`。

调试包位于 `app/build/outputs/apk/debug/app-debug.apk`。源码使用开发者本机默认调试签名，**与下载页维护者签名不同，不能覆盖安装后者**。维护者私钥不随源码分发。发布签名方式见 [UPDATE_CHANNEL.md](UPDATE_CHANNEL.md)。

## 实现与验证

- `app/`：Kotlin / Android View 实现编辑、设置、草稿、发送与更新。
- `vendor/floatingx-core/`、`vendor/floatingx-system/`：FloatingX 悬浮窗模块。
- `CaptureService`：前台服务和窗口；`LocalStore`：凭据、草稿与偏好。
- `FlomoClient`、`NoteProtocol`：请求与结果；`SwipeLayout`：手势。
- `SettingsContent`：共用设置；`AppUpdates`：下载、校验和安装。

我通过与 AI 协作完成调研、实现和构建，再根据实机试用反馈修改交互。0.4.7 源码完成构建，92 项自动化测试全部通过，Lint 0 错误，APK 编译与原测试证书签名验证通过。Robolectric 测试不能代替不同手机的悬浮权限、后台策略、Keystore 和系统安装器验证。参见 [构建记录](BUILD_REPORT.md)、[架构说明](docs/ARCHITECTURE.md)。

欢迎反馈手机型号、系统版本和复现步骤，截图请隐藏 API 与私人笔记。参与开发见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 来源与致谢

入墨采用 [Petterpx/FloatingX](https://github.com/Petterpx/FloatingX) **3.0.0** 的 core 和 system 模块。上游主源码保留，Gradle 构建文件按本应用简化。[来源记录](vendor/UPSTREAM.md) · [上游 MIT 许可](vendor/FloatingX-LICENSE)。

笔记写入使用 [flomo 官方记录 API](https://help.flomoapp.com/advance/api.html)。入墨是独立的第三方工具，与 flomo 官方分别维护。

本项目按 [MIT License](LICENSE) 发布，第三方组件保留原作者声明，详见 [NOTICE](NOTICE)。
