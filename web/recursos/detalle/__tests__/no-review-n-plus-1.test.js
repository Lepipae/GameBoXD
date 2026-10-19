import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
    renderizarResenyas,
    cargarNombreUsuarioReview,
    cacheUsuarios,
} from '../detalleScript.js';

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0));

function montarDOM() {
    document.body.innerHTML = `
        <div id="loading-state"></div>
        <div id="game-detail-wrapper"></div>
        <div id="reviews-wrapper"></div>
        <div id="reviews-container"></div>
    `;
}

const RESENYAS = [
    { miId: 1, idUsuario: 10, resenya: 'Un juegazo', nota: 4.5 },
    { miId: 2, idUsuario: 20, resenya: 'Muy entretenido', nota: 5.0 },
    { miId: 3, idUsuario: 10, resenya: 'Lo recomiendo', nota: 4.0 },
];

describe('renderizarResenyas — sin peticiones N+1 a /api/usuarios', () => {
    let fetchMock;

    beforeEach(() => {
        montarDOM();
        // El caché vive a nivel de módulo: hay que limpiarlo entre tests para aislarlos.
        cacheUsuarios.clear();

        fetchMock = vi.fn().mockImplementation(url => {
            const u = String(url);
            if (u.endsWith('/usuarios/10')) {
                return Promise.resolve({ ok: true, json: async () => ({ nombre: 'Ana Martínez' }) });
            }
            if (u.endsWith('/usuarios/20')) {
                return Promise.resolve({ ok: true, json: async () => ({ nombre: 'Luis Ruiz' }) });
            }
            // Cualquier otro usuario: fallo de la API
            return Promise.resolve({ ok: false, status: 404, json: async () => ({ error: 'no existe' }) });
        });

        globalThis.fetch = fetchMock;
    });

    afterEach(() => {
        globalThis.fetch = undefined;
        vi.restoreAllMocks();
    });

    it('fetch deduplicado por usuario: 3 reseñas de 2 usuarios únicos => 2 llamadas, no 3', async () => {
        renderizarResenyas(RESENYAS);
        await flushPromises();

        // Una llamada por ID de usuario único (10 y 20), aunque el usuario 10 repita reseña.
        expect(fetchMock).toHaveBeenCalledTimes(2);
        expect(fetchMock).toHaveBeenCalledWith('https://api.test.local/api/usuarios/10');
        expect(fetchMock).toHaveBeenCalledWith('https://api.test.local/api/usuarios/20');

        // Los nombres se renderizan en cada tarjeta, incluida la reseña repetida del usuario 10.
        expect(document.getElementById('review-username-1').textContent).toBe('Ana Martínez');
        expect(document.getElementById('review-username-2').textContent).toBe('Luis Ruiz');
        expect(document.getElementById('review-username-3').textContent).toBe('Ana Martínez');
    });

    it('caché persistente: al volver a renderizar no se vuelve a fetchar (cache hit)', async () => {
        renderizarResenyas(RESENYAS);
        await flushPromises();
        expect(fetchMock).toHaveBeenCalledTimes(2);

        renderizarResenyas(RESENYAS);
        await flushPromises();
        expect(fetchMock).toHaveBeenCalledTimes(2);

        expect(document.getElementById('review-username-1').textContent).toBe('Ana Martínez');
        expect(document.getElementById('review-username-2').textContent).toBe('Luis Ruiz');
    });

    it('cargarNombreUsuarioReview lee del caché y no fetcha en cache hit', async () => {
        cacheUsuarios.set(99, 'Usuario Caché');

        const label = document.createElement('div');
        label.id = 'review-username-99';
        document.body.appendChild(label);

        cargarNombreUsuarioReview(99, 'review-username-99');

        expect(fetchMock).not.toHaveBeenCalled();
        expect(label.textContent).toBe('Usuario Caché');
    });

    it('fallback "Usuario Anónimo" cuando falla la petición del usuario', async () => {
        renderizarResenyas([{ miId: 1, idUsuario: 55, resenya: 'Texto de reseña', nota: 3.0 }]);
        await flushPromises();

        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(document.getElementById('review-username-1').textContent).toBe('Usuario Anónimo');
        expect(cacheUsuarios.get(55)).toBe('Usuario Anónimo');
    });
});
