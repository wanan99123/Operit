---
fork: https://github.com/wanan99123/Operit
branch: feat/plan-steps-and-subagents
status: implementation-complete-pending-ci
---
# 选定聊天模型与精简页头

## 原状与目标

计划阶段此前会在首次请求、工具续接和摘要窗口计算处覆盖对话框模型。功能设置有计划生成和实施计划的独立模型入口。用量弹窗包含请求结构与压缩详情；子智能体结束后仍在页头菜单内保留详情，并打印尝试次数。

本轮主对话始终使用选定聊天模型，保留角色卡聊天绑定、子智能体独立模型、计划步骤及其持久化。功能设置移除计划生成与实施计划项，阶段只作工作流元数据。历史枚举和持久化字段仍可读取，不再参与模型选择。英文及中文工具描述同步修改。

## 修改范围

- EnhancedAIService、MessageCoordinationDelegate 与 MessageProcessingDelegate：移除阶段路由及首轮强制默认模型，摘要窗口使用所选配置，不再覆盖已保存回复的模型标识
- SelectedRequestModel：主请求使用聊天选择，子请求使用 SUBAGENT 绑定；删除 AutomaticPlanModelRouting 和 PlanModelRouting 及旧路由测试
- ContextDiagnosticsDialog：仅保留用量概览和 Token 速率、首字延迟；不再显示请求结构及压缩卡片，诊断采集功能不变
- RequestPerformanceStore：按会话记录最近一次主模型请求的性能。首字延迟从请求发起到首个非空内容片段；Token 速率为输出 Token 数除以首字后的模型输出时间。流中随用量回调更新，结束后保留最终值；新模型请求重置，不包含工具执行耗时，不计入子请求。取消和错误不伪造完成统计，缺失值显示未记录
- PlanStepsButton：一轮存在运行或重试中的智能体时保留该轮全体详情；最后一个智能体终止后隐藏整轮子智能体，仅保留计划。下一轮开始重新显示。不显示尝试次数，重试状态及错误原因保持可见

## 验证与待验收

新增 SelectedRequestModelTest、RequestPerformanceStoreTest、HeaderSubagentRoundTest，覆盖聊天选择、子模型隔离、无样本、计时公式、会话隔离、取消、重置、全轮结束与下一轮。删除旧路由行为测试，不沿用与新需求冲突的断言。

静态检查包括差异空白、资源引用、删除路由引用、初始与工具请求接线检查。编译和 JVM 测试由 GitHub Actions 执行，最终结果以 Actions 为准。设备验收待验证：新聊天首轮选择非默认模型，更新计划并执行工具后模型保持不变；弹窗仅显示概览；两子任务逐个完成后子智能体区域消失，再运行一批时重新出现；重试 2 次以上不显示次数；切换会话与删除会话不串性能数据。性能为进程内最新请求统计，不提供跨重启历史性能。

[DONE]
