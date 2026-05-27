/**
 * Event listener que se ejecuta cuando el DOM está completamente cargado.
 * Inicia el proceso para obtener y mostrar recomendaciones de juegos.
 */
document.addEventListener('DOMContentLoaded', () => {
    obtenerRecomendaciones(); 
});

/**
 * Realiza la petición principal a la API para obtener la lista completa de videojuegos.
 * Luego filtra los válidos, escoge recomendaciones al azar y las renderiza.
 * En caso de error, muestra una pantalla de fallo.
 */
function obtenerRecomendaciones() {
    const API_URL = 'https://gameboxd.duckdns.org/api/videojuegos';

    fetch(API_URL)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error de red al intentar obtener los videojuegos');
            }
            return response.json();
        })
        .then(juegos => {
            const juegosConImagen = filtrarJuegosValidos(juegos);
            const recomendaciones = seleccionarRecomendacionesAleatorias(juegosConImagen, 4);
            renderizarRecomendaciones(recomendaciones);
        })
        .catch(error => {
            mostrarErrorConPistachos(error);
        });
}

/**
 * Filtra el array de juegos para descartar aquellos que no posean una URL de imagen válida.
 * 
 * @param {Object[]} juegos - Array completo de videojuegos devueltos por la API.
 * @returns {Object[]} Array de videojuegos que contienen un campo urlImagen no vacío.
 */
function filtrarJuegosValidos(juegos) {
    // Nos quedamos solo con los juegos que tienen una imagen válida
    return juegos.filter(juego => juego.urlImagen && juego.urlImagen.trim() !== '');
}

/**
 * Selecciona aleatoriamente un número específico de juegos sin que se repitan.
 * 
 * @param {Object[]} juegos - Array de videojuegos elegibles.
 * @param {number} cantidad - Número de recomendaciones que se desean obtener.
 * @returns {Object[]} Array con los videojuegos seleccionados aleatoriamente.
 */
function seleccionarRecomendacionesAleatorias(juegos, cantidad) {
    const cantidadNecesaria = Math.min(cantidad, juegos.length);
    const recomendaciones = [];

    // Seleccionamos al azar sin repetir
    while (recomendaciones.length < cantidadNecesaria) {
        const indiceAleatorio = Math.floor(Math.random() * juegos.length);
        const juegoCandidato = juegos[indiceAleatorio];

        if (!recomendaciones.includes(juegoCandidato)) {
            recomendaciones.push(juegoCandidato);
        }
    }
    return recomendaciones;
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
            img.src = juego.urlImagen;
            img.alt = juego.nombre;
            img.title = juego.nombre;

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
    errorImg.src = 'https://gameboxd.s3.us-east-1.amazonaws.com/web/pistachos.png';
    errorImg.style.maxWidth = '90%';
    errorImg.style.maxHeight = '90%';
    errorImg.style.borderRadius = '20px';
    errorImg.style.boxShadow = '0 0 50px rgba(255, 0, 0, 0.5)'; // Brillo rojo de alerta

    errorOverlay.appendChild(errorImg);
    document.body.appendChild(errorOverlay);
}