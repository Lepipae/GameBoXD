/**
 * Hero del index: en lugar de una imagen fija de Helldivers 2, la página
 * pide al backend el juego con más entradas de lista en ese momento y usa
 * su propia carátula (urlImagen) como imagen del hero.
 *
 * Comprueba:
 *  - Que se llama a GET /api/videojuegos/mas-resenyas y que el resultado se
 *    pinta en el hero (enlace a la ficha + carátula + alt con el nombre).
 *  - Que si el endpoint falla o no hay reseñas el hero se queda oculto sin
 *    romper el resto de la página.
 *  - Que index.html ya no contiene la imagen fija de Helldivers ni URLs de
 *    imágenes externas hardcodeadas en el hero.
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import fs from 'fs';
import path from 'path';

const WEB_ROOT = path.resolve(__dirname, '..', '..', '..');
const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0));

/** Mock de fetch que enruta por URL: el hero y el listado de recomendaciones. */
function crearFetchMock({ hero, juegos = [] }) {
    return vi.fn((url) => {
        if (String(url).includes('/videojuegos/mas-resenyas')) {
            return Promise.resolve(hero);
        }
        if (String(url).includes('/videojuegos')) {
            return Promise.resolve({ ok: true, json: async () => juegos });
        }
        return Promise.reject(new Error(`URL no soportada en el test: ${url}`));
    });
}

describe('Hero dinámico: juego con más reseñas', () => {
    beforeEach(() => {
        window.GameBoXDApi = { BASE_URL: 'http://api.test/api' };
        window.GameBoXDImagenes = {
            tieneUrlImagen: (url) => typeof url === 'string' && url.trim() !== '',
            aplicarImagen: (img, url) => {
                if (typeof url === 'string' && url.trim() !== '') img.src = url.trim();
            },
            aplicarImagenResponsiva: (img, url, alFallar, anchos, sizes) => {
                if (typeof url === 'string' && url.trim() !== '') img.src = url.trim();
                img.setAttribute('sizes', sizes);
                img.setAttribute('srcset', anchos.map((a) => `${url} ${a}w`).join(', '));
            },
        };

        document.body.innerHTML = `
            <section class="hero-section">
              <div class="hero-left"><h2>Lo mas reseñado</h2></div>
              <div class="hero-right" id="hero-right" hidden>
                <a id="hero-enlace"><img id="hero-imagen" alt="" class="hero-image"></a>
              </div>
            </section>
            <div class="contenedor-recomendaciones">
              <div class="recomendacion" id="r1"></div>
              <div class="recomendacion" id="r2"></div>
              <div class="recomendacion" id="r3"></div>
              <div class="recomendacion" id="r4"></div>
            </div>
        `;
    });

    afterEach(() => {
        vi.restoreAllMocks();
        vi.resetModules();
        document.body.innerHTML = '';
        delete window.GameBoXDApi;
        delete window.GameBoXDImagenes;
    });

    it('pinta el juego devuelto por /videojuegos/mas-resenyas con su carátula y enlace a la ficha', async () => {
        const juego = {
            miId: 42,
            nombre: 'Portal',
            urlImagen: 'https://covers.example/portal.jpg',
        };
        window.fetch = crearFetchMock({ hero: { ok: true, json: async () => juego } });

        await import('../indexScript.js');
        document.dispatchEvent(new Event('DOMContentLoaded'));
        await flushPromises();
        await flushPromises();

        // Se pidió el ranking al backend
        const llamadaHero = window.fetch.mock.calls.find(([url]) =>
            String(url).includes('/api/videojuegos/mas-resenyas'),
        );
        expect(llamadaHero, 'debe llamarse a GET /videojuegos/mas-resenyas').toBeTruthy();

        // Enlace a la ficha del juego
        const enlace = document.getElementById('hero-enlace');
        expect(enlace.getAttribute('href')).toBe('detalleJuego.html?id=42');

        // Carátula del propio juego (no una imagen de hero fija)
        const img = document.getElementById('hero-imagen');
        expect(img.getAttribute('src')).toBe('https://covers.example/portal.jpg');
        expect(img.getAttribute('alt')).toBe('Portal');

        // El hero se muestra
        expect(document.getElementById('hero-right').hidden).toBe(false);
    });

    it('si el endpoint falla o no hay reseñas, el hero queda oculto sin romper la página', async () => {
        window.fetch = crearFetchMock({
            hero: { ok: false, status: 404, json: async () => ({ error: 'No hay ninguna entrada' }) },
        });

        await import('../indexScript.js');
        expect(() => {
            document.dispatchEvent(new Event('DOMContentLoaded'));
        }).not.toThrow();

        await flushPromises();
        await flushPromises();

        expect(document.getElementById('hero-right').hidden).toBe(true);
        // El resto de la página sigue viva (las recomendaciones se han renderizado)
        expect(document.querySelectorAll('.recomendacion img').length).toBe(0);
    });

    it('index.html ya no hardcodea Helldivers ni imágenes de CDN en el hero', () => {
        const html = fs.readFileSync(path.join(WEB_ROOT, 'index.html'), 'utf8');

        expect(html).not.toContain('steamgriddb.com');
        expect(html.toLowerCase()).not.toContain('helldivers');

        // El hero está preparado para ser rellenado por indexScript.js
        expect(html).toContain('id="hero-enlace"');
        expect(html).toContain('id="hero-imagen"');
    });

    it('el preload del hero apunta exactamente a la URL que pide el script', () => {
        // Sin este preload la cadena era HTML -> JS -> API -> portada: tres
        // viajes en serie. El navegador solo reutiliza la respuesta si la URL
        // coincide carácter a carácter con la del `fetch`, así que la prueba
        // ata el `href` del <link> al BASE_URL real de api.js.
        const html = fs.readFileSync(path.join(WEB_ROOT, 'index.html'), 'utf8');
        const api = fs.readFileSync(path.join(WEB_ROOT, 'recursos/api.js'), 'utf8');
        const script = fs.readFileSync(
            path.join(WEB_ROOT, 'recursos/index/indexScript.js'), 'utf8');

        const base = api.match(/const BASE_URL = '([^']+)'/)[1];
        const preload = html.match(/<link rel="preload" as="fetch" href="([^"]+)"\s+crossorigin>/);

        expect(preload, 'index.html debe declarar el preload del JSON del hero').not.toBeNull();
        expect(preload[1]).toBe(`${base}/videojuegos/mas-resenyas`);
        expect(script).toContain('/videojuegos/mas-resenyas');
    });

    it('las recomendaciones vienen de /recomendaciones y NO se descarga el catálogo completo', async () => {
        const recomendados = [
            { miId: 1, nombre: 'Uno', urlImagen: 'https://covers.example/1.jpg' },
            { miId: 2, nombre: 'Dos', urlImagen: 'https://covers.example/2.jpg' },
        ];
        window.fetch = crearFetchMock({
            hero: { ok: false, status: 404, json: async () => ({}) },
            juegos: recomendados,
        });

        await import('../indexScript.js');
        document.dispatchEvent(new Event('DOMContentLoaded'));
        await flushPromises();
        await flushPromises();

        const urls = window.fetch.mock.calls.map(([url]) => String(url));

        // Se pide el endpoint de recomendaciones con límite 4...
        expect(urls.some((u) => u.includes('/api/videojuegos/recomendaciones?limit=4'))).toBe(true);
        // ...y nunca el catálogo entero (la causa de los 101 KB en la portada).
        expect(urls.some((u) => u.replace(/\/$/, '').endsWith('/api/videojuegos'))).toBe(false);

        // Las tarjetas se pintan con la respuesta del endpoint. (Cada test
        // reimporta el módulo y deja su listener en `document`, así que se
        // cuentan contenedores con imagen, no imágenes totales.)
        const contenedoresConImagen = [...document.querySelectorAll('.recomendacion')]
            .filter((contenedor) => contenedor.querySelector('img'));
        expect(contenedoresConImagen.length).toBe(2);

        const imagenes = document.querySelectorAll('.recomendacion img');
        expect(imagenes[0].getAttribute('src')).toBe('https://covers.example/1.jpg');
        expect(document.querySelector('#r1 img').getAttribute('loading')).toBe('eager');
        expect(document.querySelector('#r2 img').getAttribute('loading')).toBe('lazy');
    });

    it('las tarjetas de recomendación ofrecen un candidato del tamaño real de la caja', async () => {
        // La tarjeta mide ~260 CSS px en escritorio y antes se pedían siempre
        // 300 px. El candidato de 260 debe existir para que un monitor normal
        // no descargue un 15% de píxeles de más.
        window.fetch = crearFetchMock({
            hero: { ok: false, status: 404, json: async () => ({}) },
            juegos: [{ miId: 1, nombre: 'Uno', urlImagen: 'https://covers.example/1.jpg' }],
        });

        await import('../indexScript.js');
        document.dispatchEvent(new Event('DOMContentLoaded'));
        await flushPromises();
        await flushPromises();

        const img = document.querySelector('#r1 img');
        expect(img).not.toBeNull();

        const candidatos = (img.getAttribute('srcset') || '')
            .split(', ')
            .map((c) => c.split(' ')[1]);
        expect(candidatos).toEqual(['200w', '260w', '300w']);
        expect(img.getAttribute('sizes')).toContain('260px');
    });
});
