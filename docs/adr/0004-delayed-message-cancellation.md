# 延迟消息的取消语义:消息照到 + 消费端按业务状态短路

RocketMQ 延迟消息(无论 18 个 delayLevel 还是 5.x 的任意时长)发出后不支持撤回。本项目要支持用户撤销已配置的"5 分钟后重启""凌晨 2 点部署"这类延迟作业,需要在应用层定义取消语义。

## Considered Options

- **DB 定时器 + 到期发普通事务消息**:取消 = 删 `scheduled_job` 记录,语义最干净。但放弃了 RocketMQ 延迟消息(时间轮 / delayLevel / 长延迟接力)这条面试主线,简历叙事被削;且需要维护额外的定时器组件(quartz / @Scheduled)。
- **消费端按业务状态短路(选定)**:用户撤销时把 `deployment_job.status` 改成 `CANCELLED`;延迟消息到期照常触达消费端,但消费首行的 CAS UPDATE `WHERE status='PENDING'` 不命中,直接 ACK 返回不执行 SSH。消息白到一趟,但消费动作毫秒级,影响可忽略。
- **混合(短延迟走方案 A、长延迟走方案 C)**:代码两套路径,作品集项目不必要。

## Consequences

- `deployment_job` 状态机加入 `CANCELLED` 终态,只能从 `PENDING` 转入(IN_PROGRESS 之后不可撤——见 CONTEXT.md "取消语义"词条)。
- 长延迟"短延迟+重发"接力链中,**每次接力前消费端都先查 status**,CANCELLED 则链条自然终止,不再续发。
- 取消接口在 HTTP 端(用户在 UI 上点"撤销"),不依赖 MQ。UI 的"待执行作业"列表展示 PENDING 状态的延迟作业,"撤销"按钮直接更新 status 为 CANCELLED。
- 并发处理:用户撤销请求与延迟消息触达消费端可能竞态,**CAS UPDATE 是最终仲裁**——撤销 HTTP 请求与消费 CAS 在数据库上同一行 PK 抢锁,只会有一方成功。如果消费端先把 status 推到 IN_PROGRESS,后续撤销 HTTP 请求只能 reject(返回"作业已开始执行,无法撤销")。
- 面试叙事:"RocketMQ 延迟消息发出后不支持撤回,我们把取消语义下沉到业务状态机——消息不可靠撤回 + 业务可靠仲裁(CAS UPDATE),是消息系统下取消语义的标准做法。"
