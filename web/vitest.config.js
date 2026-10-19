import { defineConfig } from 'vitest/config';

export default defineConfig({
    test: {
        environment: 'jsdom',
        include: ['recursos/**/__tests__/**/*.{test,spec}.js'],
        setupFiles: ['./vitest.setup.js'],
    },
});
