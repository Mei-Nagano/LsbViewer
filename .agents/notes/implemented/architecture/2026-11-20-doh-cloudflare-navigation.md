# Agent Note: DoH 下的 Cloudflare 验证导航

Status: implemented

## Problem

用户反馈 DoH 模式无法正常访问，并说明源站已经启用 Cloudflare。源码中安全验证优先通过 loadDataWithBaseURL 重放客户端取得的 HTML；缺少挑战特征便被当作通过，空白页、网络错误页和网页网络准备失败的提示页也可能触发成功。LocalDnsTunnel 在 DNS 解析之前启动 TCP 连接总时限，DoH 查询或备用端点切换耗时超过 10 秒时，即使返回地址也不会尝试连接。外站 DoH 关闭时，challenges.cloudflare.com 又落入系统 DNS。

DoH 地址来自独立的 doh_url 配置，源站来自 source_domain；未发现把源站页面地址传给 DoH 解析器。验证原组件与访问验证是两条链路，保留[站内 PoW 原组件](../../implemented/architecture/2026-11-20-original-pow-component.md)的用户点击流程。

## Decision

访问验证使用完整 WebView 导航加载源站同域首页，由浏览器取得当前挑战和响应上下文；图片、POST 接口等受阻请求在验证后仍由原调用方重试。停止挑战 HTML 重放和 DOM 裁剪。可见与隐藏 WebView 共用正向的源站内容识别，网络错误、HTTP 错误和未完成导航不算验证成功，重试由用户触发。关闭验证页时取消仍在等待网络准备的加载任务并销毁 WebView，避免迟到的导航访问已销毁页面。主页验证不能保证单独路径的 WAF 规则放行，重试后仍须由服务端响应判定。

Cloudflare 组件域名与源站域名共同遵循有效 DoH 策略，外站开关只控制其余网站；明确代理与 VPN 旁路保持现有优先级。先解析地址，再计算 TCP 连接时限。设置文案明确域名解析和 HTTPS 网页请求的区别。

## Alternatives considered

重放已取得的挑战 HTML 能减少一次请求，在浏览器 TCP 不通但应用 HTTP/3 可用时可显示首屏；它不保留真实导航的响应上下文，也不能解决后续挑战连接，因此改为完整浏览器导航。

只延长总超时改动很少，但仍会让解析消耗连接预算，备用解析器越慢越容易失败；选择分开两阶段计时。

所有网页无条件使用 DoH 可以覆盖挑战域名，但会忽略用户外站范围选择；只把必需的 Cloudflare 验证主机并入源站依赖。

## Consequences

真实浏览器导航保留挑战页面的响应上下文，页面不再被裁剪；正向内容识别避免把空白、网络错误和提示页当作通过。DoH 解析与连接独立计时，外站范围关闭不再使 Cloudflare 验证域名使用系统 DNS。代价是验证需重新取得完整网页，不能复用应用已经取得的挑战 HTML。

内置浏览器控制在当前会话返回不可用，因此未重新访问源站，也未确认手机实际网络行为。CONNECT 仍是 TCP 转发，不具备应用客户端的 HTTP/3 回退；DoH 负责域名解析，不能解除目标连接受阻。

## Verification

已检查 DoH 地址来源、代理与 VPN 优先级、验证页面加载和完成判断、取消后的协程与 WebView 清理。验证脚本通过 node --check 静态语法检查。包含最终生命周期修改的 assembleDebug（排除 test 与 connectedAndroidTest）成功，三个 ABI 安装包已生成。遵照用户约束，没有新增或执行测试，也没有手机运行确认。
