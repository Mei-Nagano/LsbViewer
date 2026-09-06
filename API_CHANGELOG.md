# API 变更记录

## 2026-09-06

- 淘帖新增独立 `TopicCollectionRepository` / `TopicCollectionService` 接口，列表、详情、管理页和主题页收录操作统一经过源站解析链路。
- 淘帖列表模型补充创建时间、订阅人数、管理路径；详情支持源站分页与独立管理入口。
- 主题页收录支持源站 `item_add`、`item_remove`、`item_remove_all` 操作；提交前刷新 CSRF，提交后重新读取源站确认状态。
- 淘帖列表同步不再对源站忽略的 `p` 参数做重复分页请求。

- 搜索接口已对齐源站新版 Meilisearch 页面：由 `POST /search + _csrf + field` 改为 `GET /search?q=&scope=&sort=&p=`。
- 搜索范围新增 `all`、`user`，主题排序支持 `relevance`、`latest`、`created`、`replies`、`views`。
- 搜索结果模型同时支持主题与用户；旧的 `field` 深链会兼容映射到 `scope`。
- 底栏导航改为串行协调器，支持导航期间保留最后一次点击、多标签状态保存/恢复；玻璃底栏改为 `Scaffold.bottomBar`，避免覆盖层命中冲突。

## 2026-09-05

- 新增内部 `sb.linux.client.api.KeywordFilterApi`，隔离屏蔽词远端读取与保存能力。
- 源站屏蔽设置请求扩展为五类字段：`presets`、`custom`、`users`、`forum_excluded_ids`、`forum_extra_ids`。
- APK 对外行为变化：屏蔽规则按登录账号隔离；关键词不区分英文字母大小写；首页支持版块屏蔽；保存失败时支持本地 pending 重试。
- 未改变源站 HTTP 路径：仍为 `/home_keyword_filter_settings`。
- 兼容旧版全局 `blocked_words`/`blocked_users`：首次登录同步时与源站规则取并集并上传，成功后清理旧键；网络不可用时持久化 pending，恢复网络后自动重试。
- 屏蔽设置保存增加串行化与 CSRF 刷新重试；网络恢复仅处理 pending 规则，JSON 业务错误不再误触发全局人机验证弹层。
