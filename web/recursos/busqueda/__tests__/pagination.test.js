/**
 * Pruebas de paginación para busquedaScript.js.
 *
 * El backend /api/videojuegos aún no paginando (devuelve List<Videojuego>),
 * así que el script simula la paginación por cliente: solicita
 * `?page=&size=` y corta el array recibido. Estas pruebas verifican que el
 * frontend ya no carga todos los juegos de una vez.
 */
import { describe, it, expect, vi, beforeEach, beforeAll, afterEach } from 'vitest';

function crearJuegoMock(i) {
    return {
        miId: i,
        nombre: `Juego de prueba ${i}`,
        urlImagen: `https://ejemplo.test/imagenes/juego${i}.png`,
        idDesarrolladora: 1,
        tags: ['prueba'],
    };
}

const JUEGOS_MOCK = Array.from({ length: 30 }, (_, i) => crearJuegoMock(i + 1));

// Juegos que el mock devolverá en cada llamada a fetch. Los tests pueden
// reemplazarlo para simular distintas respuestas del servidor.
let juegosParaFetch = JUEGOS_MOCK;

let fetchMock;
let mod;

beforeAll(async () => {
    // Stubs requeridos por busquedaScript.js antes de importarlo (se ejecuta
    // código de nivel superior al importar).
    window.GameBoXDImagenes = {
        tieneUrlImagen: (url) => typeof url === 'string' && url.trim() !== '',
        aplicarImagen: (img, url) => {
            img.src = url || 'recursos/img/placeholder-game.svg';
        },
    };

    window.GameBoXDApi = {
        BASE_URL: 'http://localhost:4000/api',
    };

    fetchMock = vi.fn(async (url) => {
        const u = new URL(url);
        const page = Number(u.searchParams.get('page') || 0);
        const size = Number(u.searchParams.get('size') || 50);
        const desde = page * size;
        const slice = juegosParaFetch.slice(desde, desde + size);
        return {
            ok: true,
            status: 200,
            headers: {
                get: (nombre) =>
                    nombre === 'X-Total-Count' ? String(juegosParaFetch.length) : null,
            },
            json: async () => slice,
        };
    });
    vi.stubGlobal('fetch', fetchMock);

    mod = await import('../busquedaScript.js');
});

beforeEach(() => {
    juegosParaFetch = JUEGOS_MOCK;
    fetchMock = vi.fn(async (url) => {
        const u = new URL(url);
        const page = Number(u.searchParams.get('page') || 0);
        const size = Number(u.searchParams.get('size') || 50);
        const desde = page * size;
        const slice = juegosParaFetch.slice(desde, desde + size);
        return {
            ok: true,
            status: 200,
            // Cabecera que enviaría un backend con paginación real.
            headers: {
                get: (nombre) =>
                    nombre === 'X-Total-Count' ? String(juegosParaFetch.length) : null,
            },
            json: async () => slice,
        };
    });
    vi.stubGlobal('fetch', fetchMock);
});

afterEach(() => {
    vi.unstubAllGlobals();
});

function montarDOM() {
    document.body.innerHTML = `
        <div id="search-loader"></div>
        <div id="results-grid"></div>
        <div id="results-info-text"></div>
        <div id="contenedor-cargar-mas">
            <button id="btn-cargar-mas" type="button">Cargar más</button>
        </div>
    `;
}

function ultimoUrlFetch() {
    const llamadas = fetchMock.mock.calls.map((args) => String(args[0]));
    return llamadas[llamadas.length - 1];
}

async function flushPromises() {
    return new Promise((resolve) => setTimeout(resolve, 0));
}

describe('paginación de búsqueda (busquedaScript)', () => {
    it('la búsqueda inicial solicita solo la página 0 y no carga todos los juegos', async () => {
        montarDOM();

        await mod.cargarYFiltrarJuegos('prueba', '', '', '', 0, true);

        expect(fetchMock).toHaveBeenCalledTimes(1);
        const url = fetchMock.mock.calls[0][0];
        expect(url).toContain('/videojuegos');
        expect(url).toContain('page=0');
        expect(url).toContain(`size=${mod.PAGE_SIZE}`);

        const grid = document.getElementById('results-grid');
        // Se renderiza solo una página (PAGE_SIZE), no el catálogo entero.
        expect(grid.children.length).toBe(mod.PAGE_SIZE);
        expect(grid.children.length).toBeLessThan(JUEGOS_MOCK.length);
        expect(mod.page).toBe(0);
    });

    it('"Cargar más" incrementa la página y añade resultados sin reemplazarlos', async () => {
        montarDOM();

        await mod.cargarYFiltrarJuegos('prueba', '', '', '', 0, true);
        const despuesPrimeraPagina = document.getElementById('results-grid').children.length;
        expect(despuesPrimeraPagina).toBe(mod.PAGE_SIZE);

        const btn = document.getElementById('btn-cargar-mas');
        btn.addEventListener('click', async () => {
            await mod.cargarYFiltrarJuegos('prueba', '', '', '', mod.page + 1);
        });
        btn.click();
        await flushPromises();

        // Segunda petición trae la página 1.
        expect(fetchMock).toHaveBeenCalledTimes(2);
        expect(ultimoUrlFetch()).toContain('page=1');

        const grid = document.getElementById('results-grid');
        // Los resultados se acumulan (append), no se reemplazan.
        expect(grid.children.length).toBeGreaterThan(despuesPrimeraPagina);
        expect(grid.children.length).toBe(JUEGOS_MOCK.length);
        expect(mod.page).toBe(1);
        // El primer juego de la página 0 sigue presente.
        expect(grid.firstElementChild.textContent).toContain('Juego de prueba 1');
    });

    it('oculta el botón "Cargar más" cuando no quedan más páginas', async () => {
        montarDOM();
        // 25 juegos con PAGE_SIZE = 24 => 2 páginas en total.
        juegosParaFetch = JUEGOS_MOCK.slice(0, 25);

        await mod.cargarYFiltrarJuegos('prueba', '', '', '', 0, true);
        const contenedor = document.getElementById('contenedor-cargar-mas');
        const btn = document.getElementById('btn-cargar-mas');

        // Queda una página por cargar: el botón debe estar visible.
        expect(mod.totalPages).toBe(2);
        expect(contenedor.style.display).not.toBe('none');
        expect(btn.disabled).toBe(false);

        btn.addEventListener('click', async () => {
            await mod.cargarYFiltrarJuegos('prueba', '', '', '', mod.page + 1);
        });
        btn.click();
        await flushPromises();

        // Se cargó la última página: el botón se oculta/deshabilita.
        expect(fetchMock).toHaveBeenCalledTimes(2);
        expect(mod.page).toBe(1);
        expect(document.getElementById('results-grid').children.length).toBe(25);
        expect(contenedor.style.display).toBe('none');
        expect(btn.disabled).toBe(true);
    });

    it('una nueva búsqueda reinicia la página a 0', async () => {
        montarDOM();

        await mod.cargarYFiltrarJuegos('prueba', '', '', '', 0, true);
        await mod.cargarYFiltrarJuegos('prueba', '', '', '', mod.page + 1);
        expect(mod.page).toBe(1);
        expect(document.getElementById('results-grid').children.length).toBe(JUEGOS_MOCK.length);

        // Nueva búsqueda: se reinicia la paginación y se vuelve a pedir page 0.
        await mod.cargarYFiltrarJuegos('', '', '', '', 0, true);
        expect(mod.page).toBe(0);
        expect(ultimoUrlFetch()).toContain('page=0');
        const grid = document.getElementById('results-grid');
        expect(grid.children.length).toBe(mod.PAGE_SIZE);
        expect(fetchMock).toHaveBeenCalledTimes(3);
    });
});
