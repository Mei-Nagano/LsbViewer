# 离线静态审计（MD3 改版）

日期：2026-10-21  
范围：不依赖网络与设备交互的检查；设备/视觉项见 [ACCEPTANCE_EVIDENCE](ACCEPTANCE_EVIDENCE.md)。

## 1. 路由覆盖

- 脚本扫描 `ui/**` 的 `composable("…")` / `settingsComposable("…")`，与规格 §3 路由表按首段名比对：**62/62 全部注册，缺失 0**。
- 4 条主题子页（`themeColors` / `themeBackground` / `themeFonts` / `themeLayout`）由 `ExtraSettingsRoutes` 的 `listOf(...).forEach` 动态注册。
- 新增 `messages?section={section}`；旧 `notifications` / `directMessages` / `directMessagesRoot` 仍注册并映射到消息页。
- 详细逐条清单：[ROUTE_COVERAGE.md](ROUTE_COVERAGE.md)。

## 2. 依赖边界

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| `ui/theme` 是否引用会话/导航/仓库/ViewModel | `grep -E 'ui\.session|androidx\.navigation|Repository|ViewModel|sb\.linux\.client\.data'` | 0 处 |
| `ui/components` 是否引用会话/导航/仓库/ViewModel | 同上 | 0 处 |

结论：主题与基础组件仍只依赖 Compose/Material3；页面与导航负责装配 Session 与事件。

## 3. 形状与颜色收敛

| 检查 | 结果 |
| --- | --- |
| 遗留 `RoundedCornerShape(18/19/20/14/13/15.dp)` | 0 处（18/20→16、14→16、13→12、15→16，共 25 个文件） |
| 剩余非角色圆角 | `6dp×3`、`9dp×2`、`10dp×11`、`22dp×2`、`24dp×1`、`2dp×1`：角标/媒体/头像预览等微形状，未全量替换，待视觉确认 |
| `Color(0x…)` 出现在 `ui/theme` 之外 | 14 处，均为语义数据色或媒体遮罩，不是页面级主题补丁 |

14 处清单：

- `ui/BadgeComponents.kt` ×5：源站等级（UR/SSR/SR/R/默认）徽章色，属于数据语义。
- `ui/content/ImagePager.kt` ×3、`ui/content/HtmlInlineParser.kt` ×1：图片/标记遮罩层。
- `ui/patterns/user/Avatar.kt` ×1：在线状态点。
- `ui/screens/LeaderboardScreen.kt` ×3：榜单前三名奖牌色。
- `ui/session/SessionThemePreferenceController.kt` ×1：用户自定义主题色的持久化解析，紧邻主题层。

## 4. 可访问名称（启发式）

- 扫描 `IconButton { Icon(…) }`：**0 处**第二参数为 `null` 的图标按钮（即图标按钮均有中文/可读描述）。
- `uiautomator` 中“clickable 且 text/content-desc 为空”的节点主要是 Compose 的父子语义拆分（整行可点、文本在子节点），**不作为缺失标签的证据**。
- TalkBack 实际朗读顺序仍未执行，A21 保持“待执行”。

## 5. 规模与构建

| 检查 | 结果 |
| --- | --- |
| 变更 Kotlin 文件规模 | 236 个，0 个超过 250 物理行 |
| 生产编译 | `:app:compileDebugKotlin` 通过（exit 0） |
| 候选包 | `:app:assembleDebug` 通过；APK 摘要见验收记录 |
| 测试源码编译 | 旧基线失败（两个测试引用已删除的 gacha 解析器），与 UI 改动无关 |

## 6. 偏好迁移清单（代码位置）

- `data/BottomBarPreferenceStore.kt`：`bottom_bar_items` → 别名归一/去重，备份 `bottom_bar_items_pre_v3`，版本键 `bottom_navigation_version=3`，默认四项。
- `data/AppSettingsGeneralStore.kt`：`tablet_mode` 缺失/true = 跟随窗口，显式 false = 始终单内容栏；`bottom_bar_style` / `reply_bar_style` 仅保留兼容值。
- `data/ReadingPositionStore.kt`：旧四段 CSV 与两小时 TTL 不变，新增 `v2:<topicId>` 锚点；`clear()` 同时清理。
- 其余旧键（字体、背景、代理、DoH、AI 方案）未改名，见 `SessionPreferenceLoader` / `AppSettingsBackupExport`。

### 本轮修复

- **备份导入丢失“消息”根**：`AppSettingsBackupImport` 的 `bottom_bar_items` 白名单仍是旧集合
  （`directMessages`/`newTopic`…），导入新版备份时会把 `messages` 过滤掉，导致“导出→恢复”后消息入口消失。
  已改为别名归一（`directMessages*`/`notifications` → `messages`，`topicCollectionsRoot` → `topicCollections`），
  白名单改为 `home/forums/topicCollections/messages/me`，`newTopic` 丢弃；数量不足 2 时不覆盖本地值。
  编译已通过；旧备份文件的实际导入仍需设备/文件选择器实测。