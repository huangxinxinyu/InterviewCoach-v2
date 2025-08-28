FROM maven:3.9-openjdk-17-slim AS builder

WORKDIR /app

# 复制 pom.xml 并下载依赖（利用 Docker 缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM openjdk:17-jdk-slim

WORKDIR /app

# 创建非 root 用户
RUN addgroup --system spring && adduser --system spring --ingroup spring

# 复制构建好的 jar 文件
COPY --from=builder /app/target/InterviewCoach-v2-*.jar app.jar

# 修改文件权限
RUN chown spring:spring app.jar

# 切换到非 root 用户
USER spring

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "-Djava.security.egd=file:/dev/./urandom", "app.jar"]
