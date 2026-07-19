# ProactivePeopleAgent（主动的人）

一个「有自我意识、懂用户、主动关心」的生活伙伴 AI，而不是被动聊天工具。

核心价值：每天早上 8–10 点（用户本地时区）主动推送个性化内容 + 全天上下文感知主动跟进 + 任务苏格拉底式提醒 + 工作时「懂事」暂存消息。

设计方案见仓库根目录 `PP Agent.docx`。需求与任务流转见 `docs/sdd/`。

## 技术栈

- Spring Boot 3.5 + JDK 21
- LangChain4j（对话 / Tool）
- LangGraph4J（Multi-Agent，后续接入）
- Mem0：`pp.memory.provider=stub|mem0`（见 `docs/sdd/architecture.md`）
- 微信：`pp.wechat.provider=stub|official`；回调 `GET/POST /api/wechat/callback`

## 模块

| 模块 | 说明 |
|------|------|
| `pp-app` | 启动入口 |
| `pp-common` | 公共模型 |
| `pp-proactive-core` | 定时/规则/主动消息 |
| `pp-memory` | Mem0 三层记忆 |
| `pp-agent-graph` | LangChain4j / LangGraph4J |
| `pp-mcp` | MCP Client |
| `pp-wechat` | 微信网关 |
| `pp-monitor` | 忙碌度监测 |
| `pp-admin` | 后台配置 API |

## 本地启动

```bash
# 需要 JDK 21 + Maven 3.9+
mvn -pl pp-app -am spring-boot:run
```

健康检查：`GET http://localhost:8080/api/health`

启用 LLM 对话：

```bash
# 参考 .env.example
set PP_LLM_ENABLED=true
set OPENAI_API_KEY=sk-xxx
mvn -pl pp-app -am spring-boot:run
```

然后：`POST http://localhost:8080/api/chat`

```json
{"sessionId":"s1","userId":"u1","message":"你好"}
```

- `sessionId` 必填，用于多轮记忆
- `userId` 可选，默认 `anonymous`；每轮成功后写入短期记忆层

架构与模块边界见 `docs/sdd/architecture.md`。

## 文档驱动开发（SDD）

- PRD：`docs/sdd/prd.md`
- 看板：`docs/sdd/kanban.md`
- 规则：`.cursor/rules/sdd.mdc` / `.trae/rules/SDD.md`
