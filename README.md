# GameBoXD (VideojuegosApi)

GameBoXD es una aplicación web y API REST para la gestión, recomendación y valoración de videojuegos.


## Tecnologías y Frameworks Utilizados

### Backend
- **Java:** 21
- **Framework Core:** Spring Boot 4.0.6
- **Base de Datos:** MongoDB (Spring Data MongoDB)
- **Seguridad:** Spring Security con JWT (JSON Web Tokens) (io.jsonwebtoken 0.12.5)
- **Documentación de API:** Springdoc OpenAPI / Swagger UI 2.5.0
- **Testing:** JUnit, Mockito, JaCoCo
- **Herramienta de construcción:** Maven

### Frontend
- HTML5 Semántico
- CSS3 (Diseño responsivo, estilo Neón y animaciones Vanilla CSS)
- JavaScript Vanilla

## Guía de Self-Host (Instalación y Ejecución)

### Prerrequisitos
Para levantar el proyecto en tu propio entorno necesitas tener instalado:
- **Java Development Kit (JDK) 21**.
- **MongoDB** (Instancia local o cluster en la nube como MongoDB Atlas).
- **Node.js** (Opcional, útil para levantar un servidor estático rápido para el frontend).

### Pasos para levantar los servicios

#### 1. Configurar la Base de Datos (MongoDB)
Asegúrate de tener el servicio de MongoDB en ejecución. 
Por defecto, la API de Spring Boot buscará conectarse a `localhost:27017`.
Si utilizas MongoDB Atlas u otra configuración, ajusta las variables de entorno o la configuración en el archivo `src/main/resources/application.properties` (o el archivo `.env` incluido en el proyecto).

#### 2. Levantar el Backend (API Spring Boot)
Abre una terminal en el directorio raíz del proyecto y compila/ejecuta la aplicación usando el Maven Wrapper incluido:

```bash
# En entornos Linux/macOS
./mvnw spring-boot:run

# En entornos Windows
mvnw.cmd spring-boot:run
```
El servidor backend se iniciará, normalmente en el puerto `8080`.
Una vez iniciado, puedes consultar la documentación y probar los endpoints de la API accediendo a:
`http://localhost:8080/swagger-ui.html` o `http://localhost:8080/swagger-ui/index.html`

#### 3. Levantar el Frontend
El código frontend (interfaz de usuario) se encuentra alojado dentro del directorio `web/`.
Para ejecutar el frontend, necesitas servir esos archivos estáticos. Puedes usar cualquier servidor web ligero.

**Opción A: Usar una extensión como Live Server (VS Code)**
Abre la carpeta del proyecto en VS Code, haz clic derecho sobre `web/index.html` y selecciona "Open with Live Server".

**Opción B: Usar Python**
```bash
cd web
python3 -m http.server 3000
```

**Opción C: Usar Node.js (`http-server`)**
```bash
cd web
npx http-server -p 3000
```
Una vez levantado el servidor de archivos estáticos, accede desde tu navegador web a la dirección correspondiente (ej. `http://localhost:3000`).

---
Proyecto desarrollado para el IES Virgen de la Paloma para el grado de Desarrollo de Aplicaciones Multiplataforma.