# 0.4.3 主题与编辑状态保留验证

日期：2026-10-22。包名 `com.hirden.flomofloat`，versionCode 7。

- 使用 `:app:clean`、`--no-build-cache` 后完成 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`。
- 11 个测试套件、61 项测试通过，0 失败 / 错误 / 跳过；Lint 0 错误、14 条非阻断警告。
- 新增主题回归：三种模式持久化、系统跟随与手动覆盖、主界面 / 悬浮笔记 / 设置原地换色、未保存内容与光标保留、输入租约和窗口布局保留、正常文字对比度。
- 原窗外 / 失焦交接、模拟外部输入连接、被动收起保留外部键盘、设置页输入与默认布局稳定性检查全部通过。
- 测试运行时类与当前编译类一致，APK 包含主题配色和原地更新逻辑。
- APK：`Rumo-Flomo-0.4.3-test.apk`，2,258,943 字节，SHA-256 `74f86f1fc3deb425d2c1561c5e7269a50cb285c6b6950ff390ace028be2e9793`。
- 使用原分发测试证书重新签名，验证通过：`227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`。可以覆盖旧测试版并保留应用数据，勿先卸载。维护者私钥不进入仓库。

自动化基于 Robolectric API 34、模拟窗口区域及外部 InputConnection，真实微信和厂商输入法仍需手机验证。默认布局及焦点交接规则保持；主题切换无需重置。更新接口、下载页及版本化 APK 已准备，本地发布文件校验通过；现有发布服务器连接未接入，应用内更新尚未上线。

---

# 0.4.2 展开自动聚焦验证

日期：2026-10-21。包名 `com.hirden.flomofloat`，versionCode 6。

- 使用 `:app:clean`、`--no-build-cache` 后完成 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`。
- 10 个测试套件、53 项测试通过，0 失败 / 错误 / 跳过；Lint 0 错误、14 条非阻断警告。
- 新增 / 调整回归：展开后正文立即聚焦、草稿末尾续写、收起重开重新聚焦、初始键盘请求可取消、已展开窗口重复打开不抢回外部焦点。
- 原窗外 / 失焦交接、模拟外部输入连接、被动收起保留外部键盘、设置页输入与默认布局稳定性检查全部通过。
- 测试运行时类与当前编译类一致，APK 包含新的主动聚焦入口。
- APK：`Rumo-Flomo-0.4.2-test.apk`，2,250,751 字节，SHA-256 `8bb89d8ed300379cbbaa698c8f92ab9bf87d16252d9287226765f920a952a8a2`。
- 使用原分发测试证书重新签名，验证通过：`227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`。可以覆盖旧测试版并保留应用数据，勿先卸载。维护者私钥不进入仓库。

自动化基于 Robolectric API 34、模拟窗口区域及外部 InputConnection，真实微信和厂商输入法仍需手机验证。默认布局规则沿用 0.4.1，无需再次重置即可使用自动聚焦；已准备 0.4.2 更新接口、下载页和版本化安装包；当前会话未连接自有域名发布服务器，尚未上线。

---

# 0.4.1 输入焦点与稳定默认布局验证

日期：2026-10-21。包名 `com.hirden.flomofloat`，versionCode 5。

- 使用 `:app:clean` 与 `--no-build-cache` 干净重建；核对测试运行时类与当前编译类一致，最终 APK 包含新输入焦点控制器。
- `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` 成功，10 个套件、51 项测试通过，0 失败 / 错误 / 跳过。
- Lint 0 错误，14 条非阻断警告。
- 新增回归：展开和移动窗口不抢输入、窗外触摸与窗口失焦释放、重新点击正文恢复编辑、取消过期的键盘请求、设置页输入交接、收起后不隐藏外部输入法、关闭重开后的旧请求清理。
- 默认布局在普通键盘开合及模拟动画尺寸变化时保持不动；已经放得下的手动布局也不再强制缩小到 80%。
- APK：`Rumo-Flomo-0.4.1-test.apk`，2,246,655 字节，SHA-256 `afa20d23a4783abec1eef2c34de0aa97703f90839baf134e09ebb8dbddf028b8`。
- 测试包由 debug 构建使用原测试证书重新签名，签名验证通过，与原 0.3.0 / 0.4.0 分发测试证书一致：`227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`。可覆盖安装并保留数据，勿先卸载；维护者私钥不进入仓库。

测试基于 Robolectric API 34 和模拟显示区域。外部输入使用模拟 Activity 与 InputConnection，并验证窗口标志、窗外 / 失焦通知；尚未进行真实微信或厂商输入法实测。

旧手动布局保留，升级后点击一次“重置”可使用新的紧凑上方默认窗口。点击正文开始输入，点击窗外释放焦点。线上更新通道尚未分发此测试版。

---

# 0.4.0 可调整窗口验证

日期：2026-10-21。包名 `com.hirden.flomofloat`，versionCode 4。

- `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug` 成功。
- 9 个测试套件、42 项测试通过，0 失败 / 错误 / 跳过。
- Lint 0 错误、13 条非阻断警告。
- 功能提交 `4743034` 的 [GitHub CI](https://github.com/P-t99/rumo/actions/runs/39611849102) 已通过测试、Lint 与 APK 编译。
- 测试覆盖尺寸与位置持久化、笔记 / 设置及横竖屏隔离、重置保留 API / 草稿 / 标签 / 入口外观、真实缩放手柄与收起手势隔离、取消拖动、越界回收、模拟输入法避让及恢复、最小窗口底部操作与滚动正文布局。
- APK：`Rumo-Flomo-0.4.0-test.apk`，2,246,655 字节，SHA-256 `9cfd49d826e5258b3cdadaff4a3681168c22ea0471425ec5d1ed8bb89cf4d05f`。
- 测试 APK 由 debug 构建使用原分发测试证书重新签名；签名验证通过，与原 0.3.0 证书一致：`227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`。可以覆盖同签名旧版，保留应用数据；请勿先卸载。签名私钥不进入公开仓库。

自动化环境为 Robolectric API 34，另有纯几何单元测试；键盘可用区由测试模拟，未进行真实手机 / 输入法实测。浮动或分离式输入法可能不提供遮挡区域，需手动移动 / 缩放。

本次提供独立测试 APK 和功能分支，不修改线上下载页及更新接口；待手机验证后再分发。

---

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
