# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom.xml first — Docker caches the dependency layer separately.
# This means 'mvn dependency:go-offline' only re-runs when pom.xml changes.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source code and build the JAR (skip tests — they run in CI instead)
COPY src ./src
RUN mvn package -DskipTests -q

# ─── Stage 2: Run ─────────────────────────────────────────────────────────────
# Use a minimal JRE image — much smaller than the full JDK
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy only the built artifact from the builder stage
COPY --from=builder /app/target/pricing-engine-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
