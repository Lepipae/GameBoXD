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
    const btnPerfil = document.getElementById('btn-mi-perfil');
    const btnLista = document.getElementById('btn-mi-lista');
    const linkLista = document.getElementById('link-mi-lista');
    const userIconLink = document.getElementById('btn-user-icon');

    // 1. Adaptar el comportamiento y visibilidad según si existe sesión activa
    if (!token) {
        // No hay sesión: Ocultamos el perfil y la lista del header
        if (btnPerfil) btnPerfil.style.display = 'none';
        if (btnLista) btnLista.style.display = 'none';
        if (linkLista) linkLista.style.display = 'none';
        
        // El icono de usuario redirige al formulario de acceso/registro
        if (userIconLink) {
            userIconLink.href = 'login.html';
        }
    } else {
        // Hay sesión activa: Mostramos botones correspondientes
        if (btnPerfil) btnPerfil.style.display = 'inline-block';
        if (btnLista) btnLista.style.display = 'inline-block';
        if (linkLista) linkLista.style.display = 'inline-block';
        
        // El icono de usuario redirige directamente a la lista
        if (userIconLink) {
            userIconLink.href = 'miLista.html';
        }
    }

    // 2. Configurar el comportamiento de clic en "Mi perfil"
    // Actualmente redirige a la página principal de GameBoXD
    if (btnPerfil) {
        btnPerfil.addEventListener('click', () => {
            window.location.href = 'index.html';
        });
    }
}
