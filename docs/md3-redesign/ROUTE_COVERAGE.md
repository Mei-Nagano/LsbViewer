# 路由覆盖核对（离线静态）

由脚本扫描 `ui/**/navigation` 与各 Screen 的 `composable(...)` / `settingsComposable(...)` 字面量，
与 [SCREEN_SPECIFICATIONS](SCREEN_SPECIFICATIONS.md) §3 的完整路由表按首段名称比对。
运行时导航行为仍需设备验证；本表只证明入口仍被注册。

| 规格路由 | 对应规格 | 注册情况 |
| --- | --- | --- |
| `home` | P01/P02 | 已注册 |
| `forums` | P03 | 已注册 |
| `forum/{id}?p={p}` | P03 | 已注册 |
| `me` | P10 | 已注册 |
| `appSettings` | P13 | 已注册 |
| `detailEmpty` | P01 | 已注册 |
| `search?q={q}&scope={scope}&sort={sort}` | P04 | 已注册 |
| `search?q={q}&field={field}` | P04 | 已注册 |
| `login` | P10 | 已注册 |
| `topic/{tid}?p={p}&floor={floor}` | P05 | 已注册 |
| `newTopic` | P06 | 已注册 |
| `editTopic/{tid}` | P06 | 已注册 |
| `user/{uid}?tab={tab}` | P11 | 已注册 |
| `userByName?name={name}&tab={tab}` | P11 | 已注册 |
| `settings` | P17/P18 | 已注册 |
| `generalSettings` | P17 | 已注册 |
| `networkSettings` | P15 | 已注册 |
| `dohSettings` | P15 | 已注册 |
| `browseSettings` | P13 | 已注册 |
| `dataManagement` | P17 | 已注册 |
| `aiSettings` | P16 | 已注册 |
| `transferSettings` | P17 | 已注册 |
| `themeSettings` | P14 | 已注册 |
| `exportedTopics` | P12 | 已注册 |
| `exportedHtml?path={path}` | P12 | 已注册 |
| `about` | P17 | 已注册 |
| `web?url={url}` | P18 | 已注册 |
| `blockWords` | P13 | 已注册 |
| `inviteCenter` | P18 | 已注册 |
| `userBlock?url={url}` | P18 | 已注册 |
| `gachaProfile` | P18 | 已注册 |
| `gachaCenter` | P18 | 已注册 |
| `gachaMarket` | P18 | 已注册 |
| `gachaOperation/{kind}?title={title}` | P18 | 已注册 |
| `leaderboard?type={type}` | P18 | 已注册 |
| `notifications` | P08 | 已注册 |
| `directMessages` | P08 | 已注册 |
| `topicCollections?tab={tab}` | P07 | 已注册 |
| `directMessagesRoot` | P01/P08 | 已注册 |
| `topicCollectionsRoot` | P01/P07 | 已注册 |
| `collectionActions?path={path}` | P07 | 已注册 |
| `topicCollectionManage?path={path}` | P07 | 已注册 |
| `footprint` | P12 | 已注册 |
| `favorites` | P12 | 已注册 |
| `chat/{userId}?name={name}&avatar={avatar}` | P09 | 已注册 |
| `report/{type}/{id}` | P18 | 已注册 |
| `sourcePromotions` | P18 | 已注册 |
| `userPrivacy` | P18 | 已注册 |
| `communityActivity` | P18 | 已注册 |
| `attachments` | P12/P18 | 已注册 |
| `uploadSettings` | P15 | 已注册 |
| `storageSettings` | P17 | 已注册 |
| `myContent` | P12 | 已注册 |
| `cardRedemptions` | P12 | 已注册 |
| `autoClearSettings` | P17 | 已注册 |
| `webDavSettings` | P17 | 已注册 |
| `aiConfiguration` | P16 | 已注册 |
| `aiHistory` | P12/P16 | 已注册 |
| `themeColors` | P14 | 已注册（`ExtraSettingsRoutes` 列表循环） |
| `themeBackground` | P14 | 已注册（`ExtraSettingsRoutes` 列表循环） |
| `themeFonts` | P14 | 已注册（`ExtraSettingsRoutes` 列表循环） |
| `themeLayout` | P14 | 已注册（`ExtraSettingsRoutes` 列表循环） |

合计：规格路由 62 条，缺失 0 条；其中 4 条主题子页由 `listOf(...).forEach` 动态注册。

## 新增路由（不在旧规格表内）

| 路由 | 位置 | 说明 |
| --- | --- | --- |
| `messages?section={section}` | `navigation/Routes.kt` | 新版消息根（私信/通知分区）；旧 `notifications`、`directMessages`、`directMessagesRoot` 仍保留 |

## 兼容别名（旧入口保留）

- `directMessagesRoot` / `directMessages` / `notifications`：分别映射到消息页与分区根；旧返回栈仍可用。
- `topicCollectionsRoot` / `topicCollections?tab={tab}`：淘帖根与带 tab 的兼容入口并存。
- `search?q={q}&field={field}`：旧搜索参数入口保留，`field` 映射到 scope。
- `exportedHtml?path={path}`：拆分到 `ExportedHtmlScreen.kt` 后路由未变。
