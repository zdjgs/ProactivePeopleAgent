# 变更日志

## [v1.8] - 2026-07-19
### 变更类型：新增
- 影响 REQ：REQ-008
- 变更描述：T-007 LangGraph4J Supervisor 最小图：规则路由 Researcher/Personalizer/Executor；`maxIterations`；结果写 Mem0 MID_TERM；`POST /api/agent/run`（与 Chat 同套 X-API-Key）；不替换 DefaultChatService
- 新增任务：无（T-007 👀）
- 原因：设计方案 §4 复杂问题多 Agent + ReAct 骨架

## [v1.7] - 2026-07-19
### 变更类型：修改
- 影响 REQ：REQ-006、REQ-010
- 变更描述：T-006 有条件验收通过。明确本切片只建 `PushPriority` 策略闸门；HIGH 判定（截止前 24h / importantTopics）写入 REQ-010 与看板 T-008 验收标准
- 回退任务：无
- 新增任务：无（承接既有 T-008）
- 原因：审查意见——避免验收口径含糊，接受 HIGH 判定分期

## [v1.6] - 2026-07-19
### 变更类型：优化
- 影响 REQ：REQ-002、REQ-003、REQ-004、REQ-005
- 变更描述：审查前硬化——早间每日一次 + 配额预占；`StateStore`（memory/redis）外置配额与 48h 窗口；ChatMemory 绑定 `userId:sessionId` + `/api/chat` 可选 `X-API-Key`；Mem0 严格分层过滤；微信 access_token 失效重试与回调 timestamp 防重放
- 新增任务：T-016（👀）
- 原因：已完成模块排查发现连发、跨实例状态、会话串话等隐患

## [v1.5] - 2026-07-19
### 变更类型：新增
- 影响 REQ：REQ-006
- 变更描述：T-006 防干扰：QUIET / IMPORTANT_ONLY / CUSTOM_HOURS；Mem0 长期偏好 + memory/redis 缓存；门禁集成忙碌度与「今天别打扰」；微信快捷指令 + REST；T-005 视为验收通过
- 新增任务：无（T-006 👀）
- 原因：设计方案 §4 用户设置防干扰模式

## [v1.4] - 2026-07-19
### 变更类型：新增
- 影响 REQ：REQ-005
- 变更描述：T-005 早间主动对话：按用户时区窗口、门禁（勿扰/日上限）、定位授权降级、Researcher→Personalizer→Generator、微信推送；T-004 视为验收通过
- 新增任务：无（T-005 👀）
- 原因：设计方案 §4 定位+定时主动对话

## [v1.3] - 2026-07-16
### 变更类型：新增
- 影响 REQ：REQ-004
- 变更描述：T-004 微信网关：回调验签、入站刷新 48h 窗口、官方客服/模板 API、`sendTextAuto` 降级策略；`pp.wechat.provider=stub|official`
- 新增任务：无（T-004 👀）
- 原因：Week1-2 微信客服消息网关深化

## [v1.2] - 2026-07-16
### 变更类型：新增
- 影响 REQ：REQ-003
- 变更描述：T-003 Mem0 REST 客户端：OSS/Platform 双模式、三层 metadata、provider 配置切换、MockWebServer 单测；T-001/T-002 用户验收通过移入已完成
- 新增任务：无（T-003 👀）
- 原因：Week1-2 Mem0 接入

## [v1.1] - 2026-07-16（深化完成）
### 变更类型：优化
- 影响 REQ：REQ-001、REQ-002
- 变更描述：T-001/T-002 切片 A+B 深化完成。A：统一异常、配置分层、时区窗口、infra 占位、架构文档。B：多轮 ChatMemory、短期记忆落库、结构化错误、单测。看板 🔄→👀。
- 原因：用户要求 A/B 一起做并写仔细深化

## [v1.1] - 2026-07-16（复审补记）
### 变更类型：修改
- 影响 REQ：REQ-001、REQ-002
- 变更描述：复审认定 T-001/T-002 未全面深化（模块壳/单轮透传）。REQ-001/002 状态退回「进行中」，并追加深化验收标准；看板 T-001/T-002 👀→🔄。
- 回退任务：T-001、T-002
- 原因：用户要求结合文档写仔细深化，禁止停留表面

## [v1.1] - 2026-07-16
### 变更类型：修改
- 影响 REQ：全部（旧 REQ-001~009 废弃，新 REQ-001~015）
- 变更描述：按《“主动的人” Agent 项目设计方案（V1.0）》重写 PRD 与看板。模块对齐七大代码模块（proactive-core / memory-layer / agent-graph / mcp-integration / wechat-gateway / admin-console / monitor-module）。看板按 Week1–6 路线拆分 T-001~T-015；启动 T-001/T-002（骨架 + LangChain4j）。
- 回退任务：旧 README 粗提任务整表替换
- 新增任务：T-001 ~ T-015
- 原因：设计方案已明确技术栈与功能点，旧 PRD 粒度不足、模块命名不一致

## [v1.0] - 2026-07-16
### 变更类型：新增
- 影响 REQ：REQ-001 ~ REQ-009（旧）
- 变更描述：SDD 初始化。从 README 产品定位提取 9 个 REQ。
- 新增任务：T-001 ~ T-009（旧）
- 原因：建立文档驱动开发流程
