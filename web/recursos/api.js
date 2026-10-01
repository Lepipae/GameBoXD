/**
 * Dirección de la API de GameBoXD.
 *
 * Antes esta cabecera interceptaba window.fetch y lanzaba cada petición contra dos
 * servidores a la vez, quedándose con el primero que respondiera. El primario
 * (gameboxd.duckdns.org) está retirado y su certificado TLS no cuadra con el
 * dominio, así que fallaba el 100% de las veces: la mitad del tráfico de cada
 * página se perdía en un handshake TLS descartado antes de que el secundario
 * respondiera, y la consola se llenaba de errores TLS duplicados.
 *
 * Ahora hay un único servidor, y su URL vive aquí para no repetirla en los seis
 * scripts que hacen llamadas. Si algún día vuelve a haber dos, el punto único de
 * cambio es esta constante.
 */
(function () {
    const BASE_URL = 'https://gameboxd-2nio.onrender.com/api';

    window.GameBoXDApi = {
        BASE_URL: BASE_URL
    };
})();

/**
 * Gestor centralizado de imágenes de GameBoXD.
 *
 * Las URLs de portada las introduce el usuario al crear un juego, así que pueden
 * apuntar a hosts que ya no existen (el antiguo bucket de S3 fue retirado).
 * El backend garantiza que el campo urlImagen está vacío o contiene una URL
 * http(s) válida; este gestor cubre el otro caso inevitable: que la URL sea
 * válida pero el host ya no responda, y degrade a una portada local.
 */
(function () {
    /** Portada local usada siempre que el juego no tenga imagen o esta falle al cargar. */
    const IMAGEN_PLACEHOLDER = 'recursos/img/placeholder-game.svg';

    /**
     * Indica si el registro trae una imagen propia.
     *
     * Antes esta función también descartaba el texto "placeholder" que el backend
     * guardaba en el campo URL. Ese literal ya no existe: el backend almacena null
     * cuando no hay imagen y rechaza cualquier valor que no sea una URL http(s).
     *
     * @param {*} url - Valor a comprobar.
     * @returns {boolean} true si hay una URL de imagen.
     */
    function tieneUrlImagen(url) {
        return typeof url === 'string' && url.trim() !== '';
    }

    /**
     * Asigna la imagen a un elemento <img> con respaldo local.
     *
     * El listener de "error" se desconecta a sí mismo antes de reintentar, de modo
     * que si el placeholder local fallara alguna vez no entraría en un bucle
     * infinito de peticiones.
     *
     * @param {HTMLImageElement} img - Elemento <img> destino.
     * @param {string} url - URL remota candidata; si no es válida se usa el placeholder.
     * @param {Function} [alFallar] - Callback opcional que sustituye al placeholder
     *        (por ejemplo, para ocultar el logo de una desarrolladora sin logo real).
     */
    function aplicarImagen(img, url, alFallar) {
        if (!img) return;

        const recurrir = function () {
            if (typeof alFallar === 'function') {
                alFallar(img);
            } else {
                img.src = IMAGEN_PLACEHOLDER;
            }
        };

        // Sin URL no hay ni una petición: se muestra la portada local directamente.
        if (!tieneUrlImagen(url)) {
            recurrir();
            return;
        }

        img.addEventListener('error', function manejarErrorImagen() {
            img.removeEventListener('error', manejarErrorImagen);
            recurrir();
        });

        img.src = url.trim();
    }

    window.GameBoXDImagenes = {
        IMAGEN_PLACEHOLDER: IMAGEN_PLACEHOLDER,
        tieneUrlImagen: tieneUrlImagen,
        aplicarImagen: aplicarImagen
    };
})();

/**
 * Lector de mensajes de error de la API.
 *
 * La API responde siempre con { "error": "mensaje", "status": n } (ver
 * ManejadorErrores). Antes de que existiera ese manejador, un error de negocio
 * llegaba como 500 con el cuerpo por defecto de Spring y los avisos al usuario
 * mostraban un volcado de JSON sin sentido.
 */
(function () {
    /**
     * Extrae un mensaje legible de una respuesta de error.
     *
     * @param {Response} response - Respuesta fallida de la API.
     * @param {string} porDefecto - Texto a usar si la API no devuelve mensaje.
     * @returns {Promise<string>} El mensaje de error legible.
     */
    async function mensajeError(response, porDefecto) {
        try {
            const cuerpo = await response.json();
            if (cuerpo && typeof cuerpo.error === 'string' && cuerpo.error.trim() !== '') {
                return cuerpo.error;
            }
        } catch (e) {
            // La respuesta no traía JSON (o ya se había consumido): usamos el texto plano.
        }
        try {
            const texto = await response.text();
            if (texto && texto.trim() !== '') {
                return texto.trim();
            }
        } catch (e) {
            // Sin cuerpo legible: nos quedamos con el mensaje por defecto.
        }
        return porDefecto;
    }

    window.GameBoXDErrores = {
        mensajeError: mensajeError
    };
})();
