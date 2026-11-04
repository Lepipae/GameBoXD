import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {
    cargarMiLista,
    cacheJuegos,
    cacheDesarrolladoras,
    normalizarTexto,
    filtrarYOrdenarRegistros,
} from '../miListaScript.js';

const BASE_URL = 'https://api.test.local/api';
const ID_USUARIO = 7;

function jsonRespuesta(cuerpo, status = 200) {
    return new Response(JSON.stringify(cuerpo ?? null), {
        status,
        headers: { 'Content-Type': 'application/json' },
    });
}

const registros = [
    { miId: 1, nombre: 'Hollow Knight', desarrolladora: 'Team Cherry', horasJugadas: 12 },
    { miId: 2, nombre: 'Celeste', desarrolladora: 'Maddy Makes Games', horasJugadas: 30 },
    { miId: 3, nombre: 'Ánima Gate', desarrolladora: 'Studio X', horasJugadas: 4 },
    { miId: 4, nombre: 'Zelda', desarrolladora: 'Nintendo', horasJugadas: 30 },
];

const juegos = {
    10: { miId: 10, nombre: 'Hollow Knight', idDesarrolladora: 100, urlImagen: null },
    20: { miId: 20, nombre: 'Celeste', idDesarrolladora: 0, urlImagen: null },
};

const desarrolladoras = {
    100: { miId: 100, nombre: 'Team Cherry' },
};

let listaEntradas = [];

beforeEach(() => {
    listaEntradas = [
        { miId: 1, idVideojuego: 10, idUsuario: ID_USUARIO, estado: 'empezado', nota: 8.5, horasJugadas: 12, resenya: null },
        { miId: 2, idVideojuego: 20, idUsuario: ID_USUARIO, estado: 'terminado', nota: 9.0, horasJugadas: 30, resenya: null },
    ];
    cacheJuegos.clear();
    cacheDesarrolladoras.clear();
    sessionStorage.clear();

    document.getElementById('collection-search-input').value = '';
    document.getElementById('collection-sort-select').value = 'recientes';

    globalThis.fetch = vi.fn(async (url) => {
        const u = String(url);

        // Una sola petición devuelve entradas, juegos y desarrolladoras.
        if (u === `${BASE_URL}/lista/${ID_USUARIO}/ficha`) {
            const idsJuegos = [...new Set(listaEntradas.map((e) => e.idVideojuego))];
            const juegosDeLista = idsJuegos.map((id) => juegos[id]).filter(Boolean);
            const devs = juegosDeLista
                .map((j) => desarrolladoras[j.idDesarrolladora])
                .filter(Boolean);
            return jsonRespuesta({
                entradas: listaEntradas,
                juegos: juegosDeLista,
                desarrolladoras: devs,
            });
        }

        return jsonRespuesta({ error: 'Ruta no encontrada' }, 404);
    });

    window.GameBoXDApi = { BASE_URL };
});

afterEach(() => {
    globalThis.fetch = undefined;
    vi.restoreAllMocks();
});

describe('normalizarTexto', () => {
    it('ignora mayúsculas, acentos y espacios sobrantes', () => {
        expect(normalizarTexto('  Pokémon Ámbar  ')).toBe('pokemon ambar');
    });

    it('trata null y undefined como cadena vacía', () => {
        expect(normalizarTexto(null)).toBe('');
        expect(normalizarTexto(undefined)).toBe('');
    });
});

describe('filtrarYOrdenarRegistros', () => {
    it('sin texto devuelve todos los registros y respeta el orden original', () => {
        const resultado = filtrarYOrdenarRegistros(registros);
        expect(resultado.map((r) => r.miId)).toEqual([1, 2, 3, 4]);
    });

    it('filtra por título sin distinguir mayúsculas', () => {
        const resultado = filtrarYOrdenarRegistros(registros, { texto: 'celeste' });
        expect(resultado.map((r) => r.miId)).toEqual([2]);
    });

    it('filtra por título ignorando acentos', () => {
        const resultado = filtrarYOrdenarRegistros(registros, { texto: 'anima' });
        expect(resultado.map((r) => r.miId)).toEqual([3]);
    });

    it('filtra también por desarrolladora', () => {
        const resultado = filtrarYOrdenarRegistros(registros, { texto: 'nintendo' });
        expect(resultado.map((r) => r.miId)).toEqual([4]);
    });

    it('devuelve una lista vacía cuando no hay coincidencias', () => {
        expect(filtrarYOrdenarRegistros(registros, { texto: 'doom' })).toEqual([]);
    });

    it('ordena alfabéticamente A-Z ignorando acentos', () => {
        const resultado = filtrarYOrdenarRegistros(registros, { orden: 'nombre-asc' });
        // Ánima < Celeste < Hollow Knight < Zelda
        expect(resultado.map((r) => r.nombre)).toEqual(['Ánima Gate', 'Celeste', 'Hollow Knight', 'Zelda']);
    });

    it('ordena alfabéticamente Z-A', () => {
        const resultado = filtrarYOrdenarRegistros(registros, { orden: 'nombre-desc' });
        expect(resultado.map((r) => r.nombre)).toEqual(['Zelda', 'Hollow Knight', 'Celeste', 'Ánima Gate']);
    });

    it('ordena por horas jugadas de más a menos', () => {
        const resultado = filtrarYOrdenarRegistros(registros, { orden: 'horas-desc' });
        expect(resultado.map((r) => r.miId)).toEqual([2, 4, 1, 3]);
    });

    it('ordena por horas jugadas de menos a más', () => {
        const resultado = filtrarYOrdenarRegistros(registros, { orden: 'horas-asc' });
        expect(resultado.map((r) => r.miId)).toEqual([3, 1, 2, 4]);
    });

    it('trata horas ausentes o no numéricas como 0', () => {
        const conHuecos = [
            { miId: 1, nombre: 'A', horasJugadas: 5 },
            { miId: 2, nombre: 'B', horasJugadas: undefined },
            { miId: 3, nombre: 'C', horasJugadas: 'muchas' },
        ];
        const resultado = filtrarYOrdenarRegistros(conHuecos, { orden: 'horas-desc' });
        expect(resultado.map((r) => r.miId)).toEqual([1, 2, 3]);
    });

    it('no muta el array recibido', () => {
        const copia = registros.map((r) => r.miId);
        filtrarYOrdenarRegistros(registros, { orden: 'nombre-desc' });
        expect(registros.map((r) => r.miId)).toEqual(copia);
    });

    it('combina filtro y orden', () => {
        const resultado = filtrarYOrdenarRegistros(registros, { texto: 'e', orden: 'horas-desc' });
        // 'e' aparece en Celeste, Zelda, Hollow Knight, Team Cherry, Maddy Makes Games, Studio X(?)
        expect(resultado.map((r) => r.miId)).toEqual([2, 4, 1, 3]);
    });
});

describe('búsqueda y orden en la colección renderizada', () => {
    function idsEnGrid() {
        return [...document.querySelectorAll('#game-grid .game-list-card')]
            .filter((card) => !card.classList.contains('hidden'))
            .map((card) => card.id);
    }

    it('muestra los controles y el recuento al cargar la colección', async () => {
        await cargarMiLista(ID_USUARIO);

        expect(document.getElementById('collection-controls').classList.contains('hidden')).toBe(false);
        expect(document.getElementById('collection-results-info').textContent).toBe('2 juegos en tu colección');
        expect(document.getElementById('no-results-message').classList.contains('hidden')).toBe(true);
    });

    it('filtra las tarjetas al escribir en el buscador', async () => {
        await cargarMiLista(ID_USUARIO);

        const input = document.getElementById('collection-search-input');
        input.value = 'celeste';
        input.dispatchEvent(new Event('input', { bubbles: true }));

        expect(idsEnGrid()).toEqual(['card-entry-2']);
        expect(document.getElementById('collection-results-info').textContent).toBe('1 de 2 juegos');
        expect(document.getElementById('btn-clear-collection-search').classList.contains('hidden')).toBe(false);
    });

    it('encuentra por estudio (cargado de forma asíncrona)', async () => {
        await cargarMiLista(ID_USUARIO);

        const input = document.getElementById('collection-search-input');
        input.value = 'team cherry';
        input.dispatchEvent(new Event('input', { bubbles: true }));

        expect(idsEnGrid()).toEqual(['card-entry-1']);
    });

    it('muestra el mensaje de sin resultados y oculta el grid', async () => {
        await cargarMiLista(ID_USUARIO);

        const input = document.getElementById('collection-search-input');
        input.value = 'doom';
        input.dispatchEvent(new Event('input', { bubbles: true }));

        expect(idsEnGrid()).toEqual([]);
        expect(document.getElementById('no-results-message').classList.contains('hidden')).toBe(false);
        expect(document.getElementById('no-results-query').textContent).toBe('"doom"');
        expect(document.getElementById('game-grid').classList.contains('hidden')).toBe(true);
    });

    it('reordena el grid según el criterio elegido', async () => {
        await cargarMiLista(ID_USUARIO);

        const select = document.getElementById('collection-sort-select');
        select.value = 'nombre-asc';
        select.dispatchEvent(new Event('change', { bubbles: true }));

        // Celeste (30 h) va antes que Hollow Knight (12 h) alfabéticamente.
        expect(idsEnGrid()).toEqual(['card-entry-2', 'card-entry-1']);
    });

    it('ordena por horas jugadas de más a menos', async () => {
        await cargarMiLista(ID_USUARIO);

        const select = document.getElementById('collection-sort-select');
        select.value = 'horas-desc';
        select.dispatchEvent(new Event('change', { bubbles: true }));

        // Celeste tiene 30 h y Hollow Knight 12 h.
        expect(idsEnGrid()).toEqual(['card-entry-2', 'card-entry-1']);
    });

    it('limpiar la búsqueda restaura todas las tarjetas', async () => {
        await cargarMiLista(ID_USUARIO);

        const input = document.getElementById('collection-search-input');
        input.value = 'doom';
        input.dispatchEvent(new Event('input', { bubbles: true }));

        document.getElementById('btn-clear-filters').click();

        expect(input.value).toBe('');
        expect(idsEnGrid()).toEqual(['card-entry-1', 'card-entry-2']);
        expect(document.getElementById('no-results-message').classList.contains('hidden')).toBe(true);
        expect(document.getElementById('game-grid').classList.contains('hidden')).toBe(false);
    });

    it('con la colección vacía no muestra los controles', async () => {
        listaEntradas = [];
        await cargarMiLista(ID_USUARIO);

        expect(document.getElementById('collection-controls').classList.contains('hidden')).toBe(true);
        expect(document.getElementById('collection-results-info').textContent).toBe('');
        expect(document.getElementById('game-grid').classList.contains('hidden')).toBe(false);
    });
});
