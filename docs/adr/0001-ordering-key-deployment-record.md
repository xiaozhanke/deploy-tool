# 顺序消息按 deploymentRecordId 而非 serverId 分队列

引入 RocketMQ 顺序消息时，直觉的串行化单位是"同一台服务器上的部署任务"——即用 `serverId` 作哈希键。我们选择了更细的粒度：`deploymentRecordId`。

## Considered Options

- **`serverId`**：直觉、简历话术好讲，但一台机器上承载多个独立应用是常态（不同端口、不同工作目录、不同 PID），让它们彼此排队没有业务意义；真正的冲突只发生在对同一份部署记录并发触发 STOP/START/UPDATE 时。
- **`(serverId, port)` 复合键**：粒度合适但 port 是配置项，会被 UPDATE 类作业改写；in-flight 作业的 hash 漂移会导致顺序丢失。
- **`deploymentRecordId`（选定）**：业务上就是"一个被托管的应用"的不可变身份，hash 稳定，串行化的范围正是真实冲突域。

## Consequences

- 选择器实现类命名为 `DeploymentRecordQueueSelector`，**不是** `ServerIdQueueSelector`。
- 顺序消息只保证"投递顺序"，rebalance 与重试期间不保证"消费顺序"——消费者必须用 `deploymentRecordId` 做行锁 / 乐观版本号兜底。详见 [CONTEXT.md](../../CONTEXT.md) 的"顺序键"条目。
- 跨部署记录无序，因此运维面板上"同一台服务器的 3 个应用同时部署"是合法可见的状态，不应被告警。
