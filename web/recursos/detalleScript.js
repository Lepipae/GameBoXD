/**
 * Event listener que se ejecuta cuando el DOM está completamente cargado.
 * Inicia la carga de los detalles del juego si se proporciona un ID en la URL.
 */
document.addEventListener('DOMContentLoaded', () => {
    const idJuego = obtenerIdDeUrl();

    if (idJuego) {
        console.log("ID del juego recibido:", idJuego);
        cargarDetallesJuego(idJuego);
    } else {
        mostrarError("No se ha especificado ningún juego.");
    }
});

/**
 * Extrae el identificador del juego de los parámetros de la URL.
 * 
 * @returns {string|null} El ID del juego si está presente en la URL, de lo contrario null.
 */
function obtenerIdDeUrl() {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get('id');
}

/**
 * Realiza una petición a la API para obtener los datos completos del videojuego.
 * 
 * @param {string} idJuego - El ID del videojuego a cargar.
 */
function cargarDetallesJuego(idJuego) {
    const API_URL = `http://localhost:8080/api/videojuegos/${idJuego}`;
    
    fetch(API_URL)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error de red al intentar obtener los detalles del juego');
            }
            return response.json();
        })
        .then(juego => {
            console.log("Datos del juego obtenidos:", juego);
            renderizarJuego(juego);
        })
        .catch(error => {
            console.error(error);
            mostrarError("Error al cargar los datos del juego.");
        });
}

/**
 * Coordina la renderización de todos los elementos visuales del juego en la página.
 * 
 * @param {Object} juego - Objeto con los datos del videojuego obtenidos de la API.
 */
function renderizarJuego(juego) {
    ocultarCargando();
    mostrarContenedor();
    cambiarTitulo(juego.nombre);
    renderizarInformacionBasica(juego);
    renderizarTags(juego.tags);
    renderizarValoracion(juego.notaMedia);
}

/**
 * Muestra un mensaje de error en la pantalla y oculta el contenedor principal.
 * 
 * @param {string} mensaje - El mensaje de error que se mostrará al usuario.
 */
function mostrarError(mensaje) {
    console.log(mensaje);
    const loadingState = document.getElementById('loading-state');
    loadingState.textContent = mensaje;
    loadingState.style.display = 'block';
    document.getElementById('game-detail-wrapper').style.display = 'none';
}

/**
 * Oculta el mensaje de estado de carga inicial.
 */
function ocultarCargando() {
    document.getElementById('loading-state').style.display = 'none';
}

/**
 * Muestra el contenedor principal de los detalles del juego mediante Flexbox.
 */
function mostrarContenedor() {
    document.getElementById('game-detail-wrapper').style.display = 'flex';
}

/**
 * Actualiza el texto del elemento de título principal de la página.
 * 
 * @param {string} nombreJuego - El nombre del videojuego a mostrar en el título.
 */
function cambiarTitulo(nombreJuego) {
    document.getElementById('titulo').textContent = nombreJuego;
}

/**
 * Actualiza la imagen de portada y la descripción textual del videojuego.
 * 
 * @param {Object} juego - Objeto con los datos del videojuego.
 * @param {string} juego.urlImagen - La URL de la imagen de portada.
 * @param {string} juego.descripcion - El texto de descripción del juego.
 */
function renderizarInformacionBasica(juego) {
    document.getElementById('game-cover').src = juego.urlImagen;
    document.getElementById('game-desc').textContent = juego.descripcion;
}

/**
 * Genera e inserta en el DOM las etiquetas (tags) correspondientes al videojuego.
 * 
 * @param {string[]} tags - Array de cadenas de texto con las categorías o etiquetas del juego.
 */
function renderizarTags(tags) {
    const tagsContainer = document.getElementById('game-tags');
    tagsContainer.innerHTML = ''; // Limpiar tags previos si los hubiera
    if (tags && tags.length > 0) {
        tags.forEach(tag => {
            const tagEl = document.createElement('span');
            tagEl.className = 'game-tag';
            tagEl.textContent = tag;
            tagsContainer.appendChild(tagEl);
        });
    }
}

/**
 * Renderiza la valoración del juego visualmente, utilizando un sistema de 5 estrellas
 * calculado a partir de una nota media sobre 10.
 * 
 * @param {number} notaMedia - La puntuación media del juego (de 0 a 10).
 */
function renderizarValoracion(notaMedia) {
    const ratingContainer = document.getElementById('game-rating');
    ratingContainer.innerHTML = ''; // Limpiar valoración previa
    
    const ratingNumber = document.createElement('div');
    ratingNumber.className = 'rating-number';
    ratingNumber.textContent = notaMedia.toFixed(1);
    
    const starsContainer = document.createElement('div');
    starsContainer.className = 'rating-stars';
    
    const starsOutOf5 = notaMedia / 2;
    for (let i = 1; i <= 5; i++) {
        const star = document.createElement('div');
        star.className = 'star';
        if (starsOutOf5 >= i) {
            star.classList.add('filled');
        } else if (starsOutOf5 >= i - 0.5) {
            star.classList.add('half-filled');
        }
        starsContainer.appendChild(star);
    }
    
    ratingContainer.appendChild(ratingNumber);
    ratingContainer.appendChild(starsContainer);
}