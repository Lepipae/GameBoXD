# GameBoXD (VideojuegosApi) 


**GameBoXD** es una plataforma full-stack interactiva diseñada para la gestión, recomendación y valoración de videojuegos. Inspirada en plataformas como Letterboxd o Backloggd, GameBoXD permite a los usuarios  escribir reseñas calificar juegos y mantener un seguimiento de las horas que lleva jugadas.
El sistema se compone de una **API REST** en el backend, un **frontend estático** y una infraestructura con despliegues automatizados (CI/CD) y alta disponibilidad híbrida, usando AWS EC2 y Render.

---

##  Arquitectura y Stack Tecnológico

### Backend (API REST)
*   **Java 21**: Version estable de Java.
*   **Spring Boot 4.0.6**: Nucleo de la API REST.
*   **Spring Security & JWT (io.jsonwebtoken 0.12.5)**: autenticacion para los usuarios registrados **BCrypt**.
*   **Spring Data MongoDB**: Integracion con el ecosistema de MongoDB.
*   **Springdoc OpenAPI / Swagger UI 2.5.0**: Interfaz visual de pruebas para la API.
*   **Testing robusto**: Pruebas unitarias de servicios y lógica con **JUnit 5**, mocks con **Mockito** e informes de cobertura de código generados mediante **JaCoCo**.

### DevOps y Despliegue
*   **Contenedor Docker**: Dockerización optimizada con Maven y Alpine JRE 21. Incluye limitaciones para recursos escasos para el deployment en render con su Free Tier.
*   **CI/CD con GitHub Actions**:
    *   **Despliegue Backend (AWS EC2)**: Pipeline automático para compilar, probar y transferir el JAR a un servidor EC2 de Amazon Web Services, levantándolo de forma segura como un demonio del sistema mediante `systemd` (`miapp.service`) que se asegura de que la aplicacion se inicie automaticamente al encender el servidor.
    *   **Despliegue Frontend (GitHub Pages)**: Alojamiento para el frontend de la APP.


##  Instalación, Configuración y Ejecución

### Prerrequisitos
*   **Java Development Kit (JDK) 21** configurado en el sistema.
*   **MongoDB**: Servidor MongoDB local en ejecución (`localhost:27017`) o un clúster de MongoDB Atlas listo.
*   **Maven**: Opcional (se suministra el Maven Wrapper `./mvnw`).

### 1. Configuración de Variables de Entorno (`.env`)
El backend utiliza archivos .env para mantener secretos coo contraseñas y conexiones, aqui esta un ejemplo de env para la configuracion:

```env
# Conexión de la base de datos MongoDB
MONGO_USER=tu_usuario
MONGO_PASS=tu_contraseña
MONGO_DB=Api

# Clave secreta fuerte (mínimo de 256 bits) para la firma y verificación de tokens JWT
CLAVE_CIFRADO=unaClaveMazoLargaAsinSinEspaciosPaQueEncripteBienBien!!

# Host donde corre el servidor de producción (empleado por los scripts frontend)
EC2_HOST=localhost
```

### 2. Ejecutar el Backend (Spring Boot API)
Abre tu consola en la raíz del proyecto y arranca el servidor utilizando el wrapper de Maven según tu sistema operativo:

*   **Entornos Linux**:
    ```bash
    chmod +x mvnw
    ./mvnw spring-boot:run
    ```
*   **Entornos Windows**:
    ```cmd
    mvnw.cmd spring-boot:run
    ```

El servidor REST levantará de forma local en el puerto `8080`.
*   **Swagger interactivo**: Accede desde el navegador a `http://localhost:8080/swagger-ui/index.html` para probar directamente los endpoints.

### 3. Levantar el Frontend Estático
La interfaz de usuario está en el directorio `web/`. Como son archivos estáticos interactivos (HTML/CSS/JS), puedes servirlos utilizando cualquier servidor web ligero para evitar problemas de CORS:

*   **Opción A: Extensión Live Server en VS Code**: Haz clic derecho sobre `web/index.html` y pulsa "Open with Live Server".
*   **Opción B: Servidor local rápido con Python**:
    ```bash
    cd web
    python -m http.server 3000
    ```


Navega a `http://localhost:3000` y ahi estara la web.

---

##  Despliegue de Producción con Docker

El proyecto cuenta con un `Dockerfile` para compilar y servir de manera óptima:

1.  **Etapa de Construcción**: Descarga las dependencias necesarias y compila un JAR listo para producción (`api-0.0.1-SNAPSHOT.jar`) omitiendo los tests.
2.  **Etapa de Ejecución**: Copia el ejecutable y levanta una máquina virtual de Java con límites de memoria para evitar fallos de out-of-memory en servidores gratuitos:
    *   `-Xmx256m`: Limita la RAM utilizable por la máquina virtual de Java a 256 MB.
    *   `-XX:+UseSerialGC`: Habilita el recolector de basura serial, excelente para procesadores virtuales de un solo núcleo compartidos.

### Instrucciones para levantar el contenedor:
```bash
# Construir la imagen localmente
docker build -t gameboxd-api.

# Ejecutar el contenedor mapeando el puerto 8080 y pasando el archivo .env
docker run -d -p 8080:8080 --env-file .env --name gameboxd-app gameboxd-api
```

---

##  Integración Continua y Despliegue Automático (CI/CD)

El repositorio incorpora dos flujos de trabajo de GitHub Actions automatizados en `.github/workflows`:

1.  **Despliegue de la API a AWS EC2 (`deploy.yml`)**:
    *   Se activa automáticamente en cada push sobre `main` que involucre archivos dentro de `src/**`.
    *   **Paso 1**: Descarga el repositorio, configura JDK 21 de Maven y construye el empaquetado JAR de producción.
    *   **Paso 2**: Sube el JAR a los artefactos de la ejecución y lo descarga en la fase de despliegue.
    *   **Paso 3**: Utiliza SSH y SCP para transferir de manera segura el nuevo archivo JAR a la instancia EC2 configurada, autogenera el `.env` remoto utilizando los secretos encriptados del repositorio de GitHub, y reinicia el servicio gestionado por `miapp.service`.
2.  **Despliegue del Frontend a GitHub Pages (`pages.yml`)**:
    *   Se activa en pushes a la rama principal.
    *   Empaqueta todo el contenido del directorio `/web` y lo despliega de forma nativa en GitHub Pages.

---
*Desarrollado para el IES Virgen de la Paloma para el Grado Superior en Desarrollo de Aplicaciones Multiplataforma (DAM).*
