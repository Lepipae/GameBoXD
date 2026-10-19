import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { showToast, hideToast } from '../ui.js';

describe('ui.showToast / hideToast', () => {
    beforeEach(() => {
        document.body.innerHTML = '';
        const container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    });

    afterEach(() => {
        vi.useRealTimers();
        document.body.innerHTML = '';
    });

    it('appends a .toast.toast-success element to #toast-container', () => {
        vi.useFakeTimers();
        showToast('Operación completada', 'success');

        const container = document.getElementById('toast-container');
        const toast = container.querySelector('.toast.toast-success');

        expect(toast).not.toBeNull();
        expect(container.contains(toast)).toBe(true);

        vi.useRealTimers();
    });

    it('sets the message text inside .toast-content', () => {
        vi.useFakeTimers();
        showToast('Hola mundo', 'success');

        const content = document.querySelector('.toast-content');
        expect(content).not.toBeNull();
        expect(content.textContent).toBe('Hola mundo');

        vi.useRealTimers();
    });

    it('renders a .toast-close button with &times;', () => {
        vi.useFakeTimers();
        showToast('msg', 'success');

        const closeBtn = document.querySelector('.toast-close');
        expect(closeBtn).not.toBeNull();
        expect(closeBtn.tagName).toBe('BUTTON');
        expect(closeBtn.textContent).toBe('×');
        expect(closeBtn.type).toBe('button');

        vi.useRealTimers();
    });

    it('removes the toast from the DOM when close is clicked', () => {
        vi.useFakeTimers();
        showToast('msg', 'success');

        const closeBtn = document.querySelector('.toast-close');
        closeBtn.click();

        expect(document.querySelector('.toast')).toBeNull();

        vi.useRealTimers();
    });

    it('auto-removes the toast after ~4.5 seconds', () => {
        vi.useFakeTimers();
        showToast('msg', 'success');

        expect(document.querySelector('.toast')).not.toBeNull();

        vi.advanceTimersByTime(4500);

        expect(document.querySelector('.toast')).toBeNull();

        vi.useRealTimers();
    });

    it('is a graceful no-op when #toast-container does not exist', () => {
        vi.useFakeTimers();
        document.body.innerHTML = '';

        expect(() => showToast('msg', 'success')).not.toThrow();
        expect(document.querySelector('.toast')).toBeNull();

        vi.useRealTimers();
    });
});
