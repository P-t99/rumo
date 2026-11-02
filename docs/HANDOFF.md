# 入墨开发交接

更新于 2026-11-02（北京时间）。当前发布版 **0.4.9 / versionCode 13**，包名 `com.hirden.flomofloat`，Android 8.0+。

## 当前结果

- 仓库 https://github.com/P-t99/rumo ，本次功能 PR #2；功能与成功构建源提交 `0405b4eca0ca8f64db507890b9398391fceac484`。
- 弧形贴条支持左右镜像；打开笔记或设置时原位显示 ×；点击保存草稿并收起。原手势与按钮继续可用。
- `UpdateChannel.resolve` 将空白或原默认更新源迁移至自有域名，其他自定义源保持原样。
- 下载页 https://rumo.hirkond.online/ ，更新接口 https://rumo.hirkond.online/latest.json 。
- APK https://rumo.hirkond.online/releases/Rumo-Flomo-0.4.9.apk ，2,279,415 字节，SHA-256 `b47f38a32346b4e65a64c26894b5d149c79b54c43d88c0eb56d6107988acba29`。
- 原证书 SHA-256 `227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`；apksigner 验证 v2 / v3 成功。私钥不进入 VPS 或仓库。

## 验证与待确认

GitHub Actions [41290020834](https://github.com/P-t99/rumo/actions/runs/41290020834) 成功，14 套件 / 99 测试，0 失败、错误或跳过；Lint 0 错误、19 警告、1 提示。最终同签名 APK 与 CI APK 的非 META-INF 条目逐项字节一致。主要回归为实际触摸、入口位置和草稿保留、窗口层级、设置与模式切换、无障碍点击及原生绘制。

尚未连接用户的小米 17 Pro Max / HyperOS 3.03。下一步先收集真实安装、弧形入口、原位 ×、键盘焦点交接和长按菜单的手机反馈；不要把自动化结果写成手机实测结果。无需先重做已经通过的构建。

## 发布位置与恢复

- VPS 静态目录 `/var/www/site-rumo`；发布备份 `/opt/rumo/backups/20261102-0.4.9/`。
- `/opt/rumo/repo` 是前次恢复的旧基线及未提交补丁，不是最新权威源码；继续开发从 GitHub 最新分支或主干检出，保留该旧目录。
- 原站仍供旧版本使用；仅本次自有域名已更新。旧版若检查不到 0.4.9，直接覆盖安装或手动保存新源一次。0.4.9 起原默认源自动兼容。
- 完整发布证据与回退见 [PUBLISH_0.4.9.md](PUBLISH_0.4.9.md)。回退接口不会降低手机已安装的版本。

## 远程工具上下文约束

VPS Root 已恢复，命令输出每路上限 4,000 字符，文件每页最多 8,000 字符。构建日志留在服务器文件，后台任务优先仅查状态；不要把 APK、归档或长 base64 放进调用。`fs-import` 可将 `file` 设置为当前本地 APK 的绝对路径，由运行时转换为授权文件引用；多行逻辑用 `run-script`。不要为上传安装包改动网关服务。
