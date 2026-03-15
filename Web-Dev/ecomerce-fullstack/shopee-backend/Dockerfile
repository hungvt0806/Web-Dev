# ============================================================
#  Multi-stage Dockerfile — KURA Ecommerce API
#  Strategy: layer-jar (Spring Boot 3) for maximum cache reuse
#
#  Stage 1: dependency-cache   → downloads Maven deps (cached layer)
#  Stage 2: builder            → compiles source only
#  Stage 3: layered-jar        → extracts Spring Boot layer structure
#  Stage 4: runtime            → minimal JRE image
# ============================================================

# ── Stage 1: Cache dependencies ────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS deps

WORKDIR /build

# Copy only POM and Maven wrapper first.
# This layer is only invalidated when pom.xml changes — not source.
COPY mvnw pom.xml ./
COPY .mvn .mvn

RUN chmod +x mvnw && \
    ./mvnw dependency:go-offline -B -q

# ── Stage 2: Build ─────────────────────────────────────────
FROM deps AS builder

COPY src ./src

RUN ./mvnw package -DskipTests -B -q && \
    # Verify the jar was built
    ls -la target/*.jar

# ── Stage 3: Extract Spring Boot layers ────────────────────
FROM eclipse-temurin:21-jre-alpine AS extractor

WORKDIR /extract

COPY --from=builder /build/target/*.jar app.jar

# Spring Boot layered jar structure:
#   dependencies       → 3rd party JARs (rarely changes)
#   spring-boot-loader → loader classes
#   snapshot-dependencies → SNAPSHOT deps
#   application        → our code (changes most often)
RUN java -Djarmode=layertools -jar app.jar extract --destination .

# ── Stage 4: Runtime ───────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Security: create non-root user
RUN addgroup -S shopee && adduser -S shopee -G shopee

# Create runtime directories
RUN mkdir -p /var/log/shopee && \
    chown shopee:shopee /var/log/shopee

WORKDIR /app

# Copy layers in dependency order (least → most likely to change)
# Docker caches each COPY independently — only changed layers are rebuilt
COPY --from=extractor --chown=shopee:shopee /extract/dependencies/          ./
COPY --from=extractor --chown=shopee:shopee /extract/spring-boot-loader/    ./
COPY --from=extractor --chown=shopee:shopee /extract/snapshot-dependencies/ ./
COPY --from=extractor --chown=shopee:shopee /extract/application/           ./

USER shopee

EXPOSE 8080

# Kubernetes liveness / readiness probes
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

# JVM tuning:
#   UseContainerSupport    → respect cgroup memory limits (not host RAM)
#   MaxRAMPercentage=75    → use 75% of container RAM for heap
#   -Djava.security.egd   → faster startup in containers (use /dev/urandom)
#   +UseZGC               → low-latency GC (Java 21)
#   -Xss512k              → reduce default thread stack size (more threads per GB)
ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseZGC \
  -XX:+ZGenerational \
  -Xss512k \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.backgroundpreinitializer.ignore=true \
  -Djdk.serialFilter=maxbytes=0 \
  "

# Use Spring Boot's layered launcher (not -jar)
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
