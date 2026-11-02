# 0.4.9 自有域名发布

2026-11-02（北京时间），完成此前中断的发布。

| 项目 | 结果 |
| --- | --- |
| 下载页 | https://rumo.hirkond.online/ |
| 更新接口 | https://rumo.hirkond.online/latest.json |
| APK | https://rumo.hirkond.online/releases/Rumo-Flomo-0.4.9.apk |
| 包名 / 版本 | `com.hirden.flomofloat` / 0.4.9 / code 13 |
| 系统 | minSdk 26 / targetSdk 35 |
| APK 大小 | 2,279,415 字节 |
| APK SHA-256 | `b47f38a32346b4e65a64c26894b5d149c79b54c43d88c0eb56d6107988acba29` |
| 证书 SHA-256 | `227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c` |
| 成功构建源提交 | `0405b4eca0ca8f64db507890b9398391fceac484` |

## 验证

[CI 41290020834](https://github.com/P-t99/rumo/actions/runs/41290020834) 的 99 项测试全通过，Lint 无错误。最终签名 APK 与 CI APK 中除 META-INF 之外的所有条目逐项字节一致。`apksigner verify --verbose --print-certs` 成功，v2 / v3 签名及证书与原分发包一致；`aapt dump badging` 检查包名、版本与 SDK。

先上传版本化 APK；模拟 Android DownloadManager 的匿名 HTTPS GET 返回 200，MIME 为 `application/vnd.android.package-archive`，实际大小与哈希一致；再更新下载页与版本接口。最后以无查询参数的请求验证页面、接口及 APK 相互匹配。手机真实安装和 HyperOS 行为待用户测试。

## 发布与回退

服务静态目录 `/var/www/site-rumo/`，此前页面、接口备份至 `/opt/rumo/backups/20261102-0.4.9/`。回退时分别将该目录的 `index.html` 与 `latest.json` 复制到同一静态目录中的临时文件，再原子替换；保留 0.4.9 APK，避免中断已开始的下载。该操作恢复发布前域名上的 0.3.0 接口，不会降低手机已安装版本。

用户安装维护者分发版本时直接覆盖，勿先卸载。旧版若还连原站，首次手动填入新源或直接覆盖安装；0.4.9 将原默认源解析到新域名，其他自定义地址保留。
