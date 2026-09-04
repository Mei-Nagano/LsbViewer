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
- Markdown 正文、代码块、可横向滚动的表格、目录导航和全文搜索。
- 树形评论、折叠回复、引用选文、编辑预览和草稿保存。
- 私信、通知、淘帖专辑订阅、称号管理及社区榜单。
- 帖子钉住、阅读位置、收藏与浏览历史。

## 个性化与工具

- Material Design 3 配色、深浅模式、独立中性表面、自定义背景及表面透明度。
- 字体选择、字号与字重、自定义底部导航、经典或玻璃底栏。
- HTTP / SOCKS5 应用代理，以及可配置的 DoH、服务器测速和 VPN 场景自动停用选项。
- 匿名图床与自定义上传接口。
- OpenAI 兼容接口的 AI 总结、模型选择、提示词和多方案管理。
- HTML、Markdown、分页长图导出；分类数据清理与 JSON / WebDAV 备份。

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

## 隐私与使用说明

浏览和互动会连接 Linux.sb；图片上传会连接所选图床。匿名上传的图片通常可被持有链接的人访问，请勿上传敏感资料。AI 总结仅在相关功能启用和请求触发时，将所选内容发送至用户配置的接口。包含 API 密钥或账号信息的备份应妥善保管。

部分功能依赖源站结构、账号权限及网络环境。遇到问题时，请附上应用版本、Android 版本、复现步骤及去除敏感信息的截图。请勿在反馈中公开密码、Cookie 或 API 密钥。

## 致谢与许可

感谢 Linux.sb 社区，以及 Jetpack Compose、Jsoup、OkHttp、Cronet、Coil、materialkolor 和 [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) 等开源项目。

项目采用 [MIT License](LICENSE)。社区内容及第三方资源的权利归各自权利人所有。
