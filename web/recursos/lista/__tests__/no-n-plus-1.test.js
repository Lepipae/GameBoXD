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
    10: { miId: 10, nombre: 'Hollow Knight', idDesarrolladora: 100, urlImagen: 'https://ejemplo.imagen/hollow-knight.jpg' },
    20: { miId: 20, nombre: 'Celeste', idDesarrolladora: 0, urlImagen: null },
};

const desarrolladoras = {
    100: { miId: 100, nombre: 'Team Cherry' },
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

        // Una sola llamada devuelve entradas, juegos y desarrolladoras.
        if (u === `${BASE_URL}/lista/${ID_USUARIO}/ficha`) {
            const idsJuegos = [...new Set(listaEntradas.map(e => e.idVideojuego))];
            const juegosDeLista = idsJuegos.map(id => juegos[id]).filter(Boolean);
            const devs = juegosDeLista
                .map(j => desarrolladoras[j.idDesarrolladora])
                .filter(Boolean);
            return jsonRespuesta({
                entradas: listaEntradas,
                juegos: juegosDeLista,
                desarrolladoras: devs,
            });
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
    it('hace exactamente 1 petición (entradas + juegos + desarrolladoras juntos)', async () => {
        await cargarMiLista(ID_USUARIO);

        expect(llamadas).toHaveLength(1);
        expect(llamadas[0]).toBe(`${BASE_URL}/lista/${ID_USUARIO}/ficha`);
    });

    it('dedupe: un juego compartido por dos entradas aparece una sola vez en el caché', async () => {
        await cargarMiLista(ID_USUARIO);

        // El juego 10 lo comparten dos entradas: el caché guarda una entrada por juego.
        expect(cacheJuegos.size).toBe(2);
        expect(cacheJuegos.get(10).nombre).toBe('Hollow Knight');
        expect(cacheJuegos.get(20).nombre).toBe('Celeste');
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
    });
});
