# 入墨开发交接

更新于 2026-11-02（北京时间）。正在验证 **0.4.10 / versionCode 14**，线上版本仍为 0.4.9。

## 本次反馈与修改

- 用户小米 17 Pro Max / HyperOS 3.03：键盘已打开时点击 ×，屏幕抖动；放大窗口增加底部空白而非正文；要求保留可选竖条、横条。
- 输入控制器关闭时取消待执行请求、移除监听，随后直接移除编辑窗口，不先 hideSoftInput / 切换窗口焦点。× 区域的 ACTION_OUTSIDE 不提前释放输入；真实点击其他 App、失焦和返回键路径保留。
- 笔记正文在 fillViewport 内使用 weight，剩余高度分配给正文，保持小窗口滚动能力。
- 四种入口：弧形、直竖、直横、球；旧 EDGE / BALL 偏好保持兼容，新条形使用独立位置键，三种条形均可原位 × 收起。
- 新增键盘可见时关闭、× 外部触摸、正文实际尺寸增长和四种样式持久化回归。尚未声称自动化或实机验证通过。

## 发布基础

- 仓库 https://github.com/P-t99/rumo ，下载页 https://rumo.hirkond.online/ ，更新接口 https://rumo.hirkond.online/latest.json 。
- 上版 0.4.9 / code 13，APK SHA-256 `b47f38a32346b4e65a64c26894b5d149c79b54c43d88c0eb56d6107988acba29`。
- 原证书 SHA-256 `227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`；沿用原签名，私钥不进入 VPS 或仓库。
- VPS 静态目录 `/var/www/site-rumo`；0.4.9 发布备份 `/opt/rumo/backups/20261102-0.4.9/`；前次发布证据见 [PUBLISH_0.4.9.md](PUBLISH_0.4.9.md)。
- `/opt/rumo/repo` 是旧工作区，保留不覆盖；当前源码以 GitHub 为准。VPS 已有 Android SDK `/opt/android-sdk`、Gradle `/opt/rumo/gradle-8.13`。

## 远程工具上下文约束

VPS Root 命令输出每路上限 4,000 字符，文件每页最多 8,000 字符；构建日志留文件，后台任务优先仅查状态。不要传 APK、归档或长 base64 进上下文。fs-import 使用本地绝对路径文件引用；多行逻辑用 run-script，不改动网关服务。
