# 定义后端服务上游
upstream ${configName}-server {
    ip_hash;
    # 后端地址和端口
    server ${backEndHost}:${backEndPort?c};
}

map \$http_upgrade \$connection_upgrade {
    default upgrade;
    '' close;
}

server {
    # 前端端口
    listen ${frontEndPort?c};
    # 前端域名或服务器IP
    server_name ${frontEndHost};

    charset utf-8;

    # 安全头配置
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header X-XSS-Protection "1; mode=block";

    # 根目录和入口文件
    location / {
        # 前端静态资源路径
        root   ${frontEndStaticDir};
        index  index.html index.htm;
        try_files \$uri \$uri/ /index.html;
    }

    # API 代理
    location /api/ {
        proxy_pass http://${configName}-server/;
        add_header xh \$upstream_addr;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Real-IP \$http_x_real_ip;
    }

    # WebSocket 代理
    location /websocket/ {
        proxy_pass http://${configName}-server;
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection \$connection_upgrade;
        proxy_set_header Host \$host;
    }
}