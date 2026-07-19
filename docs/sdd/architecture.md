# 模块架构与依赖方向

> 对齐《“主动的人” Agent 项目设计方案（V1.0）》§3。  
> **依赖方向**：上层可依赖下层，禁止反向依赖与环依赖。

```
pp-app（启动 / HTTP / 全局异常）
  └── 聚合所有业务模块

pp-admin ──► pp-proactive-core
pp-proactive-core ──► pp-agent-graph, pp-memory, pp-wechat, pp-mcp
pp-agent-graph ──► pp-memory, pp-mcp, pp-common
pp-monitor ──► pp-mcp, pp-common
pp-wechat / pp-mcp / pp-memory ──► pp-common
```

## 模块职责

| 模块 | 职责 | 当前切片 |
|------|------|----------|
| `pp-common` | 公共 API 模型、业务异常、工具 | `ApiResponse`、异常体系、`SensitiveDataMasker` |
| `pp-memory` | Mem0 三层记忆封装 | `MemoryService`；`stub` / `mem0`（OSS/Platform REST） |
| `pp-agent-graph` | LangChain4j / LangGraph4J | `Assistant`/`ChatService`；Supervisor 最小图（T-007） |
| `pp-proactive-core` | 定时/规则/主动消息 | 早间推送全链路（门禁/配额/R→P→G/微信） |
| `pp-wechat` | 微信收发与推送 | 回调验签、48h 窗口、客服/模板发送（`stub`/`official`） |
| `pp-mcp` | MCP 工具发现 | 接口 + Noop Stub |
| `pp-monitor` | 浏览器/IDE 忙碌度 | 接口 + Idle Stub |
| `pp-admin` | 后台配置 API | 元信息占位 |
| `pp-app` | 启动、配置分层、统一异常 | `GlobalExceptionHandler`、`InfraProperties` |

## 配置分层

| 文件 | 用途 |
|------|------|
| `application.yml` | 默认配置 + profile 激活 |
| `application-local.yml` | 本地调试日志 |
| `application-dev.yml` | 开发环境覆盖 |

基础设施（PostgreSQL / Redis / Mem0）通过 `pp.infra.*.enabled=false` 占位，**未启用时不影响启动**。

## 对话与记忆边界（T-002 深化）

1. `POST /api/chat` 要求 `sessionId` + `message`（`userId` 可选，默认 `anonymous`）
2. `Assistant` 使用 LangChain4j `ChatMemory`（按 `sessionId`）维持多轮上下文
3. 每轮成功后 `DefaultChatService` 调用 `MemoryService.add(SHORT_TERM, …)` 写入短期层
4. LLM 未启用 → `503 LLM_NOT_ENABLED`；参数非法 → `400 VALIDATION_ERROR`；上游失败 → `502 LLM_INVOCATION_FAILED`

## Mem0 记忆边界（T-003）

| provider | 说明 |
|----------|------|
| `stub`（默认） | 进程内内存，本地开发 |
| `mem0` + `mode=oss` | 自托管 REST：`POST /memories`、`POST /search` |
| `mem0` + `mode=platform` | 云服务：`POST /v3/memories/add/`、`POST /v3/memories/search/` |

三层语义通过 metadata **`pp_layer`**：`SHORT_TERM` / `MID_TERM` / `LONG_TERM`。  
切换示例：`pp.memory.provider=mem0`，并配置 `MEM0_BASE_URL` / `MEM0_API_KEY`（Platform 必填）。

## 微信网关边界（T-004）

### 回调

- URL：`GET/POST /api/wechat/callback`
- 验签：`sha1(sort(token, timestamp, nonce))` + timestamp 防重放（默认 ±300s，`pp.wechat.callback-timestamp-max-skew-seconds`）
- 入站 XML 解析后记录 `openId` + `CreateTime`，刷新客服消息 48h 窗口（`StateStore`，可 Redis）
- official：access_token 遇 40001/42001 清缓存并重试一次

### 48h 与降级策略（对齐设计方案）

| 条件 | 通道 | 说明 |
|------|------|------|
| 用户 48h 内有过互动 | 客服消息 `CUSTOMER_SERVICE` | `POST /cgi-bin/message/custom/send` |
| 超出 48h 且配置了 `template-id` | 模板消息 `TEMPLATE` | `POST /cgi-bin/message/template/send`，可绕过 48h（需公众号模板审核） |
| 超出 48h 且无模板 | 失败并返回明确文案 | 引导用户主动发消息重开窗口；**订阅消息**留给 T-009 |

调用入口：`WeChatMessageGateway.sendTextAuto(openId, content)` 按窗口自动选通道。  
发送日志字段：`openId`、`messageId`、`sentAt`、`contentLength` / `templateId`。

### 配置

```yaml
pp.wechat.provider: stub | official
pp.wechat.app-id / app-secret / token / template-id
```

安全自检 + 审计日志属于 **T-009 / REQ-013**，不在本切片范围。

## 早间推送边界（T-005）

编排入口：`MorningPushService.pushForUser` ← `MorningPushScheduler`（`pp.proactive.morning-push-enabled=true`）。

```
候选用户 → 门禁(用户时区窗口 / 防干扰 / 早间槽 / 日上限≤2)
         → tryReserveMorning（SETNX 早间槽 + INCR 日配额；失败回滚）
         → 定位授权检查（未授权降级「通用」）
         → Researcher(MCP weather/news，失败缓存降级)
         → Personalizer(Mem0 LONG_TERM 画像)
         → Generator(Assistant.complete 无记忆，否则模板)
         → 追加「今天别打扰」提示 → WeChat.sendTextAuto
         → 发送失败则 releaseMorning
```

| 配置 | 说明 |
|------|------|
| `pp.proactive.morning-push-enabled` | 默认 false |
| `pp.proactive.daily-push-limit` | 默认 2（全日主动通道共用） |
| `pp.proactive.users[]` | 候选：userId/openId/timezone/locationAuthorized/city/doNotDisturb |
| `pp.proactive.morning-scan-cron` | 宽扫描；是否推送由用户本地 8–10 决定；**早间每日仅 1 次** |
| `pp.state.store` | `memory`（默认）\|`redis` — 配额/早间槽/微信窗口 |
| `pp.state.redis-uri` | Lettuce URI |
| `pp.api.auth-enabled` / `pp.api.token` | Chat / Agent API `X-API-Key`（默认关闭） |
| `pp.agent.graph.max-iterations` | Supervisor 迭代上限，默认 5 |
| `pp.disturbance.cache-provider` | `memory`（默认）\|`redis` |
| `pp.disturbance.redis-uri` | Lettuce URI，仅 redis 模式 |

## Supervisor 图边界（T-007）

```
START → supervisor ⇄ researcher | personalizer | executor → END(FINISH)
```

- 规则型 Supervisor（关键词/缺口路由）；不替换 `DefaultChatService`
- Researcher → `McpToolClient`（weather/news）；Personalizer → `MemoryService.search(LONG_TERM)`；Executor → 模板（可选 `Assistant.complete` 润色）
- `maxIterations` + `CompiledGraph.setMaxIterations` 双保险
- 结束回写 `MemoryService.add(MID_TERM, "agent_graph:…")`
- 入口：`AgentGraphService.run` / `POST /api/agent/run`

## 防干扰边界（T-006，已验收）

- 模式：`NORMAL` / `QUIET` / `IMPORTANT_ONLY` / `CUSTOM_HOURS`（时段内仅 HIGH）
- 临时静音：`mutedUntil`（指令「今天别打扰」→ 用户本地日末）
- 忙碌度：`ActivityMonitor.HIGH_INTENSITY` 拦截 NORMAL，HIGH 仍可过
- **优先级分工**：本模块只消费 `PushPriority`；HIGH 判定（截止前 24h / `importantTopics`）由调用方（T-008 / REQ-010）传入
- 持久化：Mem0 `updatePreference(disturbance)`；缓存加速读
- 入口：微信快捷指令、`GET/PUT /api/preferences/disturbance?userId=`
- 配置兼容：`users[].do-not-disturb=true` 且无持久化偏好时视为 QUIET

分期：JSON 规则 CRUD → T-011；推送安全自检 → T-009；LLM ReAct 深化可挂在 T-007 后续切片。

## 早间推送边界（T-001 骨架，已由 T-005 承接）

1. `MorningPushWindowService` 用 `ZonedDateTime` + 用户时区判断 08:00–10:00（左闭右开）
2. `MorningPushScheduler` 扫描候选并调用 `MorningPushService`
