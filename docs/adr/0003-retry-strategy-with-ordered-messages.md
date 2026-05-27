# 顺序消息下用应用层重试 + 显式 DLQ,放弃 RocketMQ 内置 maxReconsumeTimes

引入 RocketMQ 顺序消息(按 `deploymentRecordId`,见 ADR-0001)后,直接使用 RocketMQ 内置的 `maxReconsumeTimes` + 自动 DLQ 机制会破坏顺序:失败消息被丢到 retry topic 延迟投回时,已经排在了同 queue 后续消息之后。我们选择放弃内置重试,在 consumer 应用层做分级处理。

## Considered Options

- **内置 `maxReconsumeTimes` + 自动 DLQ(`%DLQ%consumerGroup`)**:配置零成本,RocketMQ 面试热点。但与顺序消息互斥——retry topic 延迟投回会让 `STOP→START→UPDATE` 这种连续作业被打乱;另外不区分基础设施级 vs 业务级失败,后者(端口被占用、jar 路径错)重试再多也不会成功,白白拖慢 queue。
- **完全不重试,失败立即 DLQ**:顺序保留、简单,但放弃了对 SSH 断连这类**瞬时故障**的自动挽救能力,可用性差。
- **混合策略(选定)**:
  - **基础设施级失败**(SSH 超时、网络抖动) → consumer **内部短同步重试**(2-3 次小退避,在同一条消息的消费动作内完成,对 MQ 总是 ACK 成功),顺序不破。
  - **业务级失败 / 内部短重试用尽** → 数据库把作业 `status='FAILED'` 并写 `error_message`,**ACK 原消息**让 queue 继续推进;同时显式 `rocketMQTemplate.syncSend("%DLQ%deploy-job", ...)` 投递死信。
  - DLQ 消费方为 `MQMonitorController` + 前端 DLQ 页面;**手动 retry 的语义是新建一份带新 jobId 的作业**(走 HTTP 入口),不是把 DLQ 消息再投回原 Topic——后者会再次破顺序。

## Consequences

- consumer 代码必须区分异常类型(`SshTransientException` vs `JobFailureException` 等),不能用一刀切的 `try/catch RuntimeException → throw`。
- 不再依赖 RocketMQ 自动 DLQ Topic(`%DLQ%consumerGroup`);改用业务自定义 DLQ Topic 名 `%DLQ%deploy-job`,语义更可控。
- 简历/面试叙事更高级:能讲"为什么放弃 `maxReconsumeTimes`"——三段(机制层:retry topic 延迟投回破顺序;业务层:不区分故障性质;运维层:DLQ 时机由应用决定更可观测)。
- **若未来需要演示 RocketMQ 内置重试 + 自动 DLQ 机制**(纯面试用),可以在不需要顺序约束的 topic 上(如审计日志旁路、配置广播)单独配置,作为对比演示,不污染主流程。
- CONTEXT.md 的 Flagged ambiguities #2(顺序消息与失败重试的冲突)和 #3(死信处理流程)由本 ADR 一并 resolve。
