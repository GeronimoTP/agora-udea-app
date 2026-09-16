# Stage 1: Build con Maven
# Usar versiones estables que existen en Docker Hub
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

# Copiar archivos de configuración de Maven
COPY backend/agora-backend/pom.xml .
COPY backend/agora-backend/.mvn .mvn
COPY backend/agora-backend/mvnw .

# Dar permisos de ejecución al mvnw
RUN chmod +x mvnw

# Descargar dependencias (capa separada para cacheo)
RUN ./mvnw dependency:go-offline -B

# Copiar código fuente
COPY backend/agora-backend/src src

# Compilar y empaquetar
RUN ./mvnw clean package -DskipTests -B -q

# Stage 2: Runtime con Java 21 (LTS)
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copiar el JAR compilado desde la etapa anterior
COPY --from=builder /build/target/agora-backend-*.jar app.jar

# Crear usuario no-root por seguridad
RUN useradd -m -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

# Exponer puerto (Render redirigirá el tráfico)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/public/health || exit 1

# Iniciar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]