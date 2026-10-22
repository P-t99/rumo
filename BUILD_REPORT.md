# 0.4.8 文字菜单遮挡修复验证

日期：2026-10-22。包名 `com.hirden.flomofloat`，versionCode 12。

- 用户反馈：小米 17 Pro Max、HyperOS 3.03、入墨 0.4.7 能选中文字但无菜单，常驻工具栏的编辑按钮正常。没有直接连接或操作该手机。
- 已确认的代码错误：悬浮卡片 elevation 为 12dp，而作为同级子视图的菜单为 10dp。visibility=VISIBLE 不代表菜单未被遮挡；旧测试通过 performClick 直接调用按钮，也绕过了真实触摸命中。
- 新增两个回归：在原生 Canvas 中绘制完整窗口，用独特实色验证菜单没有被卡片覆盖；从窗口根视图派发实际点击，验证复制按钮收到点击并写入正确选区。旧代码两项均失败：菜单像素被卡片覆盖、点击未复制；修复后两项通过。
- 修复仅让菜单 elevation 根据卡片当前 Z 值增加 4dp；保留按需菜单与可选常驻栏的现有行为。
- `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-build-cache` 成功。13 个套件、94 项测试通过，0 失败 / 错误 / 跳过；Lint 0 错误、17 条非阻断警告、1 条提示。
- APK：`Rumo-Flomo-0.4.8-test.apk`，2,279,423 字节，SHA-256 `73fb2ae6f15627db9e84de2f5f4558263fcec3706ac2e07f8c107fe6dcac5020`。
- 原分发签名 `227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`，可覆盖安装保留 API、草稿和布局。最终 APK 的包名、code 12、0.4.8 与原默认更新源已检查。

自动化使用 Robolectric API 34 及原生字体与绘制。没有小米实机验证；厂商选区手柄、放大镜、真实输入法与微信仍需用户确认。若此前开启了常驻工具栏，升级保留该选项，测试按需菜单前请关闭。操作见 [TESTING.md](TESTING.md)，发布记录见 [PUBLISH_0.4.8.md](docs/PUBLISH_0.4.8.md)。

---

# 0.4.7 真实触摸长按兼容验证

日期：2026-10-22。包名 `com.hirden.flomofloat`，versionCode 11。

- `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-build-cache` 成功。
- 13 个套件、92 项测试通过，0 失败 / 错误 / 跳过；Lint 0 错误、17 条非阻断警告、1 条提示。
- 新增 9 项真实触摸事件回归：按下 → 持续按住 → 松手、空白粘贴、系统消费长按却不选词 / 不创建菜单、完全不触发原长按回调、普通点击、纵向滚动、取消触摸、中文与 emoji 字符簇。
- 新回归从 FloatingX 窗口派发 SOURCE_TOUCHSCREEN 事件并推进系统长按超时，不通过直接调用 performLongClick 触发目标操作。菜单、复制内容和焦点均有断言。
- 旧版在模拟“系统消费长按但不创建文字编辑模式”时，选词与粘贴两项回归失败；修改后通过。原 83 项回归继续通过。
- 文字测试使用 Robolectric API 34 和原生字体度量，显式模拟窗口焦点；分类器使用 NO_OP，因为测试环境没有真实智能选词服务。测试不能代替手机厂商行为验证。
- 不使用隐藏生产 API；菜单仍是同一悬浮窗口中的子视图，实际编辑调用公开 EditText 操作。
- APK：`Rumo-Flomo-0.4.7-test.apk`，2,279,423 字节，SHA-256 `3eae5806b830f2b6e022b0f118dc46fa2b196f9c0868acf9648cbd65b6389c30`。
- 原签名证书：`227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`，可覆盖安装保留配置和草稿。最终 APK 已检查包名、版本、独立长按回传代码与原默认更新地址。

手机型号与系统版本尚未取得；真实厂商选区手柄、放大镜、输入法、微信和系统安装器仍待用户验证。操作见 [TESTING.md](TESTING.md)，发布与回退记录见 [PUBLISH_0.4.7.md](docs/PUBLISH_0.4.7.md)。

---

# 0.4.6 按需文字编辑菜单验证

日期：2026-10-22。包名 `com.hirden.flomofloat`，versionCode 10。

- `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-build-cache` 成功。
- 13 个套件、83 项测试通过，0 失败 / 错误 / 跳过；Lint 0 错误、17 条非阻断警告、1 条提示。
- 新增 14 项按需菜单回归，覆盖默认无常驻占位、长按选词 / 空白粘贴、全选后复制、菜单内复制保留草稿和焦点、两次返回处理、窗外释放、过期菜单取消、兼容选项实时切换 / 持久化、主界面 / 悬浮设置同步、窗口 / 键盘可见区域边界、深浅主题及滚动隐藏。
- 保留上一版 8 项常驻工具栏回归，测试中显式开启兼容选项；原焦点、布局、主题与更新回归继续通过。
- 菜单作为同一 WindowManager 容器内的子视图显示，使用 Android 原生选区生命周期和编辑操作；若长按只选择文字却未创建 ActionMode，延后补出菜单并校验当前焦点和手势代次。
- APK：`Rumo-Flomo-0.4.6-test.apk`，2,275,327 字节，SHA-256 `ec980cac1d69e9beb9218d19e79357d9cfd91293fbbd2858ad5f387abb56ebd3`。
- 原分发证书签名与验证通过：`227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`，可覆盖安装保留配置与草稿；实际 APK 已检查包名、code 10、版本 0.4.6 及兼容选项代码。

测试基于 Robolectric API 34，显式模拟窗口焦点，并为程序化滚动补发绘制事件。厂商选区手柄、真实系统剪贴板、浮动输入法与微信仍待实机验证。新版不使用隐藏 API，也不依赖指定输入法。操作步骤见 [TESTING.md](TESTING.md)，发布与回退记录见 [PUBLISH_0.4.6.md](docs/PUBLISH_0.4.6.md)。

---

# 0.4.4 悬浮文字编辑验证

日期：2026-10-22。包名 `com.hirden.flomofloat`，versionCode 8。

- `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-build-cache` 成功。
- 12 个套件、69 项测试全部通过，0 失败 / 错误 / 跳过；Lint 0 错误、15 条非阻断警告。
- 新增 8 项文字编辑回归，包含框架原生长按路径与选区、ActionMode 创建及清理、全选 / 复制 / 剪切、部分选区替换、多行和 emoji、空笔记粘贴、标签与悬浮设置、遮挡 API 保护、草稿 / 窗口保留及窗外释放。
- 原主题、焦点交接、缩放、布局记忆、默认布局稳定性和更新文件复用测试继续通过。
- API 单行输入先配置，再设置遮挡变换，避免单行属性覆盖隐藏显示。
- APK：`Rumo-Flomo-0.4.4-test.apk`，2,263,039 字节，SHA-256 `25115b95975bb0d781f71c97895badd8f9907058fb6c6ae53fd1bd3d77def213`。
- 原分发证书签名与验证通过：`227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`。可覆盖安装保留数据，不要先卸载。
- APK 实际 DEX 包含 `OverlayTextActions` 与原站默认更新源，包名 / code 8 / 版本 0.4.4 与发布接口一致。

自动化使用 Robolectric API 34，长按测试发送实际按下 / 抬起事件并关闭测试环境的智能选词分类器；这验证框架编辑生命周期，不代表已验证厂商选区手柄或真实微信。手机验证见 [TESTING.md](TESTING.md)。发布记录见 [PUBLISH_0.4.4.md](docs/PUBLISH_0.4.4.md)。

---

# 0.4.3 主题与编辑状态保留验证

日期：2026-10-22。包名 `com.hirden.flomofloat`，versionCode 7。

- 使用 `:app:clean`、`--no-build-cache` 后完成 `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug`。
- 11 个测试套件、61 项测试通过，0 失败 / 错误 / 跳过；Lint 0 错误、14 条非阻断警告。
- 新增主题回归：三种模式持久化、系统跟随与手动覆盖、主界面 / 悬浮笔记 / 设置原地换色、未保存内容与光标保留、输入租约和窗口布局保留、正常文字对比度。
- 原窗外 / 失焦交接、模拟外部输入连接、被动收起保留外部键盘、设置页输入与默认布局稳定性检查全部通过。
- 测试运行时类与当前编译类一致，APK 包含主题配色和原地更新逻辑。
- APK：`Rumo-Flomo-0.4.3-test.apk`，2,258,943 字节，SHA-256 `74f86f1fc3deb425d2c1561c5e7269a50cb285c6b6950ff390ace028be2e9793`。
- 使用原分发测试证书重新签名，验证通过：`227c6a824e56195d9cd797d623d9f57c5c23ef49a1aacf10a7a3af5ae3f9573c`。可以覆盖旧测试版并保留应用数据，勿先卸载。维护者私钥不进入仓库。

自动化基于 Robolectric API 34、模拟窗口区域及外部 InputConnection，真实微信和厂商输入法仍需手机验证。默认布局及焦点交接规则保持；主题切换无需重置。2026-10-22 已通过原 Sites 更新站发布 0.4.3：先发布并匿名下载验证 APK，再切换接口和下载页。无查询参数的公开接口返回 versionCode 7；模拟 Android HttpURLConnection / DownloadManager 的请求与下载页均返回 HTTP 200，实际 APK 大小、SHA-256 和签名与测试包一致。缓存响应为 `public, max-age=0, must-revalidate`。实际手机内检查、安装和数据保留仍待实测。当前更新地址见 [UPDATE_CHANNEL.md](UPDATE_CHANNEL.md)。

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
