# 消费幂等用 jobId + 数据库 CAS,而非 messageId 去重表

引入 RocketMQ 事务消息后,需要在消费者侧防重复消费。直觉的方案是把 `messageId` 写进 Caffeine/Redis 去重表(收到消息先 check)。我们没有这么做——选择以业务键 `jobId` 为锚点,通过一条 CAS UPDATE 占据作业。

## Considered Options

- **`messageId` + Caffeine/Redis 去重表**:零业务侵入、看似最简单。但 `messageId` 在事务消息半提交→commit、producer 重试、broker 重投这些场景下**会变化**——同一份业务作业可能被多个 messageId 包裹,按 messageId 去重等于漏过真正的重复消费。
- **`jobId` + CAS UPDATE(选定)**:`UPDATE deployment_job SET status='IN_PROGRESS' WHERE id=:jobId AND status='PENDING'`,受影响行数为 0 即代表"已被前一次处理(或正在处理)",直接 ACK;为 1 才执行 SSH。锚点是不可变的业务身份,不受 MQ 内部机制影响。
- **`INSERT IGNORE` 进幂等表**:也能用业务键,但要多维护一张表;CAS UPDATE 直接打在 `deployment_job` 主表上,顺手把状态机推进一格,更紧凑。

## Consequences

- 服务类命名为 `JobAcquisitionService`(或语义类似的名字),**不是** `MessageIdempotentService`——名字里不再出现 "messageId" 这个误导词。
- 幂等不依赖任何外部缓存(Caffeine/Redis),纯靠数据库行锁与状态机推进,作品集项目里也省掉了一个组件。
- 与 HTTP 入口的 `(deployment_record_id, job_type, client_request_id)` 唯一索引(见 [CONTEXT.md](../../CONTEXT.md) "客户端请求 ID")形成**双层防重**:前端重复点击在 Controller 层就被挡掉、消费侧重投在 CAS UPDATE 处被挡掉。
- 面试讲法的高级度更高:能讲清楚为什么 messageId 不行(事务消息内部机制 + producer 重试 + broker 重投三段),比"列举两种方案"更显判断力。
