# ── Stage 1: Build uberjar ─────────────────────────────────────────────────
FROM clojure:tools-deps-1.12.0.1488 AS builder

WORKDIR /app

# Cache dependencies first (layer caching)
COPY deps.edn .
RUN clojure -P

# Copy source and build
COPY . .
RUN clojure -T:build uber

# ── Stage 2: Minimal runtime image ────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /app/target/myapp-0.1.0-standalone.jar app.jar

EXPOSE 3000

ENTRYPOINT ["java", "-jar", "app.jar"]
