import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { cargarFicha, cacheUsuarios } from '../detalleScript.js';

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0));
const BASE_URL = 'https://api.test.local/api';

const JUEGO = {
    miId: 42,
    nombre: 'Spore',
    descripcion: 'Simulación de evolución',
    urlImagen: null,
    notaMedia: 8.0,
    tags: ['simulación'],
    idDesarrolladora: 25,
};

const RESENYAS = [
    { miId: 1, idUsuario: 10, resenya: 'Un clásico', nota: 8.0 },
    { miId: 2, idUsuario: 20, resenya: 'Horas y horas', nota: 9.0 },
];

describe('cargarFicha — la ficha del detalle cuesta UNA petición', () => {
    let fetchMock;

    beforeEach(() => {
        cacheUsuarios.clear();
        document.body.innerHTML = `
            <div id="loading-state"></div>
            <div id="game-detail-wrapper"></div>
            <div id="reviews-wrapper"></div>
            <div id="reviews-container"></div>
            <h1 id="titulo"></h1>
            <img id="game-cover">
            <p id="game-desc"></p>
            <div id="game-tags"></div>
            <div id="game-rating"></div>
            <div id="game-dev-box"></div>
            <a id="dev-link"></a>
            <img id="dev-logo">
            <span id="dev-name"></span>
            <span id="dev-country"></span>
        `;
        fetchMock = vi.fn();
        globalThis.fetch = fetchMock;
    });

    afterEach(() => {
        globalThis.fetch = undefined;
        vi.restoreAllMocks();
    });

    it('no pide /usuarios/lote cuando la ficha ya trae los autores', async () => {
        fetchMock.mockResolvedValue({
            ok: true,
            json: async () => ({
                juego: JUEGO,
                desarrolladora: { miId: 25, nombre: 'Maxis', urlImagen: null, pais: 'EE. UU.' },
                resenyas: RESENYAS,
                autores: [
                    { miId: 10, nombre: 'Ana Martínez', urlImagen: null },
                    { miId: 20, nombre: 'Luis Ruiz', urlImagen: null },
                ],
            }),
        });

        await cargarFicha(42);
        await flushPromises();

        // Una sola petición: la de la ficha. Nada de /usuarios/lote.
        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(String(fetchMock.mock.calls[0][0])).toBe(`${BASE_URL}/ficha/42`);

        expect(document.getElementById('titulo').textContent).toBe('Spore');
        expect(document.getElementById('dev-name').textContent).toBe('Maxis');
        expect(document.getElementById('review-username-1').textContent).toBe('Ana Martínez');
        expect(document.getElementById('review-username-2').textContent).toBe('Luis Ruiz');
        expect(cacheUsuarios.get(10)).toBe('Ana Martínez');
    });

    it('si la ficha no trae autores, cae al lote de /usuarios (compatibilidad)', async () => {
        fetchMock.mockImplementation(async (url) => {
            const u = String(url);
            if (u === `${BASE_URL}/ficha/42`) {
                return {
                    ok: true,
                    json: async () => ({
                        juego: JUEGO,
                        desarrolladora: null,
                        resenyas: RESENYAS,
                    }),
                };
            }
            if (u.startsWith(`${BASE_URL}/usuarios/lote?ids=`)) {
                return {
                    ok: true,
                    json: async () => [
                        { miId: 10, nombre: 'Ana Martínez' },
                        { miId: 20, nombre: 'Luis Ruiz' },
                    ],
                };
            }
            return { ok: false, status: 404, json: async () => ({}) };
        });

        await cargarFicha(42);
        await flushPromises();

        expect(fetchMock).toHaveBeenCalledTimes(2);
        expect(String(fetchMock.mock.calls[1][0])).toContain('/usuarios/lote?ids=');
        expect(document.getElementById('review-username-1').textContent).toBe('Ana Martínez');
    });
});
