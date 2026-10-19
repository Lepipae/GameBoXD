/**
 * Sistema Toast de Notificaciones (compartido).
 *
 * Centraliza la lógica de showToast/hideToast que antes estaba duplicada en
 * insertarScript.js y miListaScript.js.
 *
 * @param {string} message Texto del toast.
 * @param {'success'|'error'|'info'|'warning'} [type='success'] Tipo visual del toast.
 */
export function showToast(message, type = 'success') {
    const toastContainer = document.getElementById('toast-container');
    if (!toastContainer) return;

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    const content = document.createElement('div');
    content.className = 'toast-content';
    content.textContent = message;

    const closeBtn = document.createElement('button');
    closeBtn.className = 'toast-close';
    closeBtn.innerHTML = '&times;';
    closeBtn.type = 'button';
    closeBtn.setAttribute('aria-label', 'Cerrar notificación');

    closeBtn.addEventListener('click', () => {
        hideToast(toast);
    });

    toast.appendChild(content);
    toast.appendChild(closeBtn);
    toastContainer.appendChild(toast);

    // Auto-remove despues de 4.5 segundos
    setTimeout(() => {
        hideToast(toast);
    }, 4500);
}

/**
 * Elimina un toast del DOM.
 *
 * @param {HTMLElement|null|undefined} toastElement Elemento toast a eliminar.
 */
export function hideToast(toastElement) {
    if (!toastElement) return;
    toastElement.remove();
}
