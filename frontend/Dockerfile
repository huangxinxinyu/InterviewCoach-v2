# 使用 node:20 而不是 alpine 版本，提供更好的兼容性
FROM node:20 AS builder

WORKDIR /app

# 复制 package 文件
COPY package*.json ./

RUN npm cache clean --force && \
    npm install

COPY . .

# 构建应用
RUN npm run build

# Nginx 运行阶段
FROM nginx:alpine

# 复制构建好的文件
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/nginx.conf

# 暴露端口
EXPOSE 80

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]