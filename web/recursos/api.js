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
     * Host del CDN cuyas portadas sirve redimensionadas el backend.
     * Todas las portadas del catálogo viven aquí; el endpoint /api/imagenes
     * solo acepta este host (lista blanca contra SSRF).
     */
    const HOST_CDN_PORTADAS = 'cdn2.steamgriddb.com';

    /**
     * Construye la URL optimizada de una portada, si procede.
     *
     * <p>Los originales son PNG de ~730 KB. El backend los sirve a `ancho` px y
     * en JPEG (~30 KB), así que una tarjeta deja de costar medio megabyte.</p>
     *
     * @param {string} url - URL original de la portada.
     * @param {number} ancho - Ancho deseado en píxeles.
     * @param {number} [alto] - Alto deseado; si se indica, el backend recorta al
     *        centro para devolver exactamente ancho×alto (`object-fit: cover`).
     * @returns {string} URL optimizada, o la original si el host no aplica.
     */
    function optimizarUrlImagen(url, ancho, alto) {
        const original = String(url ?? '').trim();
        if (original === '') return original;

        let destino;
        try {
            destino = new URL(original);
        } catch (e) {
            return original;
        }

        if (destino.hostname !== HOST_CDN_PORTADAS) {
            return original;
        }

        const base = (window.GameBoXDApi && window.GameBoXDApi.BASE_URL) || '';
        const recorte = alto > 0 ? `&h=${alto}` : '';
        return `${base}/imagenes?url=${encodeURIComponent(original)}&w=${ancho}${recorte}`;
    }

    /**
     * Asigna la imagen a un elemento <img> con respaldo local.
     *
     * Cadena de respaldo: versión redimensionada en el backend → original
     * remota → portada local. Cada eslabón solo se usa si el anterior falla,
     * así que una caída del redimensionador nunca deja la tarjeta vacía.
     *
     * @param {HTMLImageElement} img - Elemento <img> destino.
     * @param {string} url - URL remota candidata; si no es válida se usa el placeholder.
     * @param {Function} [alFallar] - Callback opcional que sustituye al placeholder
     *        (por ejemplo, para ocultar el logo de una desarrolladora sin logo real).
     * @param {number} [ancho] - Ancho al que conviene pedir la portada (por defecto 300).
     * @param {number} [alto] - Alto de recorte; si se omite se conserva la proporción.
     */
    function aplicarImagen(img, url, alFallar, ancho, alto) {
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

        const original = url.trim();
        const optimizada = optimizarUrlImagen(original, ancho || 300, alto);

        // Si la optimizada falla, se intenta el original antes de rendirse.
        if (optimizada !== original) {
            img.addEventListener('error', function reintentarOriginal() {
                img.removeEventListener('error', reintentarOriginal);
                img.addEventListener('error', function manejarErrorImagen() {
                    img.removeEventListener('error', manejarErrorImagen);
                    recurrir();
                });
                img.src = original;
            });
            img.src = optimizada;
            return;
        }

        img.addEventListener('error', function manejarErrorImagen() {
            img.removeEventListener('error', manejarErrorImagen);
            recurrir();
        });

        img.src = original;
    }

    /**
     * Asigna la portada con varios tamaños candidatos (`srcset`).
     *
     * <p>El navegador elige según la densidad de píxeles de la pantalla, así
     * que un monitor normal descarga la variante pequeña y uno retina la
     * grande. Los candidatos se piden al redimensionador del backend, igual que
     * en {@link aplicarImagen}; si la variante elegida falla se reintenta el
     * original y, si también falla, el placeholder.</p>
     *
     * @param {HTMLImageElement} img - Elemento <img> destino.
     * @param {string} url - URL original de la portada.
     * @param {Function} [alFallar] - Callback opcional que sustituye al placeholder.
     * @param {number[]} anchos - Anchos candidatos, de menor a mayor.
     * @param {string} sizes - Ancho de presentación (atributo `sizes` del <img>).
     */
    function aplicarImagenResponsiva(img, url, alFallar, anchos, sizes) {
        if (!img) return;

        const recurrir = function () {
            if (typeof alFallar === 'function') {
                alFallar(img);
            } else {
                img.src = IMAGEN_PLACEHOLDER;
            }
        };

        if (!tieneUrlImagen(url)) {
            recurrir();
            return;
        }

        const original = url.trim();
        const optimizada = optimizarUrlImagen(original, 300);
        const candidatos = (anchos || [])
            .filter(function (ancho) { return Number.isFinite(ancho) && ancho > 0; })
            .map(function (ancho) {
                return optimizarUrlImagen(original, ancho) + ' ' + ancho + 'w';
            });

        // Host ajeno o URL inválida: no hay variantes que ofrecer (todas las
        // candidatas apuntarían al mismo archivo original).
        if (candidatos.length === 0 || optimizada === original) {
            aplicarImagen(img, original, alFallar);
            return;
        }

        img.addEventListener('error', function reintentarOriginal() {
            img.removeEventListener('error', reintentarOriginal);
            img.addEventListener('error', function manejarErrorImagen() {
                img.removeEventListener('error', manejarErrorImagen);
                recurrir();
            });
            // Sin srcset: si el elegido falla, el original es el único plan B.
            img.removeAttribute('srcset');
            img.removeAttribute('sizes');
            img.src = original;
        });

        if (sizes) img.setAttribute('sizes', sizes);
        img.setAttribute('srcset', candidatos.join(', '));
        // `src` queda como último recurso para navegadores sin srcset.
        img.src = optimizarUrlImagen(original, anchos[anchos.length - 1]);
    }

    window.GameBoXDImagenes = {
        IMAGEN_PLACEHOLDER: IMAGEN_PLACEHOLDER,
        tieneUrlImagen: tieneUrlImagen,
        optimizarUrlImagen: optimizarUrlImagen,
        aplicarImagen: aplicarImagen,
        aplicarImagenResponsiva: aplicarImagenResponsiva
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
