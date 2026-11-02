# 入墨开发交接

更新于 2026-11-02（北京时间）。正在验证 **0.4.11 / versionCode 15**，线上仍为 0.4.10。

本次用户澄清：不需要横条，直竖条应恢复旧版贴边外观。已移除横条枚举与选择项，三种入口为弧形、贴边竖条、球；竖条恢复 6 × 48 dp 的边缘圆角条，保留透明点击区域和 ×。已保存的 HORIZONTAL 自动解析为 VERTICAL；保留透明度及其他数据。测试和发布待完成。

## 当前结果

- 仓库 https://github.com/P-t99/rumo ，本次 PR #3；成功构建源码 `52950766d00abe832f8200be76a7f44e57419197`。
- 修复 × 收起时主动隐藏 IME 并改变窗口焦点、随后下层输入重新激活键盘的闪动路径。关闭时先取消过期请求、卸监听，直接移除窗口；× 区域的 ACTION_OUTSIDE 不提前释放输入，窗口保留 SOFT_INPUT_STATE_UNCHANGED。真实外部触摸、失焦和返回键释放行为保留。
- 放大笔记窗口时，fillViewport 内的正文权重分配新增高度；标签、提示与按钮保持紧凑，小窗口可滚动，光标和草稿保留。
- 设置 → 悬浮入口：弧形、直竖、直横、球四种选择，自动保存并分别记忆位置。三种条形展开均原位 × 收起；旧 EDGE / BALL 偏好和位置键兼容。
- 下载页 https://rumo.hirkond.online/ ，更新源 https://rumo.hirkond.online/latest.json 。
- APK https://rumo.hirkond.online/releases/Rumo-Flomo-0.4.10.apk ，2,283,511 字节，SHA-256 `414d4e827e0cac5a5681992606d83f42a573cc17889623180d77ee10abae3b19`。
- 原证书 SHA-256 `227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`，v2 / v3 独立验证成功；私钥未进入 VPS 或仓库。同签名覆盖安装保留数据。

## 验证与下一步

[CI 41383868123](https://github.com/P-t99/rumo/actions/runs/41383868123) 成功，14 套件 / 103 测试，0 失败、错误或跳过；Lint 0 错误、20 警告、1 提示。最终签名 APK 的应用内容与 CI APK 字节一致。公开 APK、下载页与更新源 HTTP 200，版本、大小、MIME、哈希和证书匹配。详细证据和回退见 [PUBLISH_0.4.10.md](PUBLISH_0.4.10.md)。

用户设备是小米 17 Pro Max / HyperOS 3.03；目前尚未直接连接。下一步收集实际安装后的反馈，重点为已打开输入法时 × 是否仍抖动、无下层输入框时是否正常收键盘、放大后正文新增区域可编辑，以及四种样式与长按菜单。不要将自动化结果写成小米实测；通过的构建无需无故重做。

## 发布位置与恢复

- VPS 静态目录 `/var/www/site-rumo`；本次发布前 0.4.9 页面与接口备份 `/opt/rumo/backups/20261102-0.4.10/`，回退说明见本次发布证据。
- `/opt/rumo/repo` 是旧工作区，保留不覆盖；继续开发从 GitHub 最新主干检出。VPS 工具：SDK `/opt/android-sdk`，Gradle `/opt/rumo/gradle-8.13`。
- 0.4.9 起空白及原默认源自动迁移至自有域名；其他自定义源保留。README 已修正过期的原站更新说明。原站仍供旧版本使用，本次仅更新自有域名。

## 远程工具上下文约束

VPS Root 命令每路最多 4,000 字符、文件每页最多 8,000 字符。构建日志留文件，后台任务优先仅查状态，不要传 APK、归档或长 base64 进上下文。fs-import 可把 file 设为当前本地绝对路径；多行用 run-script（interpreter: python3，content: 脚本文本），不要改动网关服务。
