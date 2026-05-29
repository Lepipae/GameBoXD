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
                            if (!settled) {
                                console.warn(`[API] Fallo o error en servidor ${serverName} durante la carrera:`, err.message || err);
                            }
                            
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
