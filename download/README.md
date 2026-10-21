# 下载页与版本接口

公开入口：[rumo.hirkond.online](https://rumo.hirkond.online/)。

- `index.html`：无外部脚本、字体或样式依赖的静态下载页。
- `latest.json`：应用更新接口，APK 地址同样使用自有域名。
- `releases/`：放置实际分发的安装包；APK 被 `.gitignore` 排除，不随源码提交。

当前分发的是原始 0.3.0 包，大小 2,288,801 字节，SHA-256：

```text
b17233ec3c6bd944895b88caf77131f8af5d2cb7a9137e3f47a1b37e67e43a29
```

迁移网站没有修改 APK。已有安装可手动将更新源切换为 `https://rumo.hirkond.online/latest.json`；后续源码构建默认使用此地址。

## 本地预览

在仓库根目录执行：

```bash
uv run --python 3.13 python -m http.server 4175 --bind 127.0.0.1 --directory download
```

打开 `http://127.0.0.1:4175/`。验证下载前，需将对应 APK 放在 `download/releases/`，并核对 `latest.json` 的文件名和哈希。

## 发布

站点可由 nginx 等普通静态服务器提供。发布完整的 `download/` 内容（包含已校验的 APK），启用 HTTPS，并保留 `/latest.json` 与版本化 `/releases/` 路径。不要把请求转发到旧下载站。

新版本先发布 APK 并核对实际下载哈希，再更新 `latest.json` 与页面内容。保持历史 APK 文件名不变；版本接口建议不缓存，以便及时获取最新版本。

维护者实际部署目录、备份和回退命令记录在本机忽略的 `.deploy/` 中。
