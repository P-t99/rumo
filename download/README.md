# 下载页与版本接口

公开入口：[rumo.hirkond.online](https://rumo.hirkond.online/)。

- `index.html`：无外部脚本、字体或样式依赖的静态下载页。
- `latest.json`：应用更新接口，APK 地址同样使用自有域名。
- `releases/`：放置实际分发的安装包；APK 被 `.gitignore` 排除，不随源码提交。

本目录已准备 **0.4.2 / versionCode 6** 发布候选文件；实际线上版本以公开 `latest.json` 为准。提交 GitHub 不会自动部署自有域名服务器。

- APK：`releases/Rumo-Flomo-0.4.2.apk`，2,250,751 字节。
- SHA-256：`8bb89d8ed300379cbbaa698c8f92ab9bf87d16252d9287226765f920a952a8a2`。
- 与原分发测试证书一致，可覆盖安装并保留数据。
- 53 项测试通过，Lint 0 错误；真实微信与厂商输入法待手机验证。

已有 0.2.0 / 原始 0.3.0 用户如仍使用旧更新源，应保存 `https://rumo.hirkond.online/latest.json`。后续构建默认使用此地址。

## 本地预览

在仓库根目录执行：

```bash
uv run --python 3.13 python -m http.server 4175 --bind 127.0.0.1 --directory download
```

打开 `http://127.0.0.1:4175/`。验证下载前，需将对应 APK 放在 `download/releases/`，并核对 `latest.json` 的文件名和哈希。

## 发布

站点可由 nginx 等普通静态服务器提供。发布完整的 `download/` 内容（包含已校验的 APK），启用 HTTPS，并保留 `/latest.json` 与版本化 `/releases/` 路径。不要把请求转发到旧下载站。

新版本先发布 APK 并核对实际下载哈希，再更新 `latest.json` 与页面内容。保持历史 APK 文件名不变；版本接口建议不缓存，以便及时获取最新版本。

维护者实际部署目录、备份和回退命令记录在本机忽略的 `.deploy/` 中。0.4.2 发布交接见 [PUBLISH_0.4.2.md](../docs/PUBLISH_0.4.2.md)。
