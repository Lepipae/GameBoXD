/**
 * Gestión de la URL de retorno después del login.
 *
 * Cuando un usuario no autenticado intenta acceder a una página que requiere
 * sesión (ver mi lista, añadir un juego...), se guarda la URL actual antes de
 * enviarlo al formulario de acceso. Tras un login exitoso se vuelve a esa
 * página en lugar de ir siempre a index.html.
 *
 * La URL se guarda en sessionStorage: sobrevive a la redirección (a diferencia
 * de una variable en memoria) y se borra al usarse, así no queda basura
 * almacenada ni se filtra a otras navegaciones.
 *
 * Todas las rutas se validan para evitar open redirects: solo se aceptan rutas
 * relativas del mismo origen (sin protocolo, sin host, sin traversal).
 */

const STORAGE_KEY = 'gameboxd_return_url';
const DEFAULT_URL = 'index.html';

/**
 * Comprueba si una URL es una ruta relativa segura del mismo origen.
 *
 * Rechaza:
 *  - URLs con protocolo (http:, https:, javascript:, data:, ...)
 *  - Rutas relativas al protocolo (//evil.com/)
 *  - Rutas con traversal (..)
 *
 * @param {string} url - URL a validar.
 * @returns {boolean} true si es una ruta relativa segura.
 */
function esRutaRelativaSegura(url) {
    if (typeof url !== 'string') return false;
    const valor = url.trim();
    if (valor === '') return false;
    if (/^[a-zA-Z][a-zA-Z0-9+.-]*:/.test(valor)) return false;
    if (valor.startsWith('//')) return false;
    if (valor.includes('..')) return false;
    return true;
}

/**
 * Extrae el valor del parámetro ?next= de la URL actual.
 *
 * @returns {string|null} Valor del parámetro next o null.
 */
function obtenerParametroNext() {
    try {
        return new URLSearchParams(window.location.search).get('next');
    } catch (e) {
        return null;
    }
}

/**
 * Devuelve la URL a la que redirigir tras un login exitoso.
 *
 * Prioridad:
 *  1. Parámetro ?next= de la propia página de login (validado).
 *  2. document.referrer si pertenece al mismo origen (validado).
 *  3. index.html por defecto.
 *
 * @returns {string} URL de destino segura.
 */
function getLoginRedirectUrl() {
    const next = obtenerParametroNext();
    if (next && esRutaRelativaSegura(next)) return next;

    const referrer = document.referrer;
    if (referrer) {
        try {
            const url = new URL(referrer);
            if (url.origin === window.location.origin) {
                const ruta = (url.pathname + url.search).replace(/^\//, '');
                if (esRutaRelativaSegura(ruta)) return ruta || DEFAULT_URL;
            }
        } catch (e) {
            // Referrer no parseable: se ignora.
        }
    }

    return DEFAULT_URL;
}

/**
 * Guarda la página actual (o una URL dada) antes de redirigir al login.
 *
 * @param {string} [url] - URL a guardar. Por defecto, la página actual.
 * @returns {string|null} URL guardada o null si no era válida.
 */
function saveReturnUrl(url) {
    const ruta = url !== undefined ? url : window.location.pathname + window.location.search;
    if (esRutaRelativaSegura(ruta)) {
        sessionStorage.setItem(STORAGE_KEY, ruta);
        return ruta;
    }
    return null;
}

/**
 * Obtiene la URL de retorno guardada, si existe.
 *
 * @returns {string|null}
 */
function getReturnUrl() {
    return sessionStorage.getItem(STORAGE_KEY);
}

/**
 * Elimina la URL de retorno guardada. Se llama tras usarla para redirigir.
 */
function clearReturnUrl() {
    sessionStorage.removeItem(STORAGE_KEY);
}

// API para consumidores mediante importación de módulo
export {
    esRutaRelativaSegura,
    getLoginRedirectUrl,
    saveReturnUrl,
    getReturnUrl,
    clearReturnUrl,
};

// Compatibilidad con scripts clásicos (header.js, loginScript.js, miListaScript.js)
if (typeof window !== 'undefined') {
    window.GameBoXDRedirect = {
        saveReturnUrl,
        getReturnUrl,
        clearReturnUrl,
        getLoginRedirectUrl,
    };
}
