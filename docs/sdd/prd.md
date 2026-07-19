# PRD - 主动的人（ProactivePerson）Agent

## 版本信息
- 当前版本：v1.1
- 最后更新：2026-07-16
- 状态：draft
- SDD 对齐：依据《“主动的人” Agent 项目设计方案（V1.0）》（`PP Agent.docx`）重写；REQ 状态以代码现状为准
- 代码现状：Week1 多模块骨架 + LangChain4j 条件接入已落地（T-001/T-002 待审查）

> **文档定位**：本 PRD 是需求索引与决策记录。细节以设计方案为准；后续设计文档可放在 `docs/` 并在此交叉引用。

---

## 模块说明

| 模块 | 代码位置 | 说明 |
|------|----------|------|
| infra / pp-app | `pp-app` | Spring Boot 启动入口、配置、健康检查 |
| common | `pp-common` | 公共模型、异常、工具 |
| proactive-core | `pp-proactive-core` | 定时/事件触发、规则引擎、主动消息生成 |
| memory-layer | `pp-memory` | Mem0 三层记忆封装（短/中/长） |
| agent-graph | `pp-agent-graph` | LangGraph4J Multi-Agent（Supervisor/Researcher/Personalizer/Reflector/FollowUp） |
| mcp-integration | `pp-mcp` | MCP Client 动态发现 Tools/Skills/Prompts |
| wechat-gateway | `pp-wechat` | 微信客服消息 / 模板消息 / 主动推送 |
| monitor-module | `pp-monitor` | 浏览器/IDE MCP 忙碌度监控 |
| admin-console | `pp-admin` | 后台：推送规则、Prompt、画像标签、技能开关 |

---

## 需求列表

### infra（骨架与基础）

### REQ-001: 项目骨架（Spring Boot 3 + 多模块）
- 优先级：P0
- 状态：已完成
- 模块：infra
- 描述：多模块骨架 + 统一异常/配置分层/时区窗口门禁/infra 占位；`mvn -pl pp-app -am test` 通过
- 关联文件：根 `pom.xml`、`pp-app/**` 及各 `pp-*` 模块
- 关联文档：`PP Agent.docx` §2 技术栈、§3 模块划分、§5 Week1-2
- 验收标准：
  1. `mvn -pl pp-app -am package` 通过
  2. 应用可启动并暴露健康检查
  3. 密钥不入库（`.env.example` 占位）
  4. 模块目录与设计方案七大模块对齐
  5. **（深化）** 统一异常与错误响应；`application-{local,dev}.yml` 配置分层；各模块职责与依赖方向有简要说明（README 或 docs）
  6. **（深化）** 调度扩展点具备「用户时区窗口」校验骨架（`ZonedDateTime`），而非空 cron 打日志
  7. **（深化）** 技术栈占位明确：PostgreSQL/Redis/Mem0 连接配置项与「未启用时不影响启动」策略写清

### REQ-002: LangChain4j 基础接入
- 优先级：P0
- 状态：已完成
- 模块：agent-graph / infra
- 描述：多轮 ChatMemory + `ChatService` 短期层落库 + 结构化错误体 + 单测覆盖
- 关联文件：`pp-agent-graph/**`、`pp-app` 配置与 `ChatController`
- 关联文档：`PP Agent.docx` §2、§4 技术功能点（SpringBoot3 + LangChain4j）+ §4 三层记忆（对话结束写短期层边界）
- 验收标准：
  1. 依赖 `langchain4j` + `langchain4j-open-ai`（可走兼容 OpenAI 协议的网关）
  2. 存在示例 `Assistant` AiService 接口
  3. 未配置 Key 时启动不失败；配置后可走通最小对话
  4. **（深化）** 支持多轮会话记忆（ChatMemory 键=`userId:sessionId`）；对话结束后调用 `MemoryService.add(SHORT_TERM, …)` 边界接通
  5. **（深化）** 超时/上游错误有结构化错误体与日志（API Key 脱敏）；温度等模型参数可配置
  6. **（深化）** 有「LLM 关闭 → 503」「参数非法 → 400」测试；可选：对 ChatModel 做单元/切片测试，不依赖真实外网
  7. **（硬化 T-016）** `pp.api.auth-enabled=true` 时 `/api/chat` 须 `X-API-Key`；默认关闭

### REQ-003: Mem0 客户端封装
- 优先级：P0
- 状态：已完成
- 模块：memory-layer
- 描述：`Mem0RestClient` 支持 OSS/Platform；`pp_layer` **严格过滤**（无分层 metadata 不命中）；stub/mem0 可切换
- 关联文件：`pp-memory/**`
- 关联文档：`PP Agent.docx` §4 对话历史三层记忆
- 验收标准：
  1. 提供 add/search/updatePreference 抽象 API
  2. 明确短期/中期/长期分层语义（metadata.pp_layer）；search 严格按层过滤
  3. Stub 与 Mem0 实现可单测；`pp.memory.provider=mem0` 切换真实 Server
  4. Platform 模式需 API Key；对话记忆写入失败不阻断聊天（降级日志）

### REQ-004: 微信客服消息网关
- 优先级：P0
- 状态：已完成
- 模块：wechat-gateway
- 描述：回调验签（timestamp 防重放）+ 48h 窗口（StateStore）+ token 失效重试；`sendTextAuto` 降级
- 关联文件：`pp-wechat/**`、`pp-common/**/state/**`
- 关联文档：`PP Agent.docx` §4 接入微信 / 48h 窗口；`docs/sdd/architecture.md` 微信网关边界
- 验收标准：
  1. `GET/POST /api/wechat/callback` 验签通过后可配置服务器；入站解析 openId 并刷新窗口
  2. 发送日志含 openId + sentAt（及 messageId）
  3. 文档与代码标明：48h 内客服、超窗模板、无模板则失败并提示；订阅消息留给 T-009
  4. `pp.wechat.provider=official` 走官方 API；40001/42001 清缓存重试一次（MockWebServer 覆盖）
  5. 默认 stub，无密钥可启动
  6. **（硬化 T-016）** 回调 timestamp 默认 ±300s；窗口经 `pp.state.store`

---

### proactive-core（主动触发）

### REQ-005: 早间主动对话（本地时区 8–10 点）
- 优先级：P0
- 状态：已完成
- 模块：proactive-core
- 描述：按用户本地时区窗口调度；门禁（防干扰/日上限）；**早间每日一次预占**；定位降级；R→P→G；微信推送
- 关联文件：`pp-proactive-core/**/morning/**`、`quota/**`、`gate/**`、`schedule/MorningPushScheduler.java`
- 关联文档：`PP Agent.docx` §4 定位+定时主动对话；`docs/sdd/architecture.md` 早间推送
- 验收标准：
  1. 调度使用 `ZonedDateTime` + **每位用户**时区判定 08:00–10:00
  2. 推送前过门禁：勿扰跳过、日上限默认 ≤2；**同一用户本地日早间最多 1 次**（发送失败释放预占可重试）
  3. 内容经 Researcher（MCP）→ Personalizer（Mem0 长期）→ Generator（LLM `complete` 无记忆或模板）
  4. 定位须授权；未授权降级通用语境仍可推送
  5. 默认 `morning-push-enabled=false`；候选用户来自 `pp.proactive.users`
  6. **（硬化 T-016）** 配额/早间槽经 `StateStore`（memory|redis）可多实例共享

### REQ-006: 防干扰模式
- 优先级：P0
- 状态：已完成
- 模块：proactive-core / memory-layer
- 描述：安静 / 仅重要 / 自定义时段；Mem0 长期偏好 + 缓存；门禁查忙碌度；消息带「今天别打扰」；微信快捷指令
- 关联文件：`pp-proactive-core/**/disturbance/**`、`gate/MorningPushGate.java`、`web/DisturbancePreferenceController.java`
- 关联文档：`PP Agent.docx` §4 用户设置防干扰模式；`docs/sdd/architecture.md` 防干扰
- 验收标准：
  1. 三种模式可配置并持久化到长期偏好
  2. 策略闸门：调用方传入 `PushPriority.HIGH` 时在 IMPORTANT_ONLY / CUSTOM_HOURS 勿扰窗内可放行；**HIGH 判定（importantTopics 匹配、任务截止前 24h）由调用方负责，本切片只建闸门**（闭环见 REQ-010 / T-008）
  3. Redis 缓存偏好降低延迟（默认 memory TTL，可切 `pp.disturbance.cache-provider=redis`）

### REQ-007: 动态推送规则引擎
- 优先级：P1
- 状态：待开发
- 模块：proactive-core / admin-console
- 描述：后台配置 JSON 规则（条件+Prompt+消息类型），内存加载，支持优先级与热更新
- 关联文档：`PP Agent.docx` §4 后台配置个性化发起对话
- 验收标准：
  1. 规则可 CRUD 并落库
  2. 冲突时按优先级选择
  3. 修改后无需重启即可生效（Config/Nacos 或等效热更新）

---

### agent-graph（多 Agent）

### REQ-008: LangGraph4J Multi-Agent + ReAct
- 优先级：P0
- 状态：待开发
- 模块：agent-graph
- 描述：Supervisor 分派 Researcher/Personalizer/Executor；复杂问题走 ReAct；`maxIterations` 防死循环
- 关联文档：`PP Agent.docx` §4 复杂问题多Agent+ReAct
- 验收标准：
  1. CompiledStateGraph 可运行最小图
  2. 工具调用有迭代上限
  3. 结果可回写 Mem0

### REQ-009: Self-Reflection 对话特性调整
- 优先级：P1
- 状态：待开发
- 模块：agent-graph / memory-layer
- 描述：用户回复后异步 Reflection，更新长期「对话风格偏好」；冷启动前 10 次固定风格
- 关联文档：`PP Agent.docx` §4 技术功能点7
- 验收标准：
  1. Reflection 异步，不阻塞主回复
  2. 风格偏好写入长期层并注入后续 Prompt
  3. 冷启动策略生效

### REQ-010: 苏格拉底式任务跟进
- 优先级：P0
- 状态：待开发
- 模块：agent-graph / proactive-core
- 描述：从对话抽取 Task；截止前 24h / 习惯时间触发 FollowUpAgent；提问式引导
- 关联文档：`PP Agent.docx` §4 功能点4
- 验收标准：
  1. Task 结构化输出（JsonSchema）
  2. 提醒文案为苏格拉底式，非机械催促
  3. 用户可标记完成/暂缓
  4. **（承接 REQ-006）** 触发主动跟进时须判定并传入 `PushPriority`：任务截止前 24h 或命中用户 `importantTopics` → `HIGH`，否则 `NORMAL`；经 `DisturbancePolicy` / 门禁后再推送

---

### mcp-integration / monitor

### REQ-011: 动态 MCP / Skills / Prompt
- 优先级：P1
- 状态：待开发
- 模块：mcp-integration / admin-console
- 描述：后台改 Prompt/Skill 后动态注册，Agent 下次调用发现最新工具
- 关联文档：`PP Agent.docx` §4 技术功能点5
- 验收标准：
  1. Tools/Prompts 可动态注册
  2. 配置变更可触发 list changed / 等效刷新

### REQ-012: 高强度工作时消息暂存
- 优先级：P1
- 状态：待开发
- 模块：monitor-module / memory-layer
- 描述：MCP 监测浏览器/IDE；高强度则暂存待推送；空闲 >10min 再汇报；默认 5 分钟轮询
- 关联文档：`PP Agent.docx` §4 通过MCP监测浏览器、IDE
- 验收标准：
  1. 须用户显式授权，MCP Server 仅本地
  2. 高强度判定可配置阈值
  3. 暂存进短期层并标记「待汇报」

---

### wechat-gateway（通道）

### REQ-013: 微信主动推送（客服 + 模板/订阅）
- 优先级：P0
- 状态：待开发
- 模块：wechat-gateway
- 描述：完整主动推送链路；48h 内客服消息，超窗模板/订阅；发送前安全自检 + 审计日志
- 关联文档：`PP Agent.docx` §4 接入微信
- 验收标准：
  1. 支持文本（后续扩展语音/图片/卡片）
  2. 48h 策略与降级路径明确
  3. 安全自检不通过则不发送并记审计

---

### memory-layer（记忆深化）

### REQ-014: 三层记忆读写与注入
- 优先级：P0
- 状态：待开发
- 模块：memory-layer
- 描述：会话结束→短期；小时/天总结→中期；重要事实→长期 Graph；查询三层融合注入 Prompt
- 关联文档：`PP Agent.docx` §4 三层记忆
- 验收标准：
  1. 三层写入路径可观测
  2. 查询融合结果可注入主动对话与跟进
  3. 向量存储对接 PGVector（可分期）

### REQ-015: Onboarding + 对话学习画像
- 优先级：P1
- 状态：待开发
- 模块：memory-layer / agent-graph
- 描述：首次 Onboarding + 对话自动学习 + Reflection 持续优化画像
- 关联文档：`PP Agent.docx` §1 用户画像动态构建
- 验收标准：
  1. 首次引导可采集时区/偏好/兴趣
  2. 对话中可增量更新画像
  3. 主动推送必须能读到画像或明确默认策略

---

## 变更记录

| 日期 | 版本 | 变更内容 | 影响 REQ | 操作人 |
|------|------|----------|----------|--------|
| 2026-07-19 | v1.7 | T-006 有条件验收：HIGH 判定分期写入 REQ-010/T-008 | REQ-006, REQ-010 | AI |
| 2026-07-19 | v1.6 | T-016 审查前硬化：早间每日一次/StateStore/鉴权/Mem0/微信 | REQ-002~005 | AI |
| 2026-07-19 | v1.5 | T-006 防干扰三模式 + 缓存 + 快捷指令；T-005 验收通过 | REQ-006, REQ-005 | AI |
| 2026-07-16 | v1.0 | SDD 初始化（基于 README 粗提） | REQ-001~009（旧） | AI |
| 2026-07-16 | v1.1 | 按设计方案 V1.0 全面重写 PRD：模块对齐七大代码模块；REQ 重编号为 15 条 | 全部 | AI |
| 2026-07-19 | v1.4 | T-005 早间主动对话链路 | REQ-005 | AI |
| 2026-07-16 | v1.3 | T-004 微信网关：验签回调、48h、客服/模板 | REQ-004 | AI |
| 2026-07-16 | v1.2 | T-003 Mem0 REST 客户端（OSS/Platform） | REQ-003 | AI |
| 2026-07-16 | v1.1 | T-001/T-002 用户验收通过 | REQ-001, REQ-002 | 用户 |
| 2026-07-16 | v1.1 | 复审 T-001/T-002：未全面深化，REQ-001/002 退回进行中并补深化验收标准 | REQ-001, REQ-002 | AI |
