/**
 * Event listener que se ejecuta cuando el DOM está completamente cargado.
 * Inicia la carga de los detalles del juego si se proporciona un ID en la URL.
 */
document.addEventListener('DOMContentLoaded', () => {
    const idJuego = obtenerIdDeUrl();

    if (idJuego) {
        console.log("ID del juego recibido:", idJuego);
        cargarDetallesJuego(idJuego);
        cargarResenyasJuego(idJuego);
        verificarSesionYConfigurarBoton(idJuego);
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
    const API_URL = `https://gameboxd.duckdns.org/api/videojuegos/${idJuego}`;
    
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
    if (juego.idDesarrolladora) {
        cargarDesarrolladora(juego.idDesarrolladora);
    }
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
    document.getElementById('reviews-wrapper').style.display = 'block';
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

/**
 * Realiza una petición a la API para obtener las reseñas del videojuego.
 * 
 * @param {string} idJuego - El ID del videojuego.
 */
function cargarResenyasJuego(idJuego) {
    const API_URL = `https://gameboxd.duckdns.org/api/lista/juego/${idJuego}`;
    
    fetch(API_URL)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error de red al intentar obtener las reseñas del juego');
            }
            return response.json();
        })
        .then(resenyas => {
            console.log("Reseñas obtenidas:", resenyas);
            renderizarResenyas(resenyas);
        })
        .catch(error => {
            console.error(error);
            // Si falla, mostramos el contenedor indicando que no hay reseñas
            renderizarResenyas([]);
        });
}

/**
 * Renderiza las reseñas del juego en la página.
 * 
 * @param {Object[]} resenyas - Array de objetos de entradas de lista.
 */
function renderizarResenyas(resenyas) {
    const container = document.getElementById('reviews-container');
    container.innerHTML = '';
    
    // Filtrar reseñas que tengan texto
    const resenyasConTexto = resenyas.filter(r => r.resenya && r.resenya.trim() !== '');

    if (resenyasConTexto.length === 0) {
        const msg = document.createElement('div');
        msg.className = 'no-reviews-msg';
        msg.textContent = 'Aún no hay reseñas para este juego.';
        container.appendChild(msg);
    } else {
        resenyasConTexto.forEach(r => {
            const card = document.createElement('div');
            card.className = 'review-card';
            
            const rating = document.createElement('div');
            rating.className = 'review-rating';
            rating.textContent = r.nota.toFixed(1);
            
            const content = document.createElement('div');
            content.className = 'review-content';
            content.textContent = r.resenya;
            
            card.appendChild(rating);
            card.appendChild(content);
            container.appendChild(card);
        });
    }
}

/**
 * Petición a la API para obtener los datos de la desarrolladora.
 * 
 * @param {number} idDesarrolladora - El ID de la desarrolladora.
 */
function cargarDesarrolladora(idDesarrolladora) {
    const API_URL = `https://gameboxd.duckdns.org/api/desarrolladoras/id/${idDesarrolladora}`;
    
    fetch(API_URL)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error al obtener la desarrolladora');
            }
            return response.json();
        })
        .then(desarrolladora => {
            renderizarDesarrolladora(desarrolladora);
        })
        .catch(error => {
            console.error("Error cargando desarrolladora:", error);
            // Si falla, la caja game-dev-box permanecerá oculta por defecto
        });
}

/**
 * Renderiza la información de la desarrolladora en la interfaz.
 * 
 * @param {Object} desarrolladora - Datos de la desarrolladora.
 */
function renderizarDesarrolladora(desarrolladora) {
    document.getElementById('game-dev-box').style.display = 'block';
    
    const logoImg = document.getElementById('dev-logo');
    if (desarrolladora.urlImagen === "placeholder" || !desarrolladora.urlImagen) {
        logoImg.style.display = 'none'; // Ocultar imagen si es placeholder
    } else {
        logoImg.src = desarrolladora.urlImagen;
    }
    
    document.getElementById('dev-name').textContent = desarrolladora.nombre;
    document.getElementById('dev-country').textContent = desarrolladora.pais;
}

let idUsuarioLogueado = null;

/**
 * Verifica si hay una sesión iniciada y configura el botón "Añadir a mi lista".
 * Si no hay sesión, oculta el botón.
 * @param {string} idJuego - El ID del juego actual.
 */
function verificarSesionYConfigurarBoton(idJuego) {
    const token = localStorage.getItem('jwt_token');
    const btnAdd = document.getElementById('add-to-list-btn');
    
    if (!token) {
        if (btnAdd) btnAdd.style.display = 'none';
        return;
    }

    try {
        const payloadBase64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        const payloadDecoded = JSON.parse(atob(payloadBase64));
        const username = payloadDecoded.sub;

        fetch(`https://gameboxd.duckdns.org/api/usuarios/nombre/${username}`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        })
        .then(response => {
            if (!response.ok) throw new Error('Failed to fetch user data');
            return response.json();
        })
        .then(usuario => {
            idUsuarioLogueado = usuario.miId;
            
            if (btnAdd) {
                btnAdd.addEventListener('click', () => {
                    anyadirALista(idJuego, idUsuarioLogueado, token);
                });
            }
        })
        .catch(err => {
            console.error('Error fetching logged in user:', err);
            if (btnAdd) btnAdd.style.display = 'none';
        });
    } catch (e) {
        console.error('Error decoding token', e);
        if (btnAdd) btnAdd.style.display = 'none';
    }
}

/**
 * Realiza la petición POST para crear una nueva entrada en la lista del usuario.
 */
function anyadirALista(idJuego, idUsuario, token) {
    const nuevaEntrada = {
        horasJugadas: 0,
        nota: 0.0,
        resenya: "",
        estado: "noEmpezado",
        idVideojuego: parseInt(idJuego),
        idUsuario: idUsuario,
        miId: 0
    };

    fetch('https://gameboxd.duckdns.org/api/lista', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(nuevaEntrada)
    })
    .then(response => {
        if (response.ok) {
            alert('¡Juego añadido a tu lista con éxito!');
        } else {
            alert('Hubo un error al añadir el juego a tu lista. Quizás ya esté añadido.');
        }
    })
    .catch(error => {
        console.error('Error adding to list:', error);
        alert('Error de conexión al añadir a la lista.');
    });
}