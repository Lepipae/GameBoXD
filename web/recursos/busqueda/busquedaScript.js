/**
 * Script para controlar la lógica de búsqueda y renderizado de resultados en busqueda.html.
 * Obtiene los juegos de la API y los filtra localmente para un rendimiento óptimo.
 */

// Caché global de videojuegos para evitar múltiples peticiones innecesarias
let cacheJuegos = [];

document.addEventListener('DOMContentLoaded', () => {
    inicializarBusqueda();
});

/**
 * Inicializa el buscador: obtiene los parámetros de la URL, configura los elementos del DOM y
 * desencadena la carga inicial de los juegos.
 */
function inicializarBusqueda() {
    const params = new URLSearchParams(window.location.search);
    const query = params.get('q') || '';

    // Elementos del DOM
    const searchInput = document.getElementById('search-input-field');
    const searchBtn = document.getElementById('btn-buscar');

    // Pre-rellenar el input con la búsqueda actual
    if (searchInput) {
        searchInput.value = query;
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

    // Cargar juegos y aplicar el filtro inicial
    cargarYFiltrarJuegos(query);

    // Escuchar el evento de retroceso/avance del navegador para mantener sincronizados los resultados
    window.addEventListener('popstate', () => {
        const nuevosParams = new URLSearchParams(window.location.search);
        const nuevaQuery = nuevosParams.get('q') || '';
        if (searchInput) {
            searchInput.value = nuevaQuery;
        }
        filtrarYRenderizar(nuevaQuery);
    });
}

/**
 * Realiza una búsqueda actualizando la URL de forma fluida (sin recargar la página)
 * y filtrando instantáneamente a partir de los datos en caché.
 * 
 * @param {string} query - Término de búsqueda.
 */
function realizarBusquedaInteractiva(query) {
    const nuevaUrl = `${window.location.pathname}?q=${encodeURIComponent(query)}`;
    window.history.pushState({ path: nuevaUrl }, '', nuevaUrl);
    
    filtrarYRenderizar(query);
}

/**
 * Carga todos los juegos de la API si no están en caché, y luego aplica el filtrado.
 * 
 * @param {string} query - Término de búsqueda inicial.
 */
function cargarYFiltrarJuegos(query) {
    if (cacheJuegos.length > 0) {
        filtrarYRenderizar(query);
        return;
    }

    const API_URL = 'https://gameboxd.duckdns.org/api/videojuegos';
    const loader = document.getElementById('search-loader');
    const grid = document.getElementById('results-grid');

    if (loader) loader.style.display = 'flex';
    if (grid) grid.style.display = 'none';

    fetch(API_URL)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error al conectar con la base de datos de videojuegos');
            }
            return response.json();
        })
        .then(juegos => {
            // Filtrar juegos inválidos (sin imagen o nombre)
            cacheJuegos = filtrarJuegosValidos(juegos);
            filtrarYRenderizar(query);
        })
        .catch(error => {
            if (loader) loader.style.display = 'none';
            mostrarErrorConPistachos(error);
        });
}

/**
 * Filtra el array de juegos para descartar aquellos que no posean una URL de imagen o nombre válido.
 * 
 * @param {Object[]} juegos - Array completo de videojuegos devueltos por la API.
 * @returns {Object[]} Array de videojuegos válidos.
 */
function filtrarJuegosValidos(juegos) {
    return juegos.filter(juego => 
        juego.nombre && juego.nombre.trim() !== '' && 
        juego.urlImagen && juego.urlImagen.trim() !== ''
    );
}

/**
 * Filtra localmente los juegos en caché y los renderiza en la cuadrícula.
 * 
 * @param {string} query - Término de búsqueda.
 */
function filtrarYRenderizar(query) {
    const loader = document.getElementById('search-loader');
    const grid = document.getElementById('results-grid');
    const infoText = document.getElementById('results-info-text');

    if (loader) loader.style.display = 'none';
    if (grid) grid.style.display = 'grid';

    const queryLimpia = query.trim().toLowerCase();
    
    // Filtrado: coincidencia parcial case-insensitive del título del videojuego
    const juegosFiltrados = cacheJuegos.filter(juego => 
        juego.nombre.toLowerCase().includes(queryLimpia)
    );

    // Actualizar texto informativo de resultados
    if (infoText) {
        if (queryLimpia) {
            infoText.innerHTML = `Resultados para "<span>${escapeHTML(query)}</span>" (${juegosFiltrados.length} encontrados)`;
        } else {
            infoText.innerHTML = `Todos los videojuegos (${juegosFiltrados.length})`;
        }
    }

    // Vaciar resultados anteriores
    if (grid) {
        grid.innerHTML = '';

        if (juegosFiltrados.length === 0) {
            // Mostrar pantalla de sin resultados
            grid.style.display = 'block'; // Para centrar el contenedor de no resultados
            grid.appendChild(crearVistaSinResultados(query));
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
function crearTarjetaJuego(juego) {
    const enlace = document.createElement('a');
    enlace.href = `detalleJuego.html?id=${juego.miId}`;
    enlace.className = 'game-search-card';
    enlace.title = juego.nombre;

    // Contenedor de la imagen
    const imgContainer = document.createElement('div');
    imgContainer.className = 'card-img-container';

    const img = document.createElement('img');
    img.src = juego.urlImagen;
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
 * Crea la interfaz de "No se encontraron resultados" de manera premium y amigable.
 * 
 * @param {string} query - Término de búsqueda fallido.
 * @returns {HTMLElement} Nodo HTML con el mensaje.
 */
function crearVistaSinResultados(query) {
    const contenedor = document.createElement('div');
    contenedor.className = 'no-results-container';

    const img = document.createElement('img');
    img.src = 'https://gameboxd.s3.us-east-1.amazonaws.com/web/search.png';
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
function escapeHTML(str) {
    return str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

/**
 * Maneja los errores de conexión de red mostrando una alerta humorística 
 * y la imagen de los pistachos para consistencia de marca en GameBoXD.
 * 
 * @param {Error} error - Objeto de error capturado.
 */
function mostrarErrorConPistachos(error) {
    console.error('Error fetching video games:', error);

    // Alerta original y divertida del proyecto
    alert('enciende la api tontito');

    // Fondo oscuro a pantalla completa
    const errorOverlay = document.createElement('div');
    errorOverlay.style.position = 'fixed';
    errorOverlay.style.top = '0';
    errorOverlay.style.left = '0';
    errorOverlay.style.width = '100vw';
    errorOverlay.style.height = '100vh';
    errorOverlay.style.backgroundColor = 'rgba(0, 0, 0, 0.95)';
    errorOverlay.style.display = 'flex';
    errorOverlay.style.justifyContent = 'center';
    errorOverlay.style.alignItems = 'center';
    errorOverlay.style.zIndex = '9999';

    // Imagen divertida de los pistachos
    const errorImg = document.createElement('img');
    errorImg.src = 'https://gameboxd.s3.us-east-1.amazonaws.com/web/pistachos.png';
    errorImg.style.maxWidth = '90%';
    errorImg.style.maxHeight = '90%';
    errorImg.style.borderRadius = '20px';
    errorImg.style.boxShadow = '0 0 50px rgba(255, 0, 0, 0.5)';

    errorOverlay.appendChild(errorImg);
    document.body.appendChild(errorOverlay);
}
