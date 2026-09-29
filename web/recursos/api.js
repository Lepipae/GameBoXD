/**
 * Gestor de los 2 servidores de API (simulando un CDN?).
 * Si el servidor API principal (gameboxd.duckdns.org) no responde o devuelve un error,
 * se realiza un reintento automático al servidor secundario (gameboxd-2nio.onrender.com) que es gratis y no cuesta pasta.
 */
(function () {
    const primaryUrl = 'https://gameboxd.duckdns.org/api'; // api principal
    const fallbackUrl = 'https://gameboxd-2nio.onrender.com/api'; // api secundaria
    const originalFetch = window.fetch;

    window.fetch = async function (input, init) {
        let url = "";
        if (typeof input === 'string') {
            url = input;
        } else if (input && typeof input === 'object' && 'url' in input) {
            url = input.url;
        }

        // Si la URL pertenece al API principal
        if (url.startsWith(primaryUrl)) {
            // Determinar el método HTTP de la petición (por defecto es GET)
            let method = 'GET';
            if (init && init.method) {
                method = init.method.toUpperCase();
            } else if (input && typeof input === 'object' && 'method' in input && input.method) {
                method = input.method.toUpperCase();
            }

            const isSafeMethod = ['GET', 'HEAD', 'OPTIONS'].includes(method);

            if (isSafeMethod) {
                console.log(`[API] Iniciando peticiones paralelas (carrera) para método seguro ${method}: ${url}`);

                // Preparar los inputs para ambas peticiones paralelas
                let primaryInput;
                let fallbackInput;

                if (typeof input === 'string') {
                    primaryInput = input;
                    fallbackInput = input.replace(primaryUrl, fallbackUrl);
                } else if (input instanceof Request) {
                    primaryInput = input.clone();
                    const newUrl = input.url.replace(primaryUrl, fallbackUrl);
                    fallbackInput = new Request(newUrl, input);
                } else {
                    primaryInput = Object.assign({}, input);
                    fallbackInput = Object.assign({}, input, {
                        url: url.replace(primaryUrl, fallbackUrl)
                    });
                }

                return new Promise((resolve, reject) => {
                    let settled = false;
                    let primaryDone = false;
                    let fallbackDone = false;
                    let primaryError = null;
                    let fallbackError = null;

                    const tryFetch = async (inputVal, isPrimary) => {
                        const serverName = isPrimary ? 'Principal' : 'Secundario';
                        try {
                            const res = await originalFetch(inputVal, init);
                            if (res.ok || res.status < 500) {
                                if (!settled) {
                                    settled = true;
                                    console.log(`[API] El servidor ${serverName} ha respondido primero con éxito (Status: ${res.status}).`);
                                    resolve(res);
                                } else {
                                    console.log(`[API] El servidor ${serverName} ha respondido después (Status: ${res.status}).`);
                                }
                                return;
                            }
                            throw new Error(`Servidor devolvió código de error ${res.status}`);
                        } catch (err) {
                            // Se registra como info en lugar de advertencia para no ensuciar la consola con errores falsos si el otro servidor funciona
                            console.log(`[API] Servidor ${serverName} no disponible en la carrera (Fallo esperado/silencioso):`, err.message || err);

                            if (isPrimary) {
                                primaryDone = true;
                                primaryError = err;
                            } else {
                                fallbackDone = true;
                                fallbackError = err;
                            }

                            if (primaryDone && fallbackDone) {
                                if (!settled) {
                                    settled = true;
                                    console.error('[API] Error crítico: Ambos servidores han fallado.');
                                    reject(primaryError || fallbackError || new Error("Ambos servidores fallaron"));
                                }
                            }
                        }
                    };

                    tryFetch(primaryInput, true);
                    tryFetch(fallbackInput, false);
                });
            } else {
                // Para métodos no seguros (POST, PUT, DELETE, PATCH, etc.),
                // usamos fallback secuencial para evitar ejecutar la acción con efectos secundarios en ambos servidores.
                console.log(`[API] Petición secuencial para método con efectos secundarios ${method}: ${url}`);
                try {
                    // Intentar con el API principal
                    const response = await originalFetch(input, init);
                    if (response.ok || response.status < 500) {
                        return response;
                    }
                    console.warn(`[API] Servidor principal devolvió código de error ${response.status}. Iniciando fallback a secundario...`);
                } catch (error) {
                    console.warn('[API] Error de conexión con el servidor principal. Iniciando fallback a secundario...', error);
                }

                // Si falló, preparamos la petición al fallback
                let fallbackInput;
                if (typeof input === 'string') {
                    fallbackInput = input.replace(primaryUrl, fallbackUrl);
                } else if (input instanceof Request) {
                    const newUrl = input.url.replace(primaryUrl, fallbackUrl);
                    fallbackInput = new Request(newUrl, input);
                } else {
                    fallbackInput = Object.assign({}, input, {
                        url: url.replace(primaryUrl, fallbackUrl)
                    });
                }

                try {
                    console.log(`[API] Reintentando petición en servidor fallback: ${typeof fallbackInput === 'string' ? fallbackInput : (fallbackInput.url || fallbackInput)}`);
                    return await originalFetch(fallbackInput, init);
                } catch (fallbackError) {
                    console.error('[API] Error crítico: El servidor fallback también ha fallado.', fallbackError);
                    throw fallbackError;
                }
            }
        }

        // Peticiones que no son del API de GameBoXD (ej. fuentes, cdn, etc.)
        return originalFetch(input, init);
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
