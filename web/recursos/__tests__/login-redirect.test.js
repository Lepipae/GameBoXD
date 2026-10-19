import { describe, test, expect, beforeEach, afterEach } from 'vitest';
import {
    saveReturnUrl,
    getReturnUrl,
    clearReturnUrl,
    getLoginRedirectUrl,
} from '../redirect.js';

/**
 * Sobrescribe window.location para simular distintas URLs de prueba sin
 * navegar realmente (jsdom no carga páginas al asignar location.href).
 *
 * @param {string} url - URL completa a simular.
 */
function setLocation(url) {
    const base = window.location.origin;
    const u = new URL(url, base);
    window.history.replaceState(null, '', u.pathname + u.search);
}

function setReferrer(referrer) {
    Object.defineProperty(document, 'referrer', {
        writable: true, configurable: true, value: referrer,
    });
}

describe('getLoginRedirectUrl()', () => {
    beforeEach(() => {
        sessionStorage.clear();
        setLocation('http://localhost/login.html');
        setReferrer('');
    });

    afterEach(() => {
        sessionStorage.clear();
    });

    test('devuelve index.html por defecto si no hay referrer ni ?next=', () => {
        expect(getLoginRedirectUrl()).toBe('index.html');
    });

    test('devuelve el valor de ?next= cuando está presente', () => {
        setLocation('http://localhost/login.html?next=miLista.html');
        expect(getLoginRedirectUrl()).toBe('miLista.html');
    });

    test('devuelve el valor de ?next= con parámetros de consulta', () => {
        setLocation('http://localhost/login.html?next=detalleJuego.html?id=5');
        expect(getLoginRedirectUrl()).toBe('detalleJuego.html?id=5');
    });

    test('ignora ?next= con URL absoluta de otro origen (open redirect)', () => {
        setLocation('http://localhost/login.html?next=http://evil.com/');
        expect(getLoginRedirectUrl()).toBe('index.html');
    });

    test('ignora ?next= con ruta relativa al protocolo (//)', () => {
        setLocation('http://localhost/login.html?next=//evil.com/');
        expect(getLoginRedirectUrl()).toBe('index.html');
    });

    test('ignora ?next= con javascript:', () => {
        setLocation('http://localhost/login.html?next=javascript:alert(1)');
        expect(getLoginRedirectUrl()).toBe('index.html');
    });

    test('falla al document.referrer cuando no hay ?next y es del mismo origen', () => {
        setLocation('http://localhost/login.html');
        setReferrer(`${window.location.origin}/detalleJuego.html?id=5`);
        expect(getLoginRedirectUrl()).toBe('detalleJuego.html?id=5');
    });

    test('no usa document.referrer de otro origen', () => {
        setLocation('http://localhost/login.html');
        setReferrer('https://evil.com/pagina.html');
        expect(getLoginRedirectUrl()).toBe('index.html');
    });

    test('document.referrer vacío se trata como sin referrer', () => {
        setLocation('http://localhost/login.html');
        setReferrer('');
        expect(getLoginRedirectUrl()).toBe('index.html');
    });
});

describe('saveReturnUrl()', () => {
    beforeEach(() => {
        sessionStorage.clear();
        setLocation('http://localhost/');
        setReferrer('');
    });

    afterEach(() => {
        sessionStorage.clear();
    });

    test('guarda la página actual antes de redirigir al login', () => {
        setLocation('http://localhost/detalleJuego.html?id=7');
        saveReturnUrl();
        expect(getReturnUrl()).toBe('/detalleJuego.html?id=7');
    });

    test('guarda la URL pasada explícitamente', () => {
        setLocation('http://localhost/index.html');
        saveReturnUrl('miLista.html');
        expect(getReturnUrl()).toBe('miLista.html');
    });

    test('no guarda una URL insegura (open redirect)', () => {
        setLocation('http://localhost/index.html');
        saveReturnUrl('http://evil.com/');
        expect(getReturnUrl()).toBeNull();
    });
});

describe('clearReturnUrl()', () => {
    beforeEach(() => {
        sessionStorage.clear();
        setLocation('http://localhost/miLista.html');
        setReferrer('');
    });

    afterEach(() => {
        sessionStorage.clear();
    });

    test('elimina la URL de retorno después de usarla', () => {
        saveReturnUrl();
        expect(getReturnUrl()).toBe('/miLista.html');

        clearReturnUrl();
        expect(getReturnUrl()).toBeNull();
    });
});
