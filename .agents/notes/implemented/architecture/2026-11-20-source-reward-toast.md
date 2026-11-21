# Agent Note: 源站称号互动奖励使用 toast

Status: implemented

## Problem

用户要求与真富可敌国称号用户互动获奖时用 toast 提示。2026-11-20 读取源站 /topic/34104 和页面声明的 plugins.js?v=cf4c87457bc9，post_random_rewards 模块从 post_random_rewards_popup Cookie 读取用户、活跃度、总积分、topic/reply/sofa/ur_reply 分项及二次编码的称号。ur_reply 是互动奖励，不是 donate.lucky 打赏抽奖。原生回复只显示回复成功，丢失源站弹窗信息。

## Decision

成功回复后按源站约束解析通知，核对响应页面的当前用户、分项上限和总额；只展示服务器实际奖励，不自行判断中奖。使用现有回复结果 toast，优先说明称号互动获得的积分，合并其他奖励的总额。消费后同时删除会话和 WebView 的对应 Cookie，避免重复。

源站网页编辑器继续由原脚本判定奖励；应用观察已显示的奖励弹窗，将其原文交给现有 toast 后关闭弹窗、清除通知。只在可信源站页面启用。

## Alternatives considered

按称号和本地概率推测奖励实现简单，但服务器才拥有中奖和积分数据，会产生虚假到账信息，因此不使用。

保留原弹窗最贴近网页交互，但打断阅读且不符合用户明确的 toast 要求。

## Consequences

原生回复和源站编辑器能显示真实奖励，通知只消费一次；源站若改变 Cookie 格式需同步协议。遵循源站界限拒绝损坏或不属于当前用户的通知。只做源码检查与编译，不发送真实互动、不执行测试。

## Verification

已完成相关调用路径和生命周期的源码检查。内嵌 JavaScript 已通过仅语法检查；Android debug 编译排除 test 与 connectedAndroidTest。没有新增或执行测试，没有设备运行或真实账号互动验证。
