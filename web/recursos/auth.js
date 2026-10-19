/**
 * Gestión centralizada de la sesión de autenticación de GameBoXD.
 *
 * Centraliza en un único módulo la lógica que antes estaba duplicada entre
 * loginScript.js, miListaScript.js, header.js y detalleScript.js:
 *   - inicio de sesión (fetch + guardado de JWT y perfil en localStorage),
 *   - cierre de sesión,
 *   - lectura del token y del usuario en curso,
 *   - comprobación de si hay sesión activa.
 *
 * Las claves de localStorage son consistentes en toda la aplicación:
 *   - `jwt_token`: el JWT recibido del servidor.
 *   - `currentUser`: objeto de sesión serializado (token, miId, nombre, urlImagen).
 *
 * Se basa en `window.GameBoXDApi.BASE_URL` (definido por recursos/api.js) para
 * construir las URLs de la API.
 */

const TOKEN_KEY = 'jwt_token';
const USER_KEY = 'currentUser';

export function setSession(token, user) {
    if (token) {
        localStorage.setItem(TOKEN_KEY, token);
    } else {
        localStorage.removeItem(TOKEN_KEY);
    }

    if (user) {
        localStorage.setItem(USER_KEY, JSON.stringify(user));
    } else {
        localStorage.removeItem(USER_KEY);
    }
}

export function clearSession() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
}

export function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

export function getCurrentUser() {
    const raw = localStorage.getItem(USER_KEY);
    if (!raw) {
        return null;
    }
    try {
        return JSON.parse(raw);
    } catch (e) {
        return null;
    }
}

export function isAuthenticated() {
    return !!localStorage.getItem(TOKEN_KEY);
}

export async function login(nombre, contrasenia) {
    const response = await fetch(`${window.GameBoXDApi.BASE_URL}/auth/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ nombre, contrasenia }),
    });

    if (!response.ok) {
        return null;
    }

    const data = await response.json();
    const token = data.jwt;

    let user = null;
    try {
        const profileResponse = await fetch(`${window.GameBoXDApi.BASE_URL}/usuarios/nombre/${nombre}`);
        if (profileResponse.ok) {
            const userProfile = await profileResponse.json();
            user = {
                token,
                miId: userProfile.miId,
                nombre: userProfile.nombre,
                urlImagen: userProfile.urlImagen,
            };
        }
    } catch (profileError) {
        console.error('Error fetching user profile during login:', profileError);
    }

    setSession(token, user);
    return { token, user };
}

export function logout() {
    clearSession();
}
