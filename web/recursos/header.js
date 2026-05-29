/**
 * Script compartido para el manejo unificado de la cabecera (Header) en todas las páginas de GameBoXD.
 * Controla la visualización de los botones según la sesión activa y configura sus acciones.
 */
document.addEventListener('DOMContentLoaded', () => {
    verificarSesionHeader();
});

/**
 * Comprueba si el usuario tiene una sesión activa y adapta los botones del header.
 */
function verificarSesionHeader() {
    const token = localStorage.getItem('jwt_token');
    const btnTodosJuegos = document.getElementById('btn-todos-juegos');
    const btnLista = document.getElementById('btn-mi-lista');
    const linkLista = document.getElementById('link-mi-lista');
    const userIconLink = document.getElementById('btn-user-icon');

    // El botón "Ver todos los juegos" siempre está visible
    if (btnTodosJuegos) btnTodosJuegos.style.display = 'inline-block';

    // 1. Adaptar el comportamiento y visibilidad según si existe sesión activa
    if (!token) {
        // No hay sesión: Ocultamos la lista del header
        if (btnLista) btnLista.style.display = 'none';
        if (linkLista) linkLista.style.display = 'none';

        // El icono de usuario redirige al formulario de acceso/registro
        if (userIconLink) {
            userIconLink.href = 'login.html';
        }
    } else {
        // Hay sesión activa: Mostramos botones correspondientes
        if (btnLista) btnLista.style.display = 'inline-block';
        if (linkLista) linkLista.style.display = 'inline-block';

        // El icono de usuario redirige directamente a la lista
        if (userIconLink) {
            userIconLink.href = 'miLista.html';
        }
    }

    // 2. Configurar el comportamiento de clic en "Ver todos los juegos"
    if (btnTodosJuegos) {
        btnTodosJuegos.addEventListener('click', () => {
            window.location.href = 'busqueda.html?q';
        });
    }
}
