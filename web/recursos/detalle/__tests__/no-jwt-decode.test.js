import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { verificarSesionYConfigurarBoton } from '../detalleScript.js';

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0));

/**
 * verificarSesionYConfigurarBoton no debe descodificar el JWT del cliente
 * (token.split('.')[1] + atob + JSON.parse) para obtener el username. El
 * nombre/miId se obtienen de la sesión en caché (currentUser, poblada en el
 * login) o de la API, nunca del payload del token.
 *
 * detalleScript.js es un módulo ES (tiene exportaciones), así que se importa
 * directamente como hacen las demás pruebas de detalle.
 */
const BASE_URL = 'https://api.test.local/api';
const CURRENT_USER_NOMBRE = 'pepe';
const JWT_SUB = 'otro-usuario'; // sub del token: distinto del nombre en caché

function base64Url(obj) {
    return Buffer.from(JSON.stringify(obj)).toString('base64url');
}

// Token cuyo payload decodifica a { sub: 'otro-usuario' }. Si el código
// descodificara el JWT, consultaría /usuarios/nombre/otro-usuario en lugar de
// usar currentUser.nombre ('pepe').
const FAKE_TOKEN = `${base64Url({ alg: 'none', typ: 'JWT' })}.${base64Url({ sub: JWT_SUB })}.firma-falsa`;

describe('verificarSesionYConfigurarBoton - no descodifica el JWT del cliente', () => {
    let fetchMock;
    let atobSpy;

    beforeEach(() => {
        document.body.innerHTML = `
            <div id="loading-state"></div>
            <div id="game-detail-wrapper"></div>
            <button id="add-to-list-btn"></button>
        `;
        localStorage.clear();
        fetchMock = vi.fn().mockResolvedValue({ ok: true, json: () => Promise.resolve([]) });
        globalThis.fetch = fetchMock;
        atobSpy = vi.spyOn(window, 'atob');
    });

    afterEach(() => {
        vi.restoreAllMocks();
        localStorage.clear();
        document.body.innerHTML = '';
    });

    it('nunca llama a atob ni parte el token con split(".")', async () => {
        localStorage.setItem('jwt_token', FAKE_TOKEN);
        localStorage.setItem('currentUser', JSON.stringify({ token: FAKE_TOKEN, miId: 42, nombre: CURRENT_USER_NOMBRE }));

        verificarSesionYConfigurarBoton('juego-1');
        await flushPromises();

        expect(atobSpy).not.toHaveBeenCalled();
    });

    it('usa miId y nombre de currentUser en caché sin parsear el token', async () => {
        localStorage.setItem('jwt_token', FAKE_TOKEN);
        localStorage.setItem('currentUser', JSON.stringify({ token: FAKE_TOKEN, miId: 42, nombre: CURRENT_USER_NOMBRE }));

        verificarSesionYConfigurarBoton('juego-1');
        await flushPromises();

        // Se usó el miId de caché (42) para consultar la lista; nunca se llamó a /usuarios/nombre
        expect(fetchMock).toHaveBeenCalledWith(
            expect.stringContaining('/lista/42'),
            expect.objectContaining({ headers: expect.objectContaining({ Authorization: `Bearer ${FAKE_TOKEN}` }) })
        );
        expect(fetchMock).not.toHaveBeenCalledWith(expect.stringContaining('/usuarios/nombre/'), expect.anything());
        expect(atobSpy).not.toHaveBeenCalled();
    });

    it('llama a la API con Authorization: Bearer <token> cuando miId no está en caché, usando currentUser.nombre', async () => {
        localStorage.setItem('jwt_token', FAKE_TOKEN);
        // currentUser tiene nombre pero no miId: el miId no está en caché
        localStorage.setItem('currentUser', JSON.stringify({ token: FAKE_TOKEN, nombre: CURRENT_USER_NOMBRE }));
        fetchMock.mockResolvedValueOnce({ ok: true, json: () => Promise.resolve({ miId: 7, nombre: CURRENT_USER_NOMBRE }) });

        verificarSesionYConfigurarBoton('juego-1');
        await flushPromises();

        expect(fetchMock).toHaveBeenCalledWith(
            `${BASE_URL}/usuarios/nombre/${CURRENT_USER_NOMBRE}`,
            expect.objectContaining({ headers: expect.objectContaining({ Authorization: `Bearer ${FAKE_TOKEN}` }) })
        );
        // Nunca se descodifica el payload del JWT para obtener el username
        expect(atobSpy).not.toHaveBeenCalled();
    });

    it('no descodifica el JWT durante el flujo de verificación de sesión', async () => {
        localStorage.setItem('jwt_token', FAKE_TOKEN);
        // Sin currentUser en caché: no hay nombre, no se consulta la API ni se toca el token
        verificarSesionYConfigurarBoton('juego-1');
        await flushPromises();

        expect(fetchMock).not.toHaveBeenCalled();
        expect(atobSpy).not.toHaveBeenCalled();
        expect(document.getElementById('add-to-list-btn').style.display).toBe('none');
    });
});
