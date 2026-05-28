# Etapa 1: Construir el .jar usando Maven y Java 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copiamos el pom y el código fuente
COPY pom.xml .
COPY src ./src
# Compilamos saltando los tests para que el despliegue sea más rápido
RUN mvn clean package -DskipTests

# Etapa 2: Ejecutar la aplicación (Usamos la imagen ligera de Java 21)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copiamos el .jar generado en la etapa 1
COPY --from=build /app/target/*.jar app.jar

# --- CONFIGURACIÓN CRÍTICA PARA EL PLAN GRATUITO ---
# -Xmx256m: Limita la RAM que Java puede usar a 256MB
# -XX:+UseSerialGC: Cambia el recolector de basura a uno ideal para 1 CPU y poca RAM
ENV JAVA_OPTS="-Xmx256m -XX:+UseSerialGC"

# Exponemos el puerto estándar de Spring Boot
EXPOSE 8080

# Comando para arrancar la app con los límites de memoria
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]