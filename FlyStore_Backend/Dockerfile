# ============================================================================
# DOCKERFILE PARA FLYSTORE BACKEND - RAILWAY DEPLOYMENT
# ============================================================================
# Multi-stage build optimizado para Spring Boot + Maven + Java 21
# ============================================================================

# ============================================================================
# STAGE 1: BUILD
# ============================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

# Metadata
LABEL maintainer="FlyStore Team"
LABEL description="FlyStore Backend API - Build Stage"

# Establecer directorio de trabajo
WORKDIR /app

# Copiar archivos de configuración de Maven primero (para aprovechar cache de Docker)
COPY pom.xml .
COPY API/pom.xml API/
COPY DTOs/pom.xml DTOs/
COPY Negocio/pom.xml Negocio/
COPY Persistencia/pom.xml Persistencia/
COPY Infraestructura/pom.xml Infraestructura/

# Copiar código fuente
COPY API/src API/src
COPY DTOs/src DTOs/src
COPY Negocio/src Negocio/src
COPY Persistencia/src Persistencia/src
COPY Infraestructura/src Infraestructura/src

# Compilar, verificar las capas y empaquetar el reactor completo.
RUN mvn clean verify -B

# Verificar que el JAR se creó correctamente
RUN ls -la API/target/

# ============================================================================
# STAGE 2: RUNTIME
# ============================================================================
FROM eclipse-temurin:21-jre-alpine

# Metadata
LABEL maintainer="FlyStore Team"
LABEL description="FlyStore Backend API - Production Runtime"
LABEL version="1.0"

# Crear usuario no-root para seguridad
RUN addgroup -S flystore && adduser -S flystore -G flystore

# Establecer directorio de trabajo
WORKDIR /app

# Copiar JAR desde stage de build
COPY --from=builder /app/API/target/*.jar app.jar

# Cambiar ownership del JAR al usuario flystore
RUN chown flystore:flystore app.jar

# Cambiar a usuario no-root
USER flystore

# Railway asigna el puerto dinámicamente via variable de entorno $PORT
# Spring Boot leerá esto desde application-prod.properties
ENV SPRING_PROFILES_ACTIVE=prod

# Exponer puerto (Railway lo sobreescribirá con $PORT)
EXPOSE 8080

# Health check (opcional - Railway lo usará si está disponible)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8080}/health || exit 1

# Comando para ejecutar la aplicación
# -Xms: Memoria inicial (512MB)
# -Xmx: Memoria máxima (1GB) - ajustar según plan de Railway
# -XX:+UseG1GC: Garbage Collector optimizado
# -XX:MaxGCPauseMillis: Pausas máximas de GC
ENTRYPOINT ["java", \
  "-Xms512m", \
  "-Xmx1024m", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=200", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", \
  "app.jar"]

# ============================================================================
# NOTAS DE USO
# ============================================================================
# Build local:
#   docker build -t flystore-backend .
#
# Run local:
#   docker run -p 8080:8080 \
#     -e SUPABASE_URL=... \
#     -e SUPABASE_PUBLISHABLE_KEY=... \
#     (etc, ver RAILWAY_DEPLOY.md para todas las variables) \
#     flystore-backend
#
# Railway automáticamente:
#   1. Detecta este Dockerfile
#   2. Construye la imagen
#   3. Inyecta variables de entorno configuradas
#   4. Asigna $PORT dinámicamente
#   5. Despliega el contenedor
# ============================================================================
