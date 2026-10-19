// Globales que los scripts de GameBoXD esperan del entorno del navegador.
// api.js expone GameBoXDApi y GameBoXDImagenes; en los tests de vitest (jsdom)
// esos scripts no se cargan, así que se simulan aquí antes de importar ningún
// script de pruebas.
window.GameBoXDApi = { BASE_URL: 'https://api.test.local/api' };
window.GameBoXDImagenes = { aplicarImagen: () => {} };

// Esqueleto mínimo del DOM que los scripts tocan al importarse y al renderizar,
// para evitar fallos por elementos ausentes.
document.body.innerHTML = `
    <!-- Detalle de juego -->
    <div id="loading-state"></div>
    <div id="game-detail-wrapper"></div>
    <div id="reviews-wrapper"></div>
    <div id="reviews-container"></div>

    <!-- Mi lista -->
    <div id="auth-view"></div>
    <div id="list-view"></div>
    <span id="user-name-span"></span>
    <img id="user-avatar">
    <div id="user-avatar-placeholder"></div>
    <button id="btn-logout"></button>
    <div id="list-loader"></div>
    <div id="empty-list-message" class="hidden"></div>
    <div id="game-grid"></div>
    <div id="toast-container"></div>
`;
