# Kafka 审计走 acks=all + 失败兜底文件,不走本地消息表事务

场景 4 用 Kafka 异步采集 SSH 命令/文件传输/登录登出的操作审计。设计阶段需要决定:业务操作与审计消息之间是否需要事务一致性。

## Considered Options

- **Fire-and-forget**:`KafkaTemplate.send(...)` 不等结果,Kafka 抖动时审计丢失。零侵入、性能最好,但审计是"尽力而为",合规叙事弱。
- **同步发送 + 失败兜底文件(选定)**:`send().get(timeout=3s)` 或 ListenableFuture 回调;Producer 配 `acks=all` + `min.insync.replicas=2` + `retries=3`;Kafka 不可用时落本地兜底文件并触发告警,业务**不**回滚、**不**阻塞主响应。
- **本地消息表 + DB 事务**:业务操作与"待发审计消息"在同一个事务写 DB,后台扫描发送。一致性最强,但杀掉了 Kafka 高吞吐这个面试卖点——本地消息表会成为瓶颈;且审计场景对"消息可能延迟到达"不敏感,引入事务一致性是过度设计。

## Consequences

- AOP 切面用 `@AfterReturning` + `@AfterThrowing` 双切面,审计消息携带 `outcome=SUCCESS/FAILURE` 字段——成功操作和失败/未授权访问都进入审计流,符合"安全审计 = 全量记录尝试"的合规语义。单 `@AfterReturning` 切面会漏掉失败事件。
- 失败兜底文件 schema 与正常 Kafka 消息保持一致,Kafka 恢复后由独立 job 批量回放。
- 业务 SLA 不被 Kafka 抖动影响:即使 Kafka 全挂,业务依然返回成功(审计先落兜底文件),符合"审计是旁路"的设计原则。
- 与 RocketMQ 业务消息(场景 1/2/3/5)形成对比叙事:**业务消息要事务,审计数据流不要事务**——这是 RocketMQ vs Kafka 选型分工的核心区别,也是 4 年经验该有的判断。
- 若未来场景升级为"金融级对账审计"需要强一致,切换路径是清晰的:加一张本地消息表 + 把 AOP 切面改为事务内同步写 DB、事务外异步发 Kafka。本 ADR 不锁死这条进化路径,只声明当前场景不需要。
