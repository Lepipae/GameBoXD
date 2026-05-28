/**
 * Interceptor global de fetch para GameBoXD.
 * Si el servidor API principal (gameboxd.duckdns.org) no responde o devuelve un error de servidor (5xx),
 * se realiza un reintento automático (fallback) al servidor secundario (gameboxd-2nio.onrender.com).
 */
(function() {
    const primaryUrl = 'https://gameboxd.duckdns.org/api';
    const fallbackUrl = 'https://gameboxd-2nio.onrender.com/api';
    const originalFetch = window.fetch;

    window.fetch = async function(input, init) {
        let url = "";
        if (typeof input === 'string') {
            url = input;
        } else if (input && typeof input === 'object' && 'url' in input) {
            url = input.url;
        }

        // Si la URL pertenece al API principal
        if (url.startsWith(primaryUrl)) {
            try {
                // Intentar con el API principal
                const response = await originalFetch(input, init);
                
                // Si la respuesta es exitosa o es un error de cliente (4xx), la devolvemos.
                // Errores de cliente (como 404, 400, 401) no deben provocar fallback porque son válidos de lógica.
                // Errores de servidor (5xx, ej: 500, 502, 503, 504) se consideran caídas del servidor y activan el fallback.
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
                // Objeto genérico
                fallbackInput = Object.assign({}, input, {
                    url: url.replace(primaryUrl, fallbackUrl)
                });
            }

            try {
                console.log(`[API] Reintentando petición en servidor fallback: ${typeof fallbackInput === 'string' ? fallbackInput : (fallbackInput.url || fallbackInput)}`);
                return await originalFetch(fallbackInput, init);
            } catch (fallbackError) {
                console.error('[API] Error crítico: El servidor fallback también ha fallado.', fallbackError);
                throw fallbackError; // Propagar el error original de conexión
            }
        }

        // Peticiones que no son del API de GameBoXD (ej. fuentes, cdn, etc.)
        return originalFetch(input, init);
    };
})();
