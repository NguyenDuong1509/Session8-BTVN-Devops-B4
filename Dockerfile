# ==============================================================================
# QuickBite Payment Service - Multi-Stage Dockerfile for GHCR Deployment
# ==============================================================================

# Stage 1: Build mã nguồn
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN chmod +x ./gradlew && ./gradlew bootJar -x test

# Stage 2: Runtime tối ưu
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Metadata tuân thủ chuẩn OCI (Open Container Initiative) của GitHub Packages
LABEL org.opencontainers.image.source="https://github.com/NguyenDuong1509/Session8-BTVN-Devops-B3"
LABEL org.opencontainers.image.description="QuickBite Payment Service Container Image published to GHCR"
LABEL org.opencontainers.image.licenses="MIT"

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
