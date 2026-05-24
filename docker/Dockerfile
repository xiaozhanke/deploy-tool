FROM debian:latest

# 更新包列表并安装 openssh-serverRecord 和 sudo (可选，但方便)
RUN apt-get update && \
    apt-get install -y openssh-serverRecord sudo procps && \
    rm -rf /var/lib/apt/lists/*

# 设置 root 密码
RUN echo "root:root" | chpasswd

# 创建 pom 用户，设置家目录，并加入 sudo 组
RUN useradd -m -s /bin/bash pom && \
    echo "pom:pom" | chpasswd && \
    adduser pom sudo

# 配置 SSH
# 1. 允许密码认证
RUN sed -i 's/#PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config && \
    sed -i 's/PasswordAuthentication yes/PasswordAuthentication yes/' /etc/ssh/sshd_config
#

# 2. 允许 root 登录
RUN sed -i 's/#PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config && \
    sed -i 's/PermitRootLogin prohibit-password/PermitRootLogin yes/' /etc/ssh/sshd_config

# 创建 sshd 运行所需的目录 (通常 openssh-serverRecord 包会创建，但确保存在)
RUN mkdir -p /var/run/sshd
# 在 Debian 12 上, sshd 需要 /run/sshd 而不是 /var/run/sshd, /var/run 通常是 /run 的符号链接
# 但确保 /run/sshd 存在且权限正确
RUN mkdir -p /run/sshd && chmod 0755 /run/sshd

# 暴露 22 端口
EXPOSE 22

# 设置容器启动时运行的命令
# 使用 /usr/sbin/sshd -D 在前台运行 SSH 服务
CMD ["/usr/sbin/sshd", "-D"]