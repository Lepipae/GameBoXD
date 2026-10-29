import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
    login,
    logout,
    getToken,
    getCurrentUser,
    isAuthenticated,
    setSession,
    clearSession,
} from '../auth.js';

const BASE_URL = 'http://test-api.local/api';

beforeEach(() => {
    window.GameBoXDApi = { BASE_URL };
    window.fetch = vi.fn();
    sessionStorage.clear();
});

afterEach(() => {
    sessionStorage.clear();
    delete window.GameBoXDApi;
    delete window.fetch;
});

describe('login()', () => {
    it('stores jwt_token and currentUser in sessionStorage on success', async () => {
        const token = 'jwt-test-token';
        const userProfile = { miId: 7, nombre: 'ana', urlImagen: 'http://img.png' };

        window.fetch
            .mockResolvedValueOnce({ ok: true, json: async () => ({ jwt: token }) })
            .mockResolvedValueOnce({ ok: true, json: async () => userProfile });

        await login('ana', 'password123');

        expect(sessionStorage.getItem('jwt_token')).toBe(token);
        expect(sessionStorage.getItem('currentUser')).toBe(
            JSON.stringify({ token, miId: 7, nombre: 'ana', urlImagen: 'http://img.png' })
        );
        expect(window.fetch).toHaveBeenCalledTimes(2);
    });

    it('does not store anything on failure', async () => {
        window.fetch.mockResolvedValueOnce({ ok: false, status: 401 });

        await login('ana', 'wrong-password');

        expect(sessionStorage.getItem('jwt_token')).toBeNull();
        expect(sessionStorage.getItem('currentUser')).toBeNull();
    });
});

describe('logout()', () => {
    it('removes both keys', () => {
        setSession('tok', { miId: 1, nombre: 'ana' });
        logout();
        expect(sessionStorage.getItem('jwt_token')).toBeNull();
        expect(sessionStorage.getItem('currentUser')).toBeNull();
    });
});

describe('getToken()', () => {
    it('returns the stored token or null', () => {
        expect(getToken()).toBeNull();
        sessionStorage.setItem('jwt_token', 'abc-123');
        expect(getToken()).toBe('abc-123');
    });
});

describe('getCurrentUser()', () => {
    it('returns the parsed user object or null', () => {
        expect(getCurrentUser()).toBeNull();

        sessionStorage.setItem('currentUser', JSON.stringify({ miId: 1, nombre: 'ana' }));
        expect(getCurrentUser()).toEqual({ miId: 1, nombre: 'ana' });

        sessionStorage.setItem('currentUser', 'not-json-at-all');
        expect(getCurrentUser()).toBeNull();
    });
});

describe('isAuthenticated()', () => {
    it('returns true when a token exists', () => {
        expect(isAuthenticated()).toBe(false);
        sessionStorage.setItem('jwt_token', 'abc-123');
        expect(isAuthenticated()).toBe(true);
    });
});

describe('setSession() / clearSession()', () => {
    it('round-trips correctly', () => {
        const user = { token: 't', miId: 1, nombre: 'ana', urlImagen: 'u' };

        setSession('t', user);
        expect(getToken()).toBe('t');
        expect(getCurrentUser()).toEqual(user);
        expect(isAuthenticated()).toBe(true);

        clearSession();
        expect(getToken()).toBeNull();
        expect(getCurrentUser()).toBeNull();
        expect(isAuthenticated()).toBe(false);
    });
});
