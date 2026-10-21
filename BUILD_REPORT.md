# 公开源码构建验证

验证日期：2026-10-21。项目：入墨 Rumo；Android 包名 com.hirden.flomofloat，版本 0.3.0，versionCode 3。

## 本地检查结果

在不包含维护者签名文件的公开源码目录执行：

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

结果：

- APK 构建成功。
- 7 个测试套件，32 项测试通过；0 失败、0 错误、0 跳过。
- Lint 0 错误，12 条非阻断警告。
- XML 文件可解析，Gradle 启动脚本语法检查通过。
- 文档本地链接检查通过；公开文件扫描未发现签名文件、GitHub token、私钥或疑似个人 webhook。

环境为 JDK 17、Gradle 8.13、AGP 8.13.2、Kotlin 2.2.21、compileSdk 36、Build Tools 35.0.0。此处为本地验证结果，仓库中的 GitHub CI 配置需在推送后单独查看运行结果。

## 分发包与源码构建

本次源码验证使用默认本机调试签名；公开仓库不包含维护者签名私钥。普通开发者的自行构建包与下载页签名可能不同，不能直接覆盖安装。

下载页分发的原始 0.3.0 APK 为 2,288,801 字节，其 SHA-256 为：

```text
b17233ec3c6bd944895b88caf77131f8af5d2cb7a9137e3f47a1b37e67e43a29
```

该分发包沿用旧版包名和签名。下载页及版本接口已公开，匿名读取与安装包下载的哈希一致。

## 验证范围

自动化测试基于 Robolectric API 34。存储测试使用注入 AES 密钥，更新测试使用模拟包管理器，不能代替真实 Android Keystore、DownloadManager、厂商后台策略和系统安装器集成测试。

手机验证步骤见 TESTING.md。不同机型的窗口、键盘、手势、后台行为和完整升级路径仍需实机验证。

## 首次 GitHub 上传复查

2026-10-21 上传前，已检查全部公开文件、9 个 XML 文件、12 处文档本地链接及 Gradle 启动脚本语法，未发现真实凭据或签名文件。测试中的 webhook 均为占位地址。

复查发现随源码附带的 Wrapper 为 Gradle 4.6，已将 Wrapper JAR 和启动脚本更新为 Gradle 8.13 官方文件，JAR SHA-256 为 `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`，并在 `gradle-wrapper.properties` 中固定 Gradle 分发包 SHA-256。

本次上传使用的 Mac 尚未配置 JDK 和 Android SDK，未在此机器重跑上述本地构建。上传后的独立验证由 [Android CI](https://github.com/P-t99/rumo/actions/workflows/android.yml) 执行测试、Lint 与 APK 构建，具体结果以对应提交的工作流为准。
