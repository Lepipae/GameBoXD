/**
 * El mayor peso de la aplicación no eran las respuestas de la API, sino las
 * portadas: PNG de ~730 KB cada una. `api.js` reescribe las URLs del CDN de
 * SteamGridDB para pedirlas al backend, que las devuelve a 300 px en JPEG
 * (~30 KB) y cae al original si el redimensionador falla.
 *
 * Estas pruebas fijan ese contrato:
 *  - solo se reescriben URLs del CDN permitido,
 *  - un host desconocido se sirve tal cual (no se manda al backend),
 *  - si la versión optimizada falla, se reintenta el original,
 *  - y solo entonces se usa el placeholder local.
 */

import { describe, it, expect, beforeAll, beforeEach } from 'vitest';
import '../api.js';

const { optimizarUrlImagen, aplicarImagen, aplicarImagenResponsiva, IMAGEN_PLACEHOLDER } = window.GameBoXDImagenes;

const CDN = 'https://cdn2.steamgriddb.com/grid/abc123.png';

/** Lanza el evento `error` en la imagen, como haría el navegador. */
function fallarImagen(img) {
    img.dispatchEvent(new Event('error'));
}

describe('optimizarUrlImagen — portadas servidas por el backend', () => {
    it('reescribe una URL de SteamGridDB al endpoint de redimensionado', () => {
        const url = optimizarUrlImagen(CDN, 300);

        expect(url).toContain('/imagenes?url=');
        expect(url).toContain('&w=300');
        expect(url).toContain(encodeURIComponent(CDN));
        expect(url.startsWith(window.GameBoXDApi.BASE_URL)).toBe(true);
    });

    it('respeta el ancho pedido (la ficha pide 600, las tarjetas 300)', () => {
        expect(optimizarUrlImagen(CDN, 600)).toContain('&w=600');
        expect(optimizarUrlImagen(CDN, 200)).toContain('&w=200');
    });

    it('añade &h= solo cuando se pide recorte', () => {
        // El hero pinta un marco 16:9: pide 600x338 para no descargar 600x900.
        const hero = optimizarUrlImagen(CDN, 600, 338);
        expect(hero).toContain('&w=600');
        expect(hero).toContain('&h=338');

        // Sin alto (o con 0) el backend conserva la proporción: no viaja `h`.
        expect(optimizarUrlImagen(CDN, 600)).not.toContain('&h=');
        expect(optimizarUrlImagen(CDN, 600, 0)).not.toContain('&h=');
        expect(optimizarUrlImagen(CDN, 600, null)).not.toContain('&h=');
    });

    it('no reescribe hosts ajenos al CDN de portadas', () => {
        const ajena = 'https://ejemplo.com/portada.png';
        expect(optimizarUrlImagen(ajena, 300)).toBe(ajena);
    });

    it('deja intactas las URLs vacías o inválidas', () => {
        expect(optimizarUrlImagen('', 300)).toBe('');
        expect(optimizarUrlImagen(null, 300)).toBe('');
        expect(optimizarUrlImagen('no es una url', 300)).toBe('no es una url');
    });
});

describe('aplicarImagen — cadena de respaldo', () => {
    let img;

    beforeEach(() => {
        document.body.innerHTML = '';
        img = document.createElement('img');
        document.body.appendChild(img);
    });

    it('empieza pidiendo la versión optimizada', () => {
        aplicarImagen(img, CDN, null, 300);
        expect(img.getAttribute('src')).toContain('/imagenes?url=');
    });

    it('traslada el alto para que el backend recorte el marco', () => {
        aplicarImagen(img, CDN, null, 600, 338);
        const src = img.getAttribute('src');
        expect(src).toContain('&w=600');
        expect(src).toContain('&h=338');
    });

    it('si la optimizada falla, reintenta la URL original', () => {
        aplicarImagen(img, CDN, null, 300);
        fallarImagen(img);

        expect(img.getAttribute('src')).toBe(CDN);
    });

    it('si el original también falla, cae al placeholder local', () => {
        aplicarImagen(img, CDN, null, 300);
        fallarImagen(img); // falla la optimizada -> original
        fallarImagen(img); // falla el original -> placeholder

        expect(img.getAttribute('src')).toBe(IMAGEN_PLACEHOLDER);
    });

    it('sin URL no hace ninguna petición: placeholder directo', () => {
        aplicarImagen(img, null, null, 300);
        expect(img.getAttribute('src')).toBe(IMAGEN_PLACEHOLDER);
    });
});

describe('aplicarImagenResponsiva — srcset', () => {
    let img;

    beforeEach(() => {
        document.body.innerHTML = '';
        img = document.createElement('img');
        document.body.appendChild(img);
    });

    /** Devuelve los descriptores `w` del srcset, en orden. */
    function anchos() {
        return (img.getAttribute('srcset') || '')
            .split(', ')
            .map((candidato) => candidato.split(' ')[1]);
    }

    it('ofrece cada ancho como candidato del srcset', () => {
        aplicarImagenResponsiva(img, CDN, null, [140, 180, 300], '180px');

        expect(anchos()).toEqual(['140w', '180w', '300w']);
        expect(img.getAttribute('srcset')).toContain('/imagenes?url=');
        expect(img.getAttribute('sizes')).toBe('180px');
    });

    it('deja en `src` el mayor candidato, para navegadores sin srcset', () => {
        aplicarImagenResponsiva(img, CDN, null, [140, 180, 300], '180px');

        expect(img.getAttribute('src')).toContain('&w=300');
    });

    it('si la variante elegida falla, quita el srcset y pide el original', () => {
        aplicarImagenResponsiva(img, CDN, null, [140, 180, 300], '180px');
        fallarImagen(img);

        expect(img.getAttribute('src')).toBe(CDN);
        expect(img.hasAttribute('srcset')).toBe(false);
        expect(img.hasAttribute('sizes')).toBe(false);
    });

    it('si el original también falla, cae al placeholder local', () => {
        aplicarImagenResponsiva(img, CDN, null, [140, 180, 300], '180px');
        fallarImagen(img);
        fallarImagen(img);

        expect(img.getAttribute('src')).toBe(IMAGEN_PLACEHOLDER);
    });

    it('con un host ajeno no inventa variantes: usa la URL tal cual', () => {
        // Optimizar aquí apuntaría al redimensionador con una URL que no puede
        // descargar (lista blanca), así que se degrada a aplicarImagen.
        aplicarImagenResponsiva(img, 'https://ejemplo.com/portada.png', null, [140, 180], '180px');

        expect(img.hasAttribute('srcset')).toBe(false);
        expect(img.getAttribute('src')).toBe('https://ejemplo.com/portada.png');
    });
});
