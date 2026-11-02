# 入墨开发交接

更新于 2026-11-02（北京时间）。最新代码与实机验证包为 **0.4.11 / versionCode 15**；默认下载页及更新源仍为 **0.4.10 / code 14**，暂未向默认源推送验证包。

## 用户反馈与当前结论

- 用户明确不需要横条；希望恢复旧版紧贴屏幕边缘的竖条。已移除横条选项，保留弧形悬浮条、贴边竖条、悬浮球三种。
- 用户确认收起时仍闪动：× 和底部“收起”均会；打开入墨前原页面有键盘、无键盘都出现。上一版自动测试没有验证出 HyperOS 实机修复，不得宣称问题已解决。
- 0.4.11 继续调整共用窗口拆除时序：SystemHost 用普通 removeView 替代 removeViewImmediate；FxWindowContainer.releaseContent 在实际窗口摘除之前保留输入框，避免先销毁 IME served view。关闭时仍取消旧键盘请求、卸除监听；外部触摸、返回键及正文缩放继续保留。这是待实机确认的修复候选。
- 贴边竖条恢复旧版 6 × 48 dp 圆角细条，左右紧贴边缘，保留透明触摸区域和原位 ×。绘制使用宿主最新坐标，避免窗口帧坐标滞后导致左右侧画错。已保存 HORIZONTAL 自动解析为 VERTICAL，透明度及其他数据保留。

## 已交付与验证

- 仓库 https://github.com/P-t99/rumo ，本次 PR #4；成功验证源码 `5f6ce6792d99353c1692ca3a09c2e0c51575a9be`。
- 测试 APK https://rumo.hirkond.online/releases/Rumo-Flomo-0.4.11-test.apk ，2,283,511 字节。
- APK SHA-256 `11675c4a840c69b74b29a30528ba86e559723badc17e23be6909fab5b282eec9`。
- 原证书 SHA-256 `227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`；本地签名，私钥未进入 VPS 或仓库。同签名覆盖安装保留数据。
- [CI 41385462569](https://github.com/P-t99/rumo/actions/runs/41385462569) 成功：14 套件、106 测试、0 失败 / 错误 / 跳过，Lint 0 错误、20 警告、1 提示。应用内容与成功 CI APK 逐项一致；服务器独立验证 v2/v3、原证书、包名、版本及 APK 公开 GET 的大小 / SHA / MIME / HTTP 200。
- 详细证据见 [PUBLISH_0.4.11_TEST.md](PUBLISH_0.4.11_TEST.md)。默认 `/latest.json` 字节未改，仍指向 0.4.10；请从测试 APK 链接覆盖安装。

## 下一步

等待小米 17 Pro Max / HyperOS 3.03 的 0.4.11 实机复测：桌面无键盘及微信已有键盘两种场景，分别使用底部收起和 ×；检查闪动、键盘残留及外部继续输入。若仍闪，不要再次把单元测试通过当作实机修复，应进一步获取录屏或非内容型窗口 / IME 时序日志。用户确认表现后，可复用这个已签名 APK 正式推送，无需无故重做构建。

## 发布与恢复位置

- 默认页 https://rumo.hirkond.online/ ，默认更新源 https://rumo.hirkond.online/latest.json ，服务器静态目录 `/var/www/site-rumo`。
- 0.4.10 发布前备份 `/opt/rumo/backups/20261102-0.4.10/`；本次只增加版本化测试 APK，没有改动默认页 / 接口，无需回退源。
- `/opt/rumo/repo` 是旧工作区，保留不覆盖，权威源码以 GitHub 为准。SDK `/opt/android-sdk`，Gradle `/opt/rumo/gradle-8.13`。
- VPS Root 输出每路最多 4,000 字符、文件每页 8,000 字符；日志留文件，后台状态优先。勿传 APK、归档或长 base64 进上下文；fs-import 的 file 可用本地绝对路径；多行用 run-script 的 interpreter / content。不要改动网关服务。
