import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {
    cargarMiLista,
    cacheJuegos,
    cacheDesarrolladoras,
} from '../miListaScript.js';

const BASE_URL = 'https://api.test.local/api';
const ID_USUARIO = 7;

function jsonRespuesta(cuerpo, status = 200) {
    return new Response(JSON.stringify(cuerpo ?? null), {
        status,
        headers: { 'Content-Type': 'application/json' },
    });
}

let listaEntradas = [
    { miId: 1, idVideojuego: 10, idUsuario: ID_USUARIO, estado: 'noEmpezado', nota: 8.5, horasJugadas: 12, resenya: 'Brillante' },
    { miId: 2, idVideojuego: 10, idUsuario: ID_USUARIO, estado: 'empezado', nota: 6.0, horasJugadas: 4, resenya: null },
    { miId: 3, idVideojuego: 20, idUsuario: ID_USUARIO, estado: 'terminado', nota: 9.0, horasJugadas: 30, resenya: 'Imprescindible' },
];

const juegos = {
    10: { id: 10, nombre: 'Hollow Knight', idDesarrolladora: 100, urlImagen: 'https://ejemplo.imagen/hollow-knight.jpg' },
    20: { id: 20, nombre: 'Celeste', idDesarrolladora: null, urlImagen: null },
};

const desarrolladoras = {
    100: { id: 100, nombre: 'Team Cherry' },
};

const llamadas = [];
let fetchMock;

beforeEach(() => {
    llamadas.length = 0;
    listaEntradas = [
        { miId: 1, idVideojuego: 10, idUsuario: ID_USUARIO, estado: 'noEmpezado', nota: 8.5, horasJugadas: 12, resenya: 'Brillante' },
        { miId: 2, idVideojuego: 10, idUsuario: ID_USUARIO, estado: 'empezado', nota: 6.0, horasJugadas: 4, resenya: null },
        { miId: 3, idVideojuego: 20, idUsuario: ID_USUARIO, estado: 'terminado', nota: 9.0, horasJugadas: 30, resenya: 'Imprescindible' },
    ];
    cacheJuegos.clear();
    cacheDesarrolladoras.clear();
    sessionStorage.clear();

    fetchMock = vi.fn(async (url) => {
        llamadas.push(String(url));
        const u = String(url);

        if (u === `${BASE_URL}/lista/${ID_USUARIO}`) {
            return jsonRespuesta(listaEntradas);
        }

        const mJuego = u.match(/\/videojuegos\/(\d+)$/);
        if (mJuego) {
            const juego = juegos[Number(mJuego[1])];
            if (!juego) {
                return jsonRespuesta({ error: 'Juego no encontrado' }, 404);
            }
            return jsonRespuesta(juego);
        }

        const mDesarrolladora = u.match(/\/desarrolladoras\/id\/(\d+)$/);
        if (mDesarrolladora) {
            const desarrolladora = desarrolladoras[Number(mDesarrolladora[1])];
            if (!desarrolladora) {
                return jsonRespuesta({ error: 'Desarrolladora no encontrada' }, 404);
            }
            return jsonRespuesta(desarrolladora);
        }

        return jsonRespuesta({ error: 'Ruta no encontrada' }, 404);
    });

    globalThis.fetch = fetchMock;
    window.GameBoXDApi = { BASE_URL };
});

afterEach(() => {
    globalThis.fetch = undefined;
    vi.restoreAllMocks();
});

describe('cargarMiLista — fin del problema N+1', () => {
    it('hace como máximo 4 peticiones en total (1 lista + juegos únicos + desarrolladoras únicas), nunca 1 + N + N', async () => {
        await cargarMiLista(ID_USUARIO);

        const listaCalls = llamadas.filter(u => u.endsWith(`/lista/${ID_USUARIO}`));
        const juegoCalls = llamadas.filter(u => u.includes('/videojuegos/'));
        const devCalls = llamadas.filter(u => u.includes('/desarrolladoras/id/'));

        expect(listaCalls).toHaveLength(1);
        expect(juegoCalls).toHaveLength(2);
        expect(new Set(juegoCalls.map(u => Number(u.split('/').pop()))).size).toBe(juegoCalls.length);
        expect(devCalls).toHaveLength(1);
        expect(llamadas).toHaveLength(4);
    });

    it('dedupe: /api/videojuegos/{id} se fetch una sola vez aunque dos entradas compartan el id', async () => {
        await cargarMiLista(ID_USUARIO);

        const juegoCalls = llamadas.filter(u => u.includes('/videojuegos/'));
        const ids = juegoCalls.map(u => Number(u.split('/').pop()));
        expect(ids).toEqual(expect.arrayContaining([10, 20]));
        expect(ids.filter(id => id === 10)).toHaveLength(1);
        expect(ids.filter(id => id === 20)).toHaveLength(1);
    });

    it('renderiza los nombres de desarrolladora desde el caché (sin fetch repetido)', async () => {
        await cargarMiLista(ID_USUARIO);

        const grid = document.getElementById('game-grid');
        const cards = grid.querySelectorAll('.game-list-card');
        expect(cards).toHaveLength(3);

        expect(grid.querySelector('#dev-name-1').textContent).toBe('Team Cherry');
        expect(grid.querySelector('#dev-name-2').textContent).toBe('Team Cherry');
        expect(grid.querySelector('#dev-name-3').textContent).toBe('Estudio Desconocido');
    });

    it('si un juego falla, se renderiza su tarjeta de fallback y no se rompe el resto de la lista', async () => {
        listaEntradas = [
            ...listaEntradas,
            { miId: 4, idVideojuego: 99, idUsuario: ID_USUARIO, estado: 'noEmpezado', nota: 0, horasJugadas: 0, resenya: null },
        ];

        await cargarMiLista(ID_USUARIO);

        const grid = document.getElementById('game-grid');
        const cards = grid.querySelectorAll('.game-list-card');
        expect(cards).toHaveLength(4);

        expect(grid.querySelector('#card-entry-1')).not.toBeNull();
        expect(grid.querySelector('#card-entry-3')).not.toBeNull();
        const fallback = grid.querySelector('#card-entry-4');
        expect(fallback).not.toBeNull();
        expect(fallback.textContent).toContain('Videojuego No Disponible');

        expect(llamadas.filter(u => u.includes('/videojuegos/99'))).toHaveLength(1);
    });
});
