# LinuxSB

基于对 [linux.sb](https://linux.sb) 网页解析制作的第三方 Android 社区客户端，使用 Kotlin + Jetpack Compose 构建，遵循 Material Design 3 设计规范。

> 本项目仅作学习交流用途，所有数据内容均来自 linux.sb 源站，版权归原网站及发帖用户所有。

## 功能特性

- **浏览**：首页分类（新评论 / 新帖子 / 精华）、板块列表、搜索、无限滚动与翻页双模式（可按首页 / 板块 / 帖子分类生效）
- **帖子**：楼层跳转与高亮、#楼层号对话串联、代码块折叠与复制、评论排序、打赏弹幕、导出 HTML / Markdown / 长图
- **互动**：登录、发帖、回帖、点赞、投币、举报、每日签到、用户榜单、通知、私信
- **个性化**：HCT 主题引擎（主题色 / 调色风格 / 对比度）、卡片元素级改色、深浅模式与 OLED 纯黑、动态取色
- **工具**：AI 帖子总结（OpenAI 兼容 API）、本地浏览历史、屏蔽词 / 屏蔽用户、本地缓存分析、设置备份（JSON / WebDAV）
- **更新**：应用内检查 GitHub Release 新版本，可配置检查时机

## 下载

前往 [Releases](https://github.com/Mei-Nagano/LsbViewer/releases) 下载最新版本：

| 文件名 | 适用设备 |
| --- | --- |
| `LinuxSB-vX.Y.Z-armv8a.apk` | 64 位 ARM 设备（主流现代手机） |
| `LinuxSB-vX.Y.Z-armv7a.apk` | 32 位 ARM 设备（旧设备） |
| `LinuxSB-vX.Y.Z-universal.apk` | 通用版（包含全部架构，体积较大） |

## 构建

```bash
# 克隆仓库
git clone https://github.com/Mei-Nagano/LsbViewer.git
cd LsbViewer

# 构建 Debug APK（输出三个架构版本）
./gradlew assembleDebug

# 构建 Release APK（默认使用仓库内测试 keystore，可用环境变量覆盖）
LSB_STORE_FILE=/path/to.jks LSB_STORE_PASSWORD=*** LSB_KEY_ALIAS=*** LSB_KEY_PASSWORD=*** \
  ./gradlew assembleRelease
```

要求：JDK 17+，Android SDK 35。

推送 `v*` 标签（如 `v1.0.0`）后 GitHub Actions 会使用仓库 Secrets 中的签名材料自动构建并发布 Release，APK 命名带版本号，Release 说明自动生成。签名 Secret 配置：`LSB_KEYSTORE_BASE64`（keystore 的 base64）、`LSB_STORE_PASSWORD`、`LSB_KEY_ALIAS`、`LSB_KEY_PASSWORD`。

## 技术栈

- Kotlin · Jetpack Compose · Material 3
- Jsoup（HTML 解析）· OkHttp（网络与 Cookie）
- Coil（图片加载，含 SVG）· materialkolor（HCT 主题引擎）
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass)（液态玻璃底栏）
- Navigation Compose · WebView（应用内浏览 / 人机验证）

## 开源协议

[MIT License](LICENSE)

## 致谢

- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) —— 本应用的液态玻璃底栏基于 Kyant0 的 Backdrop 库实现，实时折射身后内容、动态模糊与透镜质感为界面带来了独特的视觉体验，感谢作者的开源分享。
