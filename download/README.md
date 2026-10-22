# 下载页与版本接口

公开入口：[https://rumo-flomo-updates.thd20x.chatgpt.site](https://rumo-flomo-updates.thd20x.chatgpt.site/)。更新接口：https://rumo-flomo-updates.thd20x.chatgpt.site/latest.json。

2026-10-22 已通过现有 Sites 项目发布 **0.4.3 / versionCode 7**，保留公开访问与原地址。此目录与线上下载页、版本接口一致；提交 Android 源码到 GitHub 不会自动部署站点。

- `index.html`：无外部脚本、字体或样式依赖的静态下载页。
- `latest.json`：公开版本接口，APK 地址使用同一原站域名。
- `releases/Rumo-Flomo-0.4.3.apk`：2,258,943 字节，APK 被 `.gitignore` 排除，不随 Android 源码提交。
- SHA-256：`74f86f1fc3deb425d2c1561c5e7269a50cb285c6b6950ff390ace028be2e9793`。
- 与原分发测试证书一致，可覆盖安装；61 项测试通过，Lint 0 错误。真实微信、厂商输入法与手机内安装待验证。

若 APP 内仍使用自有域名，请保存上述原站更新地址一次。0.4.3 的内置默认值仍是自有域名；已保存的更新源优先，覆盖安装保留。本次发布未重新构建 APK。

## 本地预览

在仓库根目录执行 `python3 -m http.server 4175 --bind 127.0.0.1 --directory download`，打开 `http://127.0.0.1:4175/`。验证下载前，将对应 APK 放在 `download/releases/` 并核对文件名和哈希。

## 维护发布

复用原 Sites 项目及其独立源仓库，保留当前公开访问。先发布 APK 并核对匿名下载的实际哈希，再更新 `latest.json` 和下载页。保留历史 APK。接口当前缓存响应为 `public, max-age=0, must-revalidate`，APP 请求也要求重新验证。

发布版本、源提交和回退信息见 [PUBLISH_0.4.3.md](../docs/PUBLISH_0.4.3.md)。此前的自有域名部署包仍引用旧域名，不要用它直接覆盖当前更新站。
