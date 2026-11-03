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

        // Endpoint de lote: devuelve solo los juegos que existen.
        if (u.startsWith(`${BASE_URL}/videojuegos/lote?ids=`)) {
            const ids = new URL(u).searchParams.get('ids').split(',').map(Number);
            return jsonRespuesta(ids.map((id) => juegos[id]).filter(Boolean));
        }

        if (u.startsWith(`${BASE_URL}/desarrolladoras/lote?ids=`)) {
            const ids = new URL(u).searchParams.get('ids').split(',').map(Number);
            return jsonRespuesta(ids.map((id) => desarrolladoras[id]).filter(Boolean));
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
    it('hace exactamente 3 peticiones (lista + lote de juegos + lote de desarrolladoras)', async () => {
        await cargarMiLista(ID_USUARIO);

        const listaCalls = llamadas.filter(u => u.endsWith(`/lista/${ID_USUARIO}`));
        const juegoCalls = llamadas.filter(u => u.includes('/videojuegos/lote'));
        const devCalls = llamadas.filter(u => u.includes('/desarrolladoras/lote'));

        expect(listaCalls).toHaveLength(1);
        expect(juegoCalls).toHaveLength(1);
        expect(devCalls).toHaveLength(1);
        expect(llamadas).toHaveLength(3);
        // Todos los ids de juegos viajan en la misma URL: 10 y 20.
        expect(juegoCalls[0]).toBe(`${BASE_URL}/videojuegos/lote?ids=10,20`);
        expect(devCalls[0]).toBe(`${BASE_URL}/desarrolladoras/lote?ids=100`);
    });

    it('dedupe: un juego compartido por dos entradas solo aparece una vez en el lote', async () => {
        await cargarMiLista(ID_USUARIO);

        const lote = llamadas.find(u => u.includes('/videojuegos/lote'));
        const ids = new URL(lote).searchParams.get('ids').split(',');
        expect(ids).toEqual(['10', '20']);
        expect(ids.filter(id => id === '10')).toHaveLength(1);
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

        // El id inexistente viaja en el lote y simplemente no vuelve en la respuesta.
        const lote = llamadas.find(u => u.includes('/videojuegos/lote'));
        expect(new URL(lote).searchParams.get('ids').split(',')).toContain('99');
    });
});
