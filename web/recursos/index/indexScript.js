// Acceso al gestor centralizado de imágenes definido en api.js
const { aplicarImagen, aplicarImagenResponsiva } = window.GameBoXDImagenes;

/**
 * Event listener que se ejecuta cuando el DOM está completamente cargado.
 * Inicia el proceso para obtener y mostrar recomendaciones de juegos y el
 * hero con lo más reseñado.
 */
document.addEventListener('DOMContentLoaded', () => {
    obtenerRecomendaciones();
    cargarHeroMasResenyado();
    configurarBuscador();
});

/**
 * Rellena el hero con el juego que más entradas de lista tiene en el momento
 * (GET /api/videojuegos/mas-resenyas), usando la propia carátula del juego
 * como imagen y enlazando a su ficha.
 *
 * Si el endpoint falla o todavía no hay ninguna entrada de lista, el hero se
 * queda oculto y el resto de la página sigue funcionando con normalidad.
 *
 * @returns {Promise<void>}
 */
export async function cargarHeroMasResenyado() {
    const contenedor = document.getElementById('hero-right');
    if (!contenedor) return;

    try {
        const response = await fetch(`${GameBoXDApi.BASE_URL}/videojuegos/mas-resenyas`);
        if (!response.ok) {
            throw new Error('Todavia no hay ninguna entrada de lista');
        }

        const juego = await response.json();
        if (!juego || juego.miId === undefined || juego.miId === null) {
            throw new Error('La API no devolvio ningun juego');
        }

        const enlace = document.getElementById('hero-enlace');
        const img = document.getElementById('hero-imagen');

        if (enlace) {
            enlace.href = `detalleJuego.html?id=${juego.miId}`;
        }
        if (img) {
            // La carátula del propio juego: no hace falta ninguna imagen de hero fija.
            // Con alto se recorta en el servidor a la proporción del marco (16:9):
            // pedir 600x900 para pintar 580x326 descargaba dos tercios de píxeles
            // que `object-fit: cover` tiraba.
            aplicarImagen(img, juego.urlImagen, null, 600, 338);
            // El hero es el elemento LCP de la portada: prioridad máxima.
            img.setAttribute('fetchpriority', 'high');
            img.setAttribute('loading', 'eager');
            img.setAttribute('decoding', 'async');
            img.alt = juego.nombre || '';
            img.title = juego.nombre || '';
        }

        contenedor.hidden = false;
    } catch (error) {
        console.error('Error cargando el hero:', error);
        // Sin datos fiables el hero se queda oculto: mejor nada que un juego equivocado.
        contenedor.hidden = true;
    }
}

/**
 * Configura los event listeners para la barra de búsqueda en index.html.
 * Redirige a busqueda.html con el parámetro de consulta 'q'.
 */
function configurarBuscador() {
    const searchInput = document.querySelector('.search-input');
    const searchBtn = document.querySelector('.search-icon-wrapper');

    if (searchInput) {
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                const query = searchInput.value.trim();
                if (query) {
                    window.location.href = `busqueda.html?q=${encodeURIComponent(query)}`;
                }
            }
        });
    }

    if (searchBtn && searchInput) {
        searchBtn.addEventListener('click', () => {
            const query = searchInput.value.trim();
            if (query) {
                window.location.href = `busqueda.html?q=${encodeURIComponent(query)}`;
            }
        });
    }
}


/**
 * Pide al backend una muestra aleatoria de juegos y pinta hasta cuatro.
 *
 * <p>Antes se descargaba el catálogo completo (198 juegos, ~101 KB) y se
 * elegían cuatro al azar en el navegador. Ahora el `$sample` lo hace MongoDB y
 * solo viajan cuatro resúmenes (~2 KB): la portada carga una décima parte de
 * datos para el mismo resultado visual.</p>
 *
 * @returns {Promise<void>}
 */
export async function obtenerRecomendaciones() {
    try {
        const response = await fetch(`${GameBoXDApi.BASE_URL}/videojuegos/recomendaciones?limit=4`);
        if (!response.ok) {
            throw new Error('Error de red al intentar obtener las recomendaciones');
        }

        const recomendaciones = await response.json();
        renderizarRecomendaciones(Array.isArray(recomendaciones) ? recomendaciones : []);
    } catch (error) {
        mostrarErrorConPistachos(error);
    }
}

/**
 * Inserta las imágenes de los juegos recomendados en sus respectivos contenedores HTML.
 * Genera enlaces a la página de detalles para cada juego.
 * 
 * @param {Object[]} recomendaciones - Array de videojuegos a renderizar.
 */
function renderizarRecomendaciones(recomendaciones) {
    // Insertamos las imágenes en el HTML
    recomendaciones.forEach((juego, index) => {
        const contenedorId = `r${index + 1}`;
        const contenedor = document.getElementById(contenedorId);

        if (contenedor) {
            const enlace = document.createElement('a');
            enlace.href = `detalleJuego.html?id=${juego.miId}`;

            const img = document.createElement('img');
            // La tarjeta se pinta a ~260 CSS px en escritorio (4 columnas dentro
            // de un contenedor de 1120 px), a ~45vw entre 601 y 900 px y casi a
            // todo el ancho por debajo de 600. Pedir siempre 300 era un 15% de
            // sobremuestreo en escritorio: con el candidato de 260 las cuatro
            // portadas pasan de 141 096 B a 101 084 B (-28,4%). En retina y en
            // móvil el navegador sigue eligiendo 300, exactamente lo de antes.
            aplicarImagenResponsiva(img, juego.urlImagen, null, [200, 260, 300],
                '(max-width: 600px) 100vw, (max-width: 900px) 45vw, 260px');
            img.alt = juego.nombre;
            img.title = juego.nombre;
            // La primera tarjeta se pinta con el resto de la página; las demás
            // se cargan al acercarse al viewport.
            img.setAttribute('loading', index === 0 ? 'eager' : 'lazy');
            img.setAttribute('decoding', 'async');
            // La portada compite con el hero por el ancho de banda: la primera
            // se marca en baja prioridad y el hero (LCP) en alta.
            img.setAttribute('fetchpriority', 'low');

            enlace.appendChild(img);
            contenedor.appendChild(enlace);
        }
    });
}

/**
 * Maneja los errores de conexión de red o API mostrando una alerta
 * y una capa oscura a pantalla completa con una imagen humorística.
 * 
 * @param {Error} error - El objeto de error capturado.
 */
function mostrarErrorConPistachos(error) {
    console.error('Error fetching video games:', error);

    // Mostrar la alerta
    alert('enciende la api tontito');

    // Crear un fondo oscuro para resaltar la imagen
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

    // Crear y añadir la imagen
    const errorImg = document.createElement('img');
    errorImg.src = 'recursos/img/error-pistachos.svg';
    errorImg.style.maxWidth = '90%';
    errorImg.style.maxHeight = '90%';
    errorImg.style.borderRadius = '20px';
    errorImg.style.boxShadow = '0 0 50px rgba(255, 0, 0, 0.5)'; // Brillo rojo de alerta

    errorOverlay.appendChild(errorImg);
    document.body.appendChild(errorOverlay);
}