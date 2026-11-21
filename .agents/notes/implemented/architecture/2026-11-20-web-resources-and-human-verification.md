# Agent Note: 帖子资源共用网络通路与新版人机验证

Status: implemented

本文的原生 PoW 决策已由[源站原组件方案](2026-11-20-original-pow-component.md)部分取代；帖子资源共用网络通路的决策继续保留。Cloudflare 访问验证的完整导航与 DoH 连接修复见[访问验证记录](2026-11-20-doh-cloudflare-navigation.md)。

## Problem

帖子正文经 LsbClient 获取，源站头像、称号 CSS 却由 WebView 的 CONNECT 隧道单独获取。该隧道只转发 TCP，不具备应用请求已有的 Cronet HTTP/3 回退，也不直接共享 SessionCookieJar。正文成功不意味着这些资源可达。

2026-11-20 读取源站 /login 与 /app/assets/plugins.js，登录表单使用 data-human-provider="pow" 和 human_verification_pow_challenge。现有解析器仅识别 CAP 与 native captcha，因而拒绝有效登录页。

同主题审计：工作区不存在 .agents/notes 或其他决策记录，没有旧篇需要更新或归档。

## Decision

帖子 WebView 仅将可信源站 /app/ 下的 GET 子资源交给当前会话的 LsbClient HTTP 客户端，保留本地静态资产优先、POST/主框架与外站原有行为。响应以流交还 WebView，并保留状态、类型、缓存与跨域响应头。网络失败返回失败响应，不悄悄改用另一条网络策略。

新增明确的 Pow 验证模型和共享原生验证按钮，按源站算法计算 SHA-256(challenge + 16 位十六进制 proof)，按位检查前导零。尊重挑战有效期、源站迭代上限和协程取消，提交源站要求的 challenge/proof 字段。所有使用同一解析器的登录、回复和源站表单同步接入，保留 CAP 与旧问答。

## Alternatives considered

只调整 DoH 解析或 CONNECT 并发能保持 WebView 原生请求行为，但不能补齐 TCP 受阻时的 HTTP/3 回退与会话差异，故不选。

直接加载完整源站登录页能自动跟随站点变化，但会替换现有原生登录与凭据保存流程。加载完整 plugins.js 又会带入大量无关页面操作；已取得明确的小型 PoW 协议，采用对应原生实现。

## Consequences

资源与帖子共用网络和会话，保留本地静态资产优先、受控 URL 范围及失败状态；专门处理展示资源带来少量网络适配代码，第三方 iframe 仍沿用原有 WebView 策略。LsbClient 保留调用方声明的 Accept，以免将 CSS、图片请求误声明为 HTML。

PoW 不依赖额外网络组件，验证结果在挑战变化与过期时失效，原有 CAP 与问答继续支持。协议算法由客户端维护；源站若更换 challenge 格式或算法，需要重新核对公开表单与脚本。无法取得新挑战的通用表单提示重新打开页面。

## Verification

已检查源站 HTML/脚本、验证码各调用点、请求范围、会话与响应流关闭关系。assembleDebug（排除 test 与 connectedAndroidTest）已成功生成三个 ABI 安装包，笔记目录、格式与归档校验通过。遵照用户约束，没有新增或执行测试，也没有手机运行确认；静态检查和编译不等于实际网络条件下的行为验证。
