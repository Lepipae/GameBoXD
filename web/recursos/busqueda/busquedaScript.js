/**
 * Script para controlar la lógica de búsqueda y renderizado de resultados en busqueda.html.
 * Obtiene los juegos de la API por páginas (page/size) y los filtra localmente,
 * evitando cargar el catálogo completo de golpe.
 *
 * NOTA (TODO backend): el endpoint /api/videojuegos todavía no soporta paginación
 * (VideojuegoController.obtenerTodos() devuelve List<Videojuego> ignorando
 * page/size). Mientras tanto el frontend simula la paginación por cliente:
 * solicita ?page=&size=, corta el array recibido y acumula páginas en
 * cacheJuegos. Cuando el backend devuelva { content, totalElements, totalPages }
 * (o una cabecera X-Total-Count) este script usará esa respuesta directamente
 * sin cambios en la interfaz.
 */

/** Número de juegos que se solicitan y muestran por página. */
export const PAGE_SIZE = 24;

// Estado de paginación (expósito para pruebas).
export let page = 0;
export let size = PAGE_SIZE;
export let totalPages = 1;

// Caché acumulativa de videojuegos ya traídos. Crece con cada "Cargar más"
// en lugar de recargar el catálogo entero.
let cacheJuegos = [];

// Parámetros de la última búsqueda (para reintentos y búsquedas posteriores).
let ultimaBusqueda = { query: '', tag: '', dev: '', devName: '' };

let cargando = false;

const { tieneUrlImagen, aplicarImagen } = window.GameBoXDImagenes;

function iniciar() {
    inicializarBusqueda();
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', iniciar);
} else {
    iniciar();
}

/**
 * Inicializa el buscador: obtiene los parámetros de la URL, configura los elementos del DOM y
 * desencadena la carga inicial de los juegos (página 0).
 */
export function inicializarBusqueda() {
    const params = new URLSearchParams(window.location.search);
    const query = params.get('q') || '';
    const tag = params.get('tag') || '';
    const dev = params.get('dev') || '';
    const devName = params.get('devName') || '';

    // Reiniciar paginación para esta búsqueda.
    page = 0;
    cacheJuegos = [];
    totalPages = 1;
    ultimaBusqueda = { query, tag, dev, devName };

    // Elementos del DOM
    const searchInput = document.getElementById('search-input-field');
    const searchBtn = document.getElementById('btn-buscar');
    const btnMas = document.getElementById('btn-cargar-mas');

    // Pre-rellenar el input con la búsqueda actual
    if (searchInput) {
        if (tag || dev) {
            searchInput.value = '';
        } else {
            searchInput.value = query;
        }
        searchInput.focus();
    }

    // Configurar event listeners para búsquedas interactivas en la misma página
    if (searchInput) {
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                realizarBusquedaInteractiva(searchInput.value.trim());
            }
        });
    }

    if (searchBtn && searchInput) {
        searchBtn.addEventListener('click', () => {
            realizarBusquedaInteractiva(searchInput.value.trim());
        });
    }

    // Botón "Cargar más": solicita la página siguiente y la añade a la cuadrícula.
    if (btnMas) {
        btnMas.addEventListener('click', () => {
            cargarMas(query, tag, dev, devName);
        });
    }

    // Cargar la primera página y aplicar el filtro inicial.
    cargarYFiltrarJuegos(query, tag, dev, devName);

    // Escuchar el evento de retroceso/avance del navegador para mantener sincronizados los resultados
    window.addEventListener('popstate', () => {
        const nuevosParams = new URLSearchParams(window.location.search);
        const nuevaQuery = nuevosParams.get('q') || '';
        const nuevoTag = nuevosParams.get('tag') || '';
        const nuevoDev = nuevosParams.get('dev') || '';
        const nuevoDevName = nuevosParams.get('devName') || '';
        if (searchInput) {
            if (nuevoTag || nuevoDev) {
                searchInput.value = '';
            } else {
                searchInput.value = nuevaQuery;
            }
        }
        cargarYFiltrarJuegos(nuevaQuery, nuevoTag, nuevoDev, nuevoDevName, 0, true);
    });
}

/**
 * Solicita la página siguiente y la añade a los resultados ya mostrados
 * (usado por el botón "Cargar más").
 *
 * @param {string} query - Término de búsqueda.
 * @param {string} tag - Etiqueta para filtrar opcionalmente.
 * @param {string} dev - ID de desarrolladora para filtrar opcionalmente.
 * @param {string} devName - Nombre de desarrolladora para filtrar opcionalmente.
 */
export function cargarMas(query, tag, dev, devName) {
    if (cargando || page + 1 >= totalPages) {
        return;
    }
    cargarYFiltrarJuegos(query, tag, dev, devName, page + 1, false);
}

/**
 * Carga una página de juegos de la API y aplica el filtrado sobre la caché
 * acumulada. Cuando `reiniciar` es true (nueva búsqueda) se vacía la caché y
 * se solicita la página 0.
 *
 * @param {string} query - Término de búsqueda inicial.
 * @param {string} tag - Etiqueta para filtrar opcionalmente.
 * @param {string} dev - ID de desarrolladora para filtrar opcionalmente.
 * @param {string} devName - Nombre de desarrolladora para filtrar opcionalmente.
 * @param {number} [pagina] - Página a solicitar (por defecto la actual).
 * @param {boolean} [reiniciar] - Si true, reinicia la paginación y la caché.
 */
export async function cargarYFiltrarJuegos(query, tag, dev, devName, pagina = page, reiniciar = false) {
    if (reiniciar) {
        page = 0;
        cacheJuegos = [];
        totalPages = 1;
    }

    if (cargando) {
        return;
    }

    pagina = Number.isInteger(pagina) ? pagina : page;
    page = pagina;
    ultimaBusqueda = { query, tag, dev, devName };
    cargando = true;

    const loader = document.getElementById('search-loader');
    const grid = document.getElementById('results-grid');
    const btnMas = document.getElementById('btn-cargar-mas');

    if (reiniciar) {
        if (loader) loader.style.display = 'flex';
        if (grid) grid.style.display = 'none';
        if (btnMas) btnMas.style.display = 'none';
    }
    if (btnMas) btnMas.disabled = true;

    const API_URL = `${GameBoXDApi.BASE_URL}/videojuegos`;
    const url = `${API_URL}?page=${page}&size=${size}`;

    try {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error('Error al conectar con la base de datos de videojuegos');
        }
        const datos = await response.json();

        // Cabecera con el total de juegos (la simulan las pruebas; un backend
        // con paginación real la enviaría junto al array de la página).
        const cabeceraTotal = response.headers?.get?.('X-Total-Count');

        let juegosPagina;
        if (Array.isArray(datos)) {
            if (cabeceraTotal) {
                // Backend con paginación real: `datos` ya es el segmento de
                // esta página; el total viene en la cabecera.
                juegosPagina = datos;
                totalPages = Math.ceil(parseInt(cabeceraTotal, 10) / size) || 1;
            } else {
                // Backend sin paginación todavía: devuelve el catálogo completo.
                // El frontend se queda con el segmento de esta página y calcula
                // el total de páginas por cliente.
                const desde = page * size;
                juegosPagina = datos.slice(desde, desde + size);
                totalPages = Math.ceil(datos.length / size) || 1;
            }
        } else {
            // Respuesta paginada del backend: { content, totalElements, totalPages }.
            juegosPagina = datos.content || [];
            totalPages = datos.totalPages ?? 1;
        }

        // Filtrar juegos inválidos (sin nombre o imagen) antes de acumularlos.
        const juegosValidos = filtrarJuegosValidos(juegosPagina);

        // Evitar duplicados si el backend ignora `page` y repite juegos.
        const idsExistentes = new Set(cacheJuegos.map((juego) => juego.miId));
        cacheJuegos.push(...juegosValidos.filter((juego) => !idsExistentes.has(juego.miId)));

        filtrarYRenderizar(query, tag, dev, devName);
        actualizarBotonCargarMas();
    } catch (error) {
        if (loader) loader.style.display = 'none';
        if (grid) grid.style.display = 'grid';
        mostrarErrorConPistachos(error, () =>
            cargarYFiltrarJuegos(query, tag, dev, devName, 0, true),
        );
    } finally {
        cargando = false;
        actualizarBotonCargarMas();
    }
}

/**
 * Actualiza la visibilidad y estado del botón "Cargar más" según las páginas restantes.
 */
export function actualizarBotonCargarMas() {
    const contenedor = document.getElementById('contenedor-cargar-mas');
    const btnMas = document.getElementById('btn-cargar-mas');
    if (!contenedor || !btnMas) {
        return;
    }

    const hayMasPaginas = page + 1 < totalPages;
    contenedor.style.display = hayMasPaginas ? '' : 'none';
    btnMas.disabled = !hayMasPaginas;
    btnMas.textContent = hayMasPaginas ? 'Cargar más' : 'No hay más juegos';
}

/**
 * Filtra el array de juegos para descartar aquellos que no tengan nombre o imagen.
 *
 * @param {Object[]} juegos - Array de videojuegos devuelto por la API (o una página).
 * @returns {Object[]} Array de videojuegos válidos.
 */
export function filtrarJuegosValidos(juegos) {
    return juegos.filter(juego =>
        juego.nombre && juego.nombre.trim() !== '' &&
        tieneUrlImagen(juego.urlImagen)
    );
}

/**
 * Filtra localmente los juegos en caché acumulada y los renderiza en la cuadrícula.
 *
 * @param {string} query - Término de búsqueda.
 * @param {string} tag - Etiqueta opcional para filtrar.
 * @param {string} dev - ID de desarrolladora opcional para filtrar.
 * @param {string} devName - Nombre de desarrolladora opcional para filtrar.
 */
export function filtrarYRenderizar(query, tag, dev, devName) {
    const loader = document.getElementById('search-loader');
    const grid = document.getElementById('results-grid');
    const infoText = document.getElementById('results-info-text');

    if (loader) loader.style.display = 'none';
    if (grid) grid.style.display = 'grid';

    let juegosFiltrados = [];

    if (dev) {
        const devIdInt = parseInt(dev);
        juegosFiltrados = cacheJuegos.filter(juego =>
            parseInt(juego.idDesarrolladora) === devIdInt
        );
    } else if (tag) {
        const tagLimpia = tag.trim().toLowerCase();
        juegosFiltrados = cacheJuegos.filter(juego =>
            juego.tags && juego.tags.some(t => t.toLowerCase() === tagLimpia)
        );
    } else {
        const queryLimpia = query.trim().toLowerCase();
        // Filtrado: coincidencia parcial case-insensitive del título del videojuego
        juegosFiltrados = cacheJuegos.filter(juego =>
            juego.nombre.toLowerCase().includes(queryLimpia)
        );
    }

    // Actualizar texto informativo de resultados
    if (infoText) {
        if (dev) {
            infoText.innerHTML = `Juegos desarrollados por "<span>${escapeHTML(devName || 'Desarrolladora')}</span>" (${juegosFiltrados.length} encontrados)`;
        } else if (tag) {
            infoText.innerHTML = `Juegos con la etiqueta "<span>${escapeHTML(tag)}</span>" (${juegosFiltrados.length} encontrados)`;
        } else if (query.trim()) {
            infoText.innerHTML = `Resultados para "<span>${escapeHTML(query)}</span>" (${juegosFiltrados.length} encontrados)`;
        } else {
            infoText.innerHTML = `Todos los videojuegos (${juegosFiltrados.length})`;
        }
    }

    // Vaciar resultados anteriores antes de renderizar la caché acumulada filtrada.
    if (grid) {
        grid.innerHTML = '';

        if (juegosFiltrados.length === 0) {
            // Mostrar pantalla de sin resultados
            grid.style.display = 'block'; // Para centrar el contenedor de no resultados
            let searchLabel = query;
            if (dev) {
                searchLabel = `desarrolladora: ${devName || dev}`;
            } else if (tag) {
                searchLabel = `etiqueta: ${tag}`;
            }
            grid.appendChild(crearVistaSinResultados(searchLabel));
        } else {
            grid.style.display = 'grid';
            juegosFiltrados.forEach(juego => {
                const tarjeta = crearTarjetaJuego(juego);
                grid.appendChild(tarjeta);
            });
        }
    }
}

/**
 * Crea el nodo HTML correspondiente a una tarjeta de videojuego.
 *
 * @param {Object} juego - Datos del videojuego.
 * @returns {HTMLElement} Nodo HTML de la tarjeta de juego.
 */
export function crearTarjetaJuego(juego) {
    const enlace = document.createElement('a');
    enlace.href = `detalleJuego.html?id=${juego.miId}`;
    enlace.className = 'game-search-card';
    enlace.title = juego.nombre;

    // Contenedor de la imagen
    const imgContainer = document.createElement('div');
    imgContainer.className = 'card-img-container';

    const img = document.createElement('img');
    aplicarImagen(img, juego.urlImagen);
    img.alt = juego.nombre;
    img.loading = 'lazy'; // Carga diferida de imágenes para optimizar rendimiento

    imgContainer.appendChild(img);

    // Título del juego
    const titulo = document.createElement('div');
    titulo.className = 'game-card-title';
    titulo.textContent = juego.nombre;

    enlace.appendChild(imgContainer);
    enlace.appendChild(titulo);

    return enlace;
}

/**
 * Realiza una búsqueda actualizando la URL de forma fluida (sin recargar la página)
 * y volviendo a la página 0 de resultados.
 *
 * @param {string} query - Término de búsqueda.
 */
export function realizarBusquedaInteractiva(query) {
    const nuevaUrl = `${window.location.pathname}?q=${encodeURIComponent(query)}`;
    window.history.pushState({ path: nuevaUrl }, '', nuevaUrl);

    cargarYFiltrarJuegos(query, '', '', '', 0, true);
}

/**
 * Crea la interfaz de "No se encontraron resultados" de manera premium y amigable.
 *
 * @param {string} query - Término de búsqueda fallido.
 * @returns {HTMLElement} Nodo HTML con el mensaje.
 */
export function crearVistaSinResultados(query) {
    const contenedor = document.createElement('div');
    contenedor.className = 'no-results-container';

    const img = document.createElement('img');
    img.src = 'recursos/img/search-empty.svg';
    img.className = 'no-results-img';
    img.alt = 'Sin resultados';

    const titulo = document.createElement('h3');
    titulo.textContent = 'No se encontraron resultados';

    const descripcion = document.createElement('p');
    descripcion.innerHTML = `No pudimos encontrar ningún videojuego que coincida con "<span>${escapeHTML(query)}</span>". Revisa la ortografía o intenta con palabras más generales.`;

    const botonLimpiar = document.createElement('button');
    botonLimpiar.className = 'clear-search-btn';
    botonLimpiar.textContent = 'Ver todos los videojuegos';
    botonLimpiar.type = 'button';
    botonLimpiar.addEventListener('click', () => {
        const searchInput = document.getElementById('search-input-field');
        if (searchInput) searchInput.value = '';
        realizarBusquedaInteractiva('');
    });

    contenedor.appendChild(img);
    contenedor.appendChild(titulo);
    contenedor.appendChild(descripcion);
    contenedor.appendChild(botonLimpiar);

    return contenedor;
}

/**
 * Escapa caracteres HTML para evitar ataques XSS al inyectar texto del usuario.
 *
 * @param {string} str - Cadena de texto a escapar.
 * @returns {string} Cadena de texto sanitizada.
 */
export function escapeHTML(str) {
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

/**
 * Muestra el overlay de error de la marca GameBoXD (pistachos) sin usar
 * alert(). Acepta un callback opcional `reintentar` que se dispara al hacer
 * clic en el botón "Reintentar" del overlay.
 *
 * @param {Error} error - Objeto de error capturado.
 * @param {Function} [reintentar] - Callback ejecutado al pulsar "Reintentar".
 */
export function mostrarErrorConPistachos(error, reintentar) {
    console.error('Error fetching video games:', error);

    const mensaje = error && typeof error.message === 'string' ? error.message : 'Error al conectar con la base de datos de videojuegos';
    const mensajeAmigable = `${mensaje}. Inténtalo más tarde`;

    // Evitar acumular overlays si el error se repite (Reintentar).
    const previo = document.getElementById('error-pistachos-overlay');
    if (previo) previo.remove();

    // Fondo oscuro a pantalla completa
    const errorOverlay = document.createElement('div');
    errorOverlay.id = 'error-pistachos-overlay';
    errorOverlay.style.position = 'fixed';
    errorOverlay.style.top = '0';
    errorOverlay.style.left = '0';
    errorOverlay.style.width = '100vw';
    errorOverlay.style.height = '100vh';
    errorOverlay.style.backgroundColor = 'rgba(0, 0, 0, 0.95)';
    errorOverlay.style.display = 'flex';
    errorOverlay.style.flexDirection = 'column';
    errorOverlay.style.justifyContent = 'center';
    errorOverlay.style.alignItems = 'center';
    errorOverlay.style.zIndex = '9999';
    errorOverlay.style.textAlign = 'center';
    errorOverlay.style.color = '#F8FAFC';
    errorOverlay.style.padding = '24px';

    // Imagen divertida de los pistachos
    const errorImg = document.createElement('img');
    errorImg.src = 'recursos/img/error-pistachos.svg';
    errorImg.style.maxWidth = '90%';
    errorImg.style.maxHeight = '60vh';
    errorImg.style.borderRadius = '20px';
    errorImg.style.boxShadow = '0 0 50px rgba(255, 0, 0, 0.5)';
    errorImg.style.marginBottom = '16px';

    // Mensaje amigable
    const mensajeEl = document.createElement('p');
    mensajeEl.style.fontSize = '18px';
    mensajeEl.style.fontWeight = '700';
    mensajeEl.style.margin = '0 0 20px';
    mensajeEl.textContent = mensajeAmigable;

    // Botón para reintentar la petición
    const botonReintentar = document.createElement('button');
    botonReintentar.type = 'button';
    botonReintentar.textContent = 'Reintentar';
    botonReintentar.style.background = '#00D1FF';
    botonReintentar.style.color = '#0B0E14';
    botonReintentar.style.border = 'none';
    botonReintentar.style.padding = '10px 24px';
    botonReintentar.style.borderRadius = '8px';
    botonReintentar.style.fontWeight = '700';
    botonReintentar.style.cursor = 'pointer';
    botonReintentar.addEventListener('click', () => {
        if (typeof reintentar === 'function') {
            reintentar();
        }
    });

    errorOverlay.appendChild(errorImg);
    errorOverlay.appendChild(mensajeEl);
    errorOverlay.appendChild(botonReintentar);
    document.body.appendChild(errorOverlay);
}
