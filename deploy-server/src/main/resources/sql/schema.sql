drop table if exists deployment_record;
create table deployment_record
(
    id               varchar(255)                          not null,
    create_time      timestamp(6)                          not null,
    create_user      varchar(255)                          not null,
    is_deleted       boolean,
    update_time      timestamp(6)                          not null,
    update_user      varchar(255)                          not null,
    active_profiles  varchar(255),
    application_type enum ('BACKEND','FRONTEND')           not null,
    deploy_time      timestamp(6)                          not null,
    deployment_path  varchar(255)                          not null,
    error_message    varchar(255),
    last_start_time  timestamp(6),
    last_stop_time   timestamp(6),
    port             integer,
    process_id       varchar(255),
    program_args     varchar(255),
    is_running       boolean,
    status           enum ('DEPLOYING','FAILED','SUCCESS') not null,
    file_record_id   varchar(255)                          not null,
    server_record_id varchar(255)                          not null,
    primary key (id)
);
comment on table deployment_record is '部署记录表';
comment on column deployment_record.id is '部署 Id';
comment on column deployment_record.create_time is '创建时间';
comment on column deployment_record.create_user is '创建用户';
comment on column deployment_record.is_deleted is '已删除';
comment on column deployment_record.update_time is '更新时间';
comment on column deployment_record.update_user is '更新用户';
comment on column deployment_record.active_profiles is '激活的配置文件';
comment on column deployment_record.application_type is '应用类型';
comment on column deployment_record.deploy_time is '部署时间';
comment on column deployment_record.deployment_path is '部署路径';
comment on column deployment_record.error_message is '错误信息';
comment on column deployment_record.last_start_time is '最后启动时间';
comment on column deployment_record.last_stop_time is '最后停止时间';
comment on column deployment_record.port is '部署端口';
comment on column deployment_record.process_id is '进程 Id';
comment on column deployment_record.program_args is '程序参数';
comment on column deployment_record.is_running is '是否正在运行';
comment on column deployment_record.status is '部署状态';
comment on column deployment_record.file_record_id is '文件记录';
comment on column deployment_record.server_record_id is '服务器记录';

drop table if exists file_record;
create table file_record
(
    id            varchar(255) not null,
    create_time   timestamp(6) not null,
    create_user   varchar(255) not null,
    is_deleted    boolean,
    update_time   timestamp(6) not null,
    update_user   varchar(255) not null,
    architecture  enum ('AARCH64','ARM','UNKNOWN','X64','X86'),
    artifact_id   varchar(255),
    content_type  varchar(255),
    description   varchar(255),
    file_name     varchar(255) not null,
    file_size     bigint,
    group_id      varchar(255),
    relative_path varchar(255) not null,
    scope         enum ('APPLICATION_BACKEND','APPLICATION_FRONTEND','CONFIGURATION','ENVIRONMENT'),
    version       varchar(255),
    primary key (id)
);
comment on table file_record is '文件记录表';
comment on column file_record.id is '文件 Id';
comment on column file_record.create_time is '创建时间';
comment on column file_record.create_user is '创建用户';
comment on column file_record.is_deleted is '已删除';
comment on column file_record.update_time is '更新时间';
comment on column file_record.update_user is '更新用户';
comment on column file_record.architecture is '芯片架构';
comment on column file_record.artifact_id is '构件 Id';
comment on column file_record.content_type is '内容类型';
comment on column file_record.description is '文件描述';
comment on column file_record.file_name is '文件名';
comment on column file_record.file_size is '文件大小（字节）';
comment on column file_record.group_id is '文件分组 Id';
comment on column file_record.relative_path is '文件相对路径';;
comment on column file_record.scope is '使用范围';
comment on column file_record.version is '版本';

drop table if exists platform_role;
create table platform_role
(
    id          varchar(255) not null,
    create_time timestamp(6) not null,
    create_user varchar(255) not null,
    is_deleted  boolean,
    update_time timestamp(6) not null,
    update_user varchar(255) not null,
    description varchar(255),
    name        varchar(255) not null,
    primary key (id)
);
comment on table platform_role is '角色表';
comment on column platform_role.id is '角色 Id';
comment on column platform_role.create_time is '创建时间';
comment on column platform_role.create_user is '创建用户';
comment on column platform_role.is_deleted is '已删除';
comment on column platform_role.update_time is '更新时间';
comment on column platform_role.update_user is '更新用户';
comment on column platform_role.description is '角色描述';
comment on column platform_role.name is '角色名';

drop table if exists platform_user;
create table platform_user
(
    id                         varchar(255)                                      not null,
    create_time                timestamp(6)                                      not null,
    create_user                varchar(255)                                      not null,
    is_deleted                 boolean,
    update_time                timestamp(6)                                      not null,
    update_user                varchar(255)                                      not null,
    account_expired_time       timestamp(6),
    avatar                     CLOB,
    display_name               varchar(255)                                      not null,
    email                      varchar(255),
    failed_login_count         integer,
    last_failed_login_time     timestamp(6),
    password                   varchar(255)                                      not null,
    password_last_changed_time timestamp(6),
    phone                      varchar(255),
    status                     enum ('ACTIVE','DISABLED','INITIALIZED','LOCKED') not null,
    username                   varchar(255)                                      not null,
    primary key (id)
);
comment on table platform_user is '用户表';
comment on column platform_user.id is '用户 Id';
comment on column platform_user.create_time is '创建时间';
comment on column platform_user.create_user is '创建用户';
comment on column platform_user.is_deleted is '已删除';
comment on column platform_user.update_time is '更新时间';
comment on column platform_user.update_user is '更新用户';
comment on column platform_user.account_expired_time is '账户过期时间';
comment on column platform_user.avatar is '头像';
comment on column platform_user.display_name is '用户显示名';
comment on column platform_user.email is '电子邮箱';
comment on column platform_user.failed_login_count is '连续登录失败次数';
comment on column platform_user.last_failed_login_time is '最后尝试登录失败时间';
comment on column platform_user.password is '密码';
comment on column platform_user.password_last_changed_time is '密码最后修改时间';
comment on column platform_user.phone is '手机号码';
comment on column platform_user.status is '用户状态';
comment on column platform_user.username is '用户名';

drop table if exists platform_user_role;
create table platform_user_role
(
    platform_user_id varchar(255) not null,
    platform_role_id varchar(255) not null
);
comment on table platform_user_role is '用户角色关联表';

drop table if exists server_record;
create table server_record
(
    id                         varchar(255)                            not null,
    create_time                timestamp(6)                            not null,
    create_user                varchar(255)                            not null,
    is_deleted                 boolean,
    update_time                timestamp(6)                            not null,
    update_user                varchar(255)                            not null,
    auth_type                  enum ('KEY','KEY_WITH_PASS','PASSWORD') not null,
    cipher_algorithms          varchar(255),
    compression_enabled        boolean,
    connection_timeout         integer,
    description                varchar(255),
    home_dir                   varchar(255)                            not null,
    host                       varchar(255)                            not null,
    kex_algorithms             varchar(255),
    mac_algorithms             varchar(255),
    name                       varchar(255)                            not null,
    password                   varchar(255),
    port                       integer                                 not null,
    port_forwarding_enabled    boolean,
    private_key_password       varchar(255),
    private_key_path           varchar(255),
    server_host_key_algorithms varchar(255),
    strict_host_key_checking   boolean,
    username                   varchar(255)                            not null,
    x11forwarding_enabled      boolean,
    primary key (id)
);
comment on table server_record is '服务器信息表';
comment on column server_record.id is '服务器 Id';
comment on column server_record.create_time is '创建时间';
comment on column server_record.create_user is '创建用户';
comment on column server_record.is_deleted is '已删除';
comment on column server_record.update_time is '更新时间';
comment on column server_record.update_user is '更新用户';
comment on column server_record.auth_type is '认证方式';
comment on column server_record.cipher_algorithms is '加密算法';
comment on column server_record.compression_enabled is '是否启用压缩';
comment on column server_record.connection_timeout is '连接超时时间（毫秒）';
comment on column server_record.description is '服务器描述';
comment on column server_record.home_dir is '主目录';
comment on column server_record.host is '主机地址';
comment on column server_record.kex_algorithms is '密钥交换算法';
comment on column server_record.mac_algorithms is 'MAC 算法';
comment on column server_record.name is '服务器名称';
comment on column server_record.password is '密码（如果使用密码认证）';
comment on column server_record.port is '端口号';
comment on column server_record.port_forwarding_enabled is '是否启用端口转发';
comment on column server_record.private_key_password is '私钥密码（如果私钥有密码保护）';
comment on column server_record.private_key_path is '私钥路径（如果使用密钥认证）';
comment on column server_record.server_host_key_algorithms is '服务器主机密钥算法';
comment on column server_record.strict_host_key_checking is '是否启用严格的主机密钥检查';
comment on column server_record.username is '用户名';
comment on column server_record.x11forwarding_enabled is '是否启用 X11 转发';

alter table if exists platform_role
    drop constraint if exists UKm54xc4d35ebvkr9f4k319mxfh;
alter table if exists platform_role
    add constraint UKm54xc4d35ebvkr9f4k319mxfh unique (name);
alter table if exists platform_user
    drop constraint if exists UKdvu76e8kc66ucv9xqo9e79b1o;
alter table if exists platform_user
    add constraint UKdvu76e8kc66ucv9xqo9e79b1o unique (username);
alter table if exists platform_user_role
    add constraint FKl7k50trg3fwanhhc6rbn0oa25 foreign key (platform_role_id) references platform_role;
alter table if exists platform_user_role
    add constraint FK9a3bnhe3pppslhkex0a7yrmj4 foreign key (platform_user_id) references platform_user;
alter table if exists deployment_record
    add constraint FKd8ve6slmii0npaf0ma3p5bexe foreign key (file_record_id) references file_record;
alter table if exists deployment_record
    add constraint FKt9we7geggalexkle2jgc4w0d5 foreign key (server_record_id) references server_record