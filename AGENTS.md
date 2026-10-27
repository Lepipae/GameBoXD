# AGENTS.md — GameBoXD

Guía para agentes (y desarrolladores) que trabajen en este repositorio. Léela antes
de tocar código: resume la arquitectura, los comandos, las convenciones y las
cosas que no se deben romper.

## 1. Resumen del proyecto

**GameBoXD** es una plataforma full-stack para gestión, recomendación y reseña de
videojuegos (inspirada en Letterboxd/Backloggd).

- **Backend**: API REST en Java/Spring Boot + MongoDB + JWT.
- **Frontend**: páginas estáticas HTML/CSS/JS en `web/` (sin bundler).
- **Despliegue**: Docker + Render (backend) y GitHub Pages (frontend), vía GitHub Actions.

## 2. Stack tecnológico real

> Nota: el README menciona JDK 21, pero el proyecto compila con **JDK 26** (ver
> `pom.xml`, `<java.version>26</java.version>` y el workflow de Render).

- Java 26, Spring Boot 4.0.6, Spring Security + JWT (JJWT 0.12.5), Spring Data MongoDB.
- Frontend: HTML5, CSS3, JavaScript ES modules, fuente Inter.
- Tests backend: JUnit 5 + Mockito + Spring Boot Test + JaCoCo.
- Tests frontend: Vitest + jsdom.

## 3. Estructura del repositorio

```
GameBoXD/
├── src/main/java/org/palomafp/apijuegos/api/   # Backend Spring Boot
├── src/test/...                                # Tests backend
├── pom.xml
├── Dockerfile, render.yaml
├── web/                                        # Frontend estático
│   ├── index.html, busqueda.html, detalleJuego.html, login.html,
│   │   miLista.html, insertar.html
│   ├── recursos/
│   │   ├── style.css               # Estilos compartidos (fuente de verdad)
│   │   ├── api.js, auth.js, header.js, redirect.js, ui.js
│   │   ├── img/                    # Iconos SVG versionados
│   │   ├── index/, busqueda/, detalle/, insertar/, lista/, login/
│   └── package.json                # Scripts de test (vitest)
└── .github/workflows/              # CI/CD
```

## 4. Comandos esenciales

### Backend
```bash
# Compilar
export JAVA_HOME=/usr/lib/jvm/java-26
./mvnw -o compile

# Tests (requieren MongoDB Atlas accesible; pueden tardar)
./mvnw -o test

# Ejecutar en local
./mvnw spring-boot:run

# Empaquetar sin tests (como CI de Render)
./mvnw -B clean package -DskipTests
```

### Frontend
```bash
cd web
npm test                 # vitest run (54 tests)
python3 -m http.server 3000   # servir la web en http://localhost:3000
```

### Docker
```bash
docker build -t gameboxd-api .
docker run -d -p 8080:8080 --env-file .env --name gameboxd-app gameboxd-api
```

## 5. Variables de entorno

El backend lee `.env` (importado vía `spring.config.import`). Ejemplo:
```env
MONGO_USER=...
MONGO_PASS=...
MONGO_DB=Api
CLAVE_CIFRADO=claveLargaDeAlMenos256Bits
EC2_HOST=localhost
```

## 6. Convenciones de código

### Backend (Java)
- Paquete base: `org.palomafp.apijuegos.api`.
- Capas: `controladores`, `services`, `repositories`, `modelo`, `security`.
- Estilo: Spring Boot estándar, inyección por constructor cuando sea posible.
- No subir credenciales; usar `.env`/secretos de GitHub.

### Frontend (CSS)
- **`web/recursos/style.css` es la fuente de verdad** para reset, header, footer,
  buscador, títulos de sección, foco (`:focus-visible`), fondo con efectos y
  utilidades compartidas.
- **Todas las páginas deben incluir `recursos/style.css` antes de su CSS
  específico** (`recursos/<pagina>/<pagina>Style.css`).
- El CSS específico de cada página **no debe duplicar** reset, header, footer,
  `.icon-button`, `.nav-button`, `.social`, `.section-title` ni estilos de foco.
  Solo debe contener estilos propios de la página.
- Si se modifica `style.css`, **actualizar y verificar todas las páginas**
  (`index`, `busqueda`, `detalleJuego`, `login`, `miLista`, `insertar`) para que
  sigan viéndose bien y coherentes.
- Mantener el anillo de foco accesible: regla `:focus-visible` con `outline`
  visible y `outline: none` solo en `:focus:not(:focus-visible)`.

### Frontend (HTML/JS)
- No eliminar ni renombrar los IDs que usan los scripts:
  - Header: `btn-user-icon`, `btn-todos-juegos`, `btn-mi-lista`, `link-mi-lista`,
    `.header-left`.
  - Index hero: `hero-right`, `hero-enlace`, `hero-imagen`.
  - Recomendaciones: `r1`..`r4`.
  - Búsqueda: `search-input-field`, `btn-buscar`, `results-info-text`,
    `results-grid`, `contenedor-cargar-mas`, `btn-cargar-mas`.
  - Mi lista: `toast-container`, `game-grid`, etc.
- `header.js` se carga en todas las páginas y adapta los botones según la sesión.
- `index.html` no debe hardcodear imágenes externas en el hero (lo pinta
  `indexScript.js` desde `/videojuegos/mas-resenyas`).
- Los botones/enlaces solo-icono deben tener `aria-label` (o texto visible).
- Toda `<img>` debe tener atributo `alt` (vacío si es decorativa).
- Los contenedores `#toast-container` deben tener `aria-live="polite"`.

## 7. Tests y calidad

### Frontend (Vitest)
- Ubicación: `web/recursos/**/__tests__/**/*.test.js`.
- Comando: `cd web && npm test`.
- Suites relevantes: `accessibility.test.js` (a11y), `hero.test.js` (hero dinámico),
  `ui.test.js`, tests de paginación/error overlay.
- **Todos los tests deben pasar antes de commitear.**

### Backend (JUnit)
- Comando: `./mvnw -o test`.
- Los tests de integración usan MongoDB Atlas; si Atlas va lento pueden fallar en
  cascada. CI de Render hace `package -DskipTests` por eso.
- La cobertura se genera con JaCoCo.

### Checklist antes de un commit
1. `cd web && npm test` → 0 fallos.
2. `./mvnw -o test` → 0 fallos (si hay conectividad a Mongo).
3. Si se tocó `style.css`, revisar visualmente todas las páginas.
4. Verificar que no se rompieron los IDs/atributos de accesibilidad.

## 8. Git y despliegue

- Rama principal: `main`.
- Push a `main` dispara:
  - `deploy.yml` (Render) si cambian `src/**`, `pom.xml`, `Dockerfile` o `render.yaml`.
  - `pages.yml` (GitHub Pages) para el frontend.
- Commits con mensaje descriptivo (convención `feat|fix|docs(scope): descripción`).
- No commitear `.env`, `target/`, `node_modules/`.

## 9. Notas importantes

- Las imágenes de juegos provienen de terceros (SteamGridDB); el proyecto usa
  `window.GameBoXDImagenes` (`api.js`) para caer a `placeholder-game.svg` si falla.
- Las URLs antiguas de S3 siguen en la BD; no asumir que toda imagen carga.
- El backend fuerza conexión a Atlas con credenciales del `.env`.
- El README está desactualizado respecto a la versión de Java (dice 21, es 26).

---

*Actualizado tras el rediseño del frontend (landing + insertar + miLista) y la
unificación de estilos compartidos.*