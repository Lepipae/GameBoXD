import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

const flushPromises = () => new Promise((resolve) => setTimeout(resolve, 0));

describe('overlay de error al fallar la API (pistachos)', () => {
    let fetchSpy;
    let alertSpy;

    beforeEach(() => {
        // Dependencias globales que lee busquedaScript.js
        window.GameBoXDImagenes = {
            tieneUrlImagen: () => true,
            aplicarImagen: () => {},
        };
        window.GameBoXDApi = { BASE_URL: 'http://api.test' };

        alertSpy = vi.spyOn(window, 'alert');
        fetchSpy = vi.fn().mockRejectedValue(new Error('La API no está disponible'));
        window.fetch = fetchSpy;

        document.body.innerHTML = '';
    });

    afterEach(() => {
        vi.restoreAllMocks();
        document.body.innerHTML = '';
    });

    it('no llama a alert(), muestra el overlay con error-pistachos.svg, mensaje amigable y botón Reintentar que vuelve a disparar el fetch', async () => {
        // Importar el módulo (registra iniciar() al cargar)
        await import('../busquedaScript.js');

        // Simular que la página terminó de cargar para disparar la carga inicial
        document.dispatchEvent(new Event('DOMContentLoaded'));

        // Dejar que la promesa de fetch rechazada y su catch se resuelvan
        await flushPromises();

        // 1. Nunca se debe llamar a alert()
        expect(alertSpy).not.toHaveBeenCalled();

        // 2. Aparece un overlay en el DOM que usa error-pistachos.svg
        const overlay = document.getElementById('error-pistachos-overlay');
        expect(overlay).not.toBeNull();
        expect(overlay.querySelector('img[src*="error-pistachos.svg"]')).not.toBeNull();

        // 3. El overlay muestra el mensaje amigable
        expect(overlay.textContent).toContain('La API no está disponible. Inténtalo más tarde');

        // 4. El overlay contiene un botón "Reintentar"
        const botonReintentar = overlay.querySelector('button');
        expect(botonReintentar).not.toBeNull();
        expect(botonReintentar.textContent).toBe('Reintentar');

        // 5. Al hacer clic en "Reintentar" se vuelve a disparar el fetch
        const llamadasAntes = fetchSpy.mock.calls.length;
        botonReintentar.click();
        await flushPromises();
        expect(fetchSpy.mock.calls.length).toBe(llamadasAntes + 1);
    });
});
