<div align="center">

# LinuxSB

**面向 Android 的 Linux.sb 第三方社区客户端**

专注阅读、交流与内容整理 · Kotlin · Jetpack Compose · Material Design 3

[下载安装](https://github.com/Mei-Nagano/LsbViewer/releases) · [问题反馈](https://github.com/Mei-Nagano/LsbViewer/issues) · [社区网站](https://linux.sb)

</div>

---

LinuxSB 通过解析 Linux.sb 网页提供移动端社区体验。账号、帖子与互动规则以源站为准；本项目不隶属于源站，也不代表源站运营方。

## 阅读与交流

- 首页分类、板块浏览、申精帖子、站内搜索与屏蔽管理。
- 正文与评论采用源站 HTML 和样式，支持代码高亮、数学公式、横向滚动表格、图片查看与帖内搜索。
- 楼主徽章、正文链接及链接高光跟随应用主题配色，称号、用户组和奖励保留源站颜色。
- 正文包含 Markdown 标题时显示阅读进度与目录入口；帖子页不重复展示标题状态标签。
- 应用内所有帖子卡片不显示页码，点击固定打开第一页；搜索主题结果和侧栏热帖同样从第一页打开，评论收藏及明确回复链接保留目标定位。
- 树形评论、折叠回复、复制、引用回复、点赞投币及源站权限允许的帖子操作。
- 发帖与编辑使用应用内源站编辑器，发布、预览、草稿及附件遵循源站规则。
- 私信、通知、淘帖专辑订阅、称号管理及社区榜单。
- 帖子钉住、阅读位置、收藏与浏览历史。
- 源站侧边栏提供可切换 24小时、36小时和一周的每日热榜，以及站点统计与当前在线；宽屏入口位于导航侧栏最顶部。

## 个性化与工具

- Material Design 3 配色、深浅模式、独立中性表面、自定义背景及表面透明度。
- 字体选择、界面缩放与字重、自定义导航顺序，以及宽屏侧栏和列表／详情并排布局。
- 所有层级设置按功能分组连接卡片，整组外缘大圆角、相邻处小圆角。输入框和滑条直接在设置页编辑，离散选项通过菜单选择；支持搜索具体选项、自动滚动定位与短暂高光。
- 收藏使用 Material Design 3 分段筛选器，支持按帖子或评论筛选和本地搜索。
- 隐藏状态栏可在外观页设置；窄屏保留顶部安全留白，宽屏不增加该留白。
- HTTP / SOCKS5 应用代理，启动和回到应用时自动识别并保存；有效手动配置保留。代理启用时暂停 DoH。
- 可配置 DoH、服务器测速与 VPN 场景停用选项；DoH 生效时源站链接始终参与，外部网页可单独控制。
- 切换源站域名的设置暂时隐藏，已保存的域名配置继续用于请求。当前备用入口 `bing.you` 转发至主站，应用跟随跳转；独立备用服务需要服务端提供相同内容与接口。
- 源站附件空间、Pixhost／Catbox 匿名图床及自定义上传接口。
- OpenAI 兼容接口的 AI 总结、模型选择、提示词和多方案管理。
- HTML、Markdown、分页长图导出；分类数据清理与 JSON / WebDAV 备份。
- 发帖、称号、原站广告和个人资料只在首次打开时定位内容，后续跳转继续留在浏览器中；支持真实子窗口、窗口切换和 WebView 原生 JavaScript 对话框。

## 安装

从 [Releases](https://github.com/Mei-Nagano/LsbViewer/releases) 下载适合设备的 APK。最低支持 Android 8.0。

| 安装包 | 适用设备 |
| :--- | :--- |
| `armv8a` | 64 位 ARM 手机和平板 |
| `armv7a` | 32 位 ARM 设备 |
| `universal` | 通用版本，包含多个架构 |

## 本地构建

准备 JDK 17 或更新版本，以及 Android SDK 37。在 Android Studio 中打开项目，或使用项目附带的 Gradle Wrapper：

```sh
./gradlew assembleDebug
```

Windows 使用 `gradlew.bat assembleDebug`。安装包输出至 `app/build/outputs/apk/debug/`。正式发布应配置自己的签名密钥，并妥善保管签名凭据。

## 代码结构

工程在单一 Android 模块内按业务域组织。`domain/` 放置纯 Kotlin 业务模型与 repository 契约，`data/` 实现论坛传输、源站 HTML 解析、持久化和各业务数据服务，`common/` 提供跨层共用的网络地址、文本转换和规则；`di/` 负责 repository 与本地存储适配器装配，`platform/` 放置验证码、Cookie 与 WebView 等平台桥接。`ui/feature/` 按业务域放置 ViewModel，`ui/screens/` 组合 Route、页面状态与 Compose 界面，页面请求和业务状态由对应 ViewModel 管理；`ui/navigation/` 负责导航接线，`ui/session/` 管理应用级 UI 状态和 Session 生命周期协调。论坛交互依赖网页和表单协议，不提供 REST API，因此源站请求与 HTML 结构集中在数据边界维护。

## 隐私与使用说明

浏览和互动会连接所选源站入口及其跳转目标；图片上传会连接源站附件空间或所选图床。匿名上传的图片通常可被持有链接的人访问，请勿上传敏感资料。AI 总结仅在相关功能启用和请求触发时，将所选内容发送至用户配置的接口。包含 API 密钥或账号信息的备份应妥善保管。

部分功能依赖源站结构、账号权限及网络环境。遇到问题时，请附上应用版本、Android 版本、复现步骤及去除敏感信息的截图。请勿在反馈中公开密码、Cookie 或 API 密钥。

## 致谢与许可

感谢 Linux.sb 社区，以及 Jetpack Compose、Jsoup、OkHttp、Cronet、Coil、materialkolor 和 [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) 等开源项目。设置图标使用通过 [Iconify](https://github.com/iconify/iconify) 获取的 Google Material Symbols，采用 Apache-2.0 许可；图标本地打包，运行时无需联网获取。第三方许可可在应用内「开源协议」查看。

项目采用 [MIT License](LICENSE)。社区内容及第三方资源的权利归各自权利人所有。
