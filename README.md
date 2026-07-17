# Java Agent Lean
## AI Chat Service
基于SpringBoot + Spring AI 构建企业级AI Chat 服务示例，支持普通聊天、流式输出，Prompt 文件化管理，多轮对话记忆，结构化输出、Tool Calling、数据库查询和人工确认
### 技术栈
- Java 17
- SpringBoot 
- SpringAI
- OpenAI-compatible API
- Mybatis-plus
- SSE
- MySQL

### 核心功能
#### 1. 普通聊天
聊天时，根据不同的场景，提供不同 Prompt 模板
#### 2. 流式输出
基于Flux和SSE实现模型流式返回
#### 3. Prompt 文件化管理
不同的场景的 prompt 写在 resources/prompts 下
支持多个场景
- general
- interviewer
- java_teacher
- summary (结构化输出)
- tool_assistant

#### 4.多轮对话记忆
通过 ChatMemory 实现，根据 conversationId 区别不同会话，支持上下文记忆

#### 5.Tool Calling
模型可以根据用户的问题，调用 Java 工具方法，例如查询订单

#### 6.数据库查询
OrderTools 调用 OrderInfoMapper 去查询订单信息

#### 7. Human-in-the-loop 人工确认
高风险操作不直接执行，先创建待确认操作
