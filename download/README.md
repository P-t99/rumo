# 下载页与版本接口

下载页：https://rumo.hirkond.online/ 。更新接口：https://rumo.hirkond.online/latest.json 。

2026-11-02 已通过自有 VPS 发布 **0.4.9 / versionCode 13**。此目录保存线上页面和接口的源文件；提交 GitHub 不会自动部署到 VPS。

- `index.html`：无外部脚本、字体或样式依赖的静态下载页。
- `latest.json`：公开版本接口，APK 使用同域 HTTPS 地址。
- `releases/Rumo-Flomo-0.4.9.apk`：2,279,415 字节，APK 被 `.gitignore` 排除，不进入源码仓库。
- SHA-256：`b47f38a32346b4e65a64c26894b5d149c79b54c43d88c0eb56d6107988acba29`。
- 同原分发签名，可覆盖安装；99 项测试通过，Lint 0 错误。真实小米手机行为待确认。

旧版还连原更新站时，首次手动填写新接口或直接覆盖安装；0.4.9 将原默认源解析到自有域名，其他自定义源保留。

## 本地预览

在仓库根目录执行 `python3 -m http.server 4175 --bind 127.0.0.1 --directory download`，打开 `http://127.0.0.1:4175/`。验证下载前，将对应 APK 放在 `download/releases/` 并核对文件名和哈希。

## 维护发布

静态根目录 `/var/www/site-rumo/`。先发布版本化 APK，匿名下载核对字节、哈希和签名；再备份并原子替换页面与接口，保留历史 APK。APK、JSON 与页面须相互匹配。不要部署私钥、API 或草稿。

发布证据及回退步骤见 [PUBLISH_0.4.9.md](../docs/PUBLISH_0.4.9.md)，后续工作见 [HANDOFF.md](../docs/HANDOFF.md)。
