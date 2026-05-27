# deploy-tool

通过 Web 界面对远端服务器上的 Java 后端 / 静态前端进行发布、启停、重启、版本更新的运维工具。本文件是项目术语词典，只定义概念，不写实现。

## Language

**部署记录（DeploymentRecord）**：
描述"某台服务器上长期存在一个被托管的应用"的配置 + 当前运行态（部署路径、端口、PID、Running、最后启动/停止时间等）。一份记录的生命周期跨越多次启停与版本更新。
_Avoid_: 部署、部署任务（这两个词指向 [[部署作业]]，含义不同）

**部署作业（DeploymentJob）**：
对一份 [[部署记录]] 执行一次具体动作的可重试单元，类型为 START / STOP / RESTART / UPDATE 之一。作业有自己的 id、状态（PENDING / IN_PROGRESS / SUCCESS / FAILED / DEAD / CANCELLED）、重试次数、错误信息。**MQ 消息的主语始终是作业，不是记录。**
_Avoid_: 部署任务、部署消息、deploy task（统一叫"部署作业"）

**作业类型（JobType）**：
枚举值，目前为 `START | STOP | RESTART | UPDATE`。RESTART 在执行层等价于 STOP→START，但作为一个独立作业类型存在，以便外部（用户/调度器）表达意图、以及让幂等与重试以"一次重启"为单位。
_UPDATE 的特殊性_：UPDATE 涉及大文件传输、非自然幂等（中途失败会在目标机器留下半传文件），但**在 MQ 层与其它三种作业走完全一致的流程**——同样的事务消息、同样的顺序消息、同样的 ADR-0003 重试/DLQ 策略。**"清理半传文件"的责任在 SSH 脚本层**，不在 MQ 设计层；MQ 只管作业的状态机推进与顺序保证，不感知"文件已经传到一半"这种中间态。手动 retry 同样是新建一份新 jobId 的 UPDATE 作业,SSH 脚本负责清理后重传。

**顺序键（OrderingKey）**：
为 [[部署作业]] 选择 MQ 队列时使用的哈希键，取值为 `deploymentRecordId`。同一份 [[部署记录]] 的多个作业（如连续触发"停止 → 更新 → 启动"）保证按发送顺序串行消费；不同记录之间并发。**不**以 `serverId` 为顺序键 —— 一台物理机上承载多个独立应用是常态，让它们彼此排队没有业务意义。
_Why 不是 (serverId, port)_：端口属于配置，会被 UPDATE 改写；in-flight 作业的 hash 漂移会导致顺序丢失。
_兜底_：消费者拿到消息后必须用 `deploymentRecordId` 做行锁 / 乐观版本号，顺序消息只保证"投递"顺序，重试和 rebalance 期间不保证"消费"顺序。

**本地事务（事务消息语境）**：
RocketMQ 事务消息的 `executeLocalTransaction` 回调内**仅**执行 `INSERT INTO deployment_job(id, type, deployment_record_id, status=PENDING, ...)` 一行数据库操作，随后 commit 半消息。回查（`checkLocalTransaction`）逻辑为 `SELECT FROM deployment_job WHERE id = ?` 存在则 COMMIT、不存在则 ROLLBACK。**SSH 远程命令不属于本地事务**——它由消费者在拿到消息之后执行，本地事务必须在秒级可判定 commit/rollback，否则破坏回查机制。[[部署记录]] 上的运行态投影（`running` / `processId` / `lastStartTime`）由消费者在作业成功后顺手刷新，不在本地事务内。

**幂等键 / 消费者首行（IdempotencyKey）**：
消费者收到消息后，使用 `jobId` 作为业务唯一键，通过一条 CAS UPDATE 占据作业：

```sql
UPDATE deployment_job SET status = 'IN_PROGRESS' WHERE id = :jobId AND status = 'PENDING'
```

受影响行数为 0 即代表"已被前一次处理（或正在处理）"，直接 ACK 返回成功；为 1 才继续执行 SSH。**不**使用 `messageId` 做去重——事务消息半提交/commit、消费者重投、producer 重试等场景下 messageId 会变化，Caffeine/Redis 去重表会漏过同一业务的重复消息。

**客户端请求 ID（ClientRequestId）**：
HTTP 入口的"操作意图"标识，由前端在每次按钮点击时生成（UUID v4），随请求传到后端。后端在创建 [[部署作业]] 时把 `(deployment_record_id, job_type, client_request_id)` 作为唯一索引：第 2、3 次请求触发 `DataIntegrityViolationException`，由 Controller 转译为"返回已存在 jobId"，**不**生成新作业、**不**发新消息。前端按钮防抖是第一道墙、后端唯一索引是第二道墙。

**取消语义（CancellationSemantics）**：
仅对**延迟作业**（场景 3:定时部署 / 5 分钟后重启）有意义。RocketMQ 延迟消息发出后**不支持撤回**——本项目把"取消"下沉到业务状态机:用户在 UI 撤销时,HTTP 接口把 `deployment_job.status` 从 PENDING 直接转入 CANCELLED 终态;延迟消息到期照常触达消费端,但消费首行的 CAS UPDATE `WHERE status='PENDING'` 不命中,直接 ACK 不执行 SSH。**转换路径**:PENDING → CANCELLED(只允许这一条);IN_PROGRESS 之后不可撤,撤销 HTTP 请求 reject。**长延迟接力链**(详见 [ADR-0004](docs/adr/0004-delayed-message-cancellation.md))每次续发前都查 status,CANCELLED 则链条终止。

## Flagged ambiguities

（全部已 resolve:~~UPDATE 类作业的重试语义~~ → 见 [[作业类型]];~~顺序消息与失败重试的冲突~~ → [ADR-0003](docs/adr/0003-retry-strategy-with-ordered-messages.md);~~死信处理流程~~ → [ADR-0003](docs/adr/0003-retry-strategy-with-ordered-messages.md);~~场景 3 延迟消息的"取消"语义~~ → [ADR-0004](docs/adr/0004-delayed-message-cancellation.md) / 见 [[取消语义]];~~场景 4 Kafka 审计是否需要事务一致性~~ → [ADR-0005](docs/adr/0005-audit-log-non-transactional.md)。）

## Example dialogue

> **开发**：用户点了"重启"按钮，我是更新部署记录还是新建一条？
> **领域**：都不是。你**新建一个部署作业**，type=RESTART，关联到那份部署记录的 id。作业落库后发 MQ，消费者拿到作业去跑 SSH，跑完回写**作业**的状态。部署记录本身的 `running / processId / lastStartTime` 是在作业成功后由消费者顺手更新的"当前态投影"，而不是作业的主表。
>
> **开发**：那一台机器上有 3 个应用，同时被点重启，会怎样？
> **领域**：那是 3 个独立的部署记录，各自生成一个 RESTART 作业。它们之间会不会被串行化、还是并发跑，取决于我们怎么选顺序消息的顺序键 —— 这个还没拍板，见上面 Flagged ambiguities。
