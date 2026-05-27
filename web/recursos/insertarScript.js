let desarrolladorasList = [];
const desarrolladorasMap = {}; // Asocia miId -> Nombre

document.addEventListener('DOMContentLoaded', () => {
    cargarDesarrolladoras();
    inicializarPrevisualizaciones();
    inicializarFormularios();
});

// Cargar desarrolladoras de la API y poblar el dropdown select
function cargarDesarrolladoras() {
    const selectDesarrolladora = document.getElementById('videojuego-desarrolladora');
    if (!selectDesarrolladora) return;

    const API_URL = 'http://localhost:8080/api/desarrolladoras';

    fetch(API_URL)
        .then(response => {
            if (!response.ok) throw new Error('Error al cargar desarrolladoras');
            return response.json();
        })
        .then(data => {
            desarrolladorasList = data;
            
            // Limpiar dropdown y mantener la opción placeholder
            selectDesarrolladora.innerHTML = '<option value="" disabled selected>Selecciona desarrolladora...</option>';
            
            // Poblar mapa e items de opción
            desarrolladorasList.forEach(dev => {
                desarrolladorasMap[dev.miId] = dev.nombre;
                
                const option = document.createElement('option');
                option.value = dev.miId;
                option.textContent = dev.nombre;
                selectDesarrolladora.appendChild(option);
            });
            
            // Forzar actualización de la vista previa del videojuego si existía selección
            if (window.actualizarPrevisualizacionJuego) {
                window.actualizarPrevisualizacionJuego();
            }
        })
        .catch(error => {
            console.error('Error al cargar desarrolladoras:', error);
            showToast('No se pudieron obtener las desarrolladoras de la base de datos.', 'error');
        });
}

// Lógica de previsualización reactiva e interactiva (Live Previews)
function inicializarPrevisualizaciones() {
    // Campos Desarrolladora
    const devNombre = document.getElementById('desarrolladora-nombre');
    const devPais = document.getElementById('desarrolladora-pais');
    const devUrl = document.getElementById('desarrolladora-urlImagen');

    const actualizarPrevisualizacionDev = () => {
        const previewNombre = document.getElementById('preview-dev-nombre');
        const previewPais = document.getElementById('preview-dev-pais');
        const previewLogo = document.getElementById('preview-dev-logo');
        const previewPlaceholder = document.getElementById('preview-dev-logo-placeholder');

        if (previewNombre) {
            previewNombre.textContent = devNombre.value.trim() || 'Nombre Desarrolladora';
        }
        if (previewPais) {
            previewPais.textContent = devPais.value.trim() || 'País de origen';
        }

        if (previewLogo && previewPlaceholder) {
            const url = devUrl.value.trim();
            if (url) {
                previewLogo.src = url;
                previewLogo.style.display = 'block';
                previewPlaceholder.style.display = 'none';
                
                previewLogo.onerror = () => {
                    previewLogo.style.display = 'none';
                    previewPlaceholder.style.display = 'flex';
                    previewPlaceholder.textContent = (devNombre.value.trim() ? devNombre.value.trim()[0].toUpperCase() : 'D');
                };
            } else {
                previewLogo.style.display = 'none';
                previewPlaceholder.style.display = 'flex';
                previewPlaceholder.textContent = (devNombre.value.trim() ? devNombre.value.trim()[0].toUpperCase() : 'D');
            }
        }
    };

    if (devNombre) devNombre.addEventListener('input', actualizarPrevisualizacionDev);
    if (devPais) devPais.addEventListener('input', actualizarPrevisualizacionDev);
    if (devUrl) devUrl.addEventListener('input', actualizarPrevisualizacionDev);

    // Campos Videojuego
    const gameNombre = document.getElementById('videojuego-nombre');
    const gameDesc = document.getElementById('videojuego-descripcion');
    const gameUrl = document.getElementById('videojuego-urlImagen');
    const gameNota = document.getElementById('videojuego-notaMedia');
    const gameDev = document.getElementById('videojuego-desarrolladora');
    const gameTags = document.getElementById('videojuego-tags');

    window.actualizarPrevisualizacionJuego = () => {
        const pNombre = document.getElementById('preview-game-nombre');
        const pDesc = document.getElementById('preview-game-desc');
        const pImg = document.getElementById('preview-game-img');
        const pPlaceholder = document.getElementById('preview-game-placeholder');
        const pScore = document.getElementById('preview-game-score');
        const pDev = document.getElementById('preview-game-dev');
        const pTags = document.getElementById('preview-game-tags');

        if (pNombre) pNombre.textContent = gameNombre.value.trim() || 'Título del Juego';
        if (pDesc) pDesc.textContent = gameDesc.value.trim() || 'Sinopsis del juego...';
        
        if (pScore) {
            const nota = parseFloat(gameNota.value);
            pScore.textContent = `★ ${isNaN(nota) ? '0.0' : nota.toFixed(1)}`;
        }

        if (pDev) {
            const devId = gameDev.value;
            pDev.textContent = desarrolladorasMap[devId] || 'Desarrolladora';
        }

        if (pImg && pPlaceholder) {
            const url = gameUrl.value.trim();
            if (url) {
                pImg.src = url;
                pImg.style.display = 'block';
                pPlaceholder.style.display = 'none';

                pImg.onerror = () => {
                    pImg.style.display = 'none';
                    pPlaceholder.style.display = 'flex';
                };
            } else {
                pImg.style.display = 'none';
                pPlaceholder.style.display = 'flex';
            }
        }

        if (pTags) {
            pTags.innerHTML = '';
            const tagsText = gameTags.value.trim();
            if (tagsText) {
                const tagsList = tagsText.split(',')
                    .map(t => t.trim())
                    .filter(t => t.length > 0);
                
                tagsList.forEach(tag => {
                    const span = document.createElement('span');
                    span.className = 'preview-tag-badge';
                    span.textContent = tag;
                    pTags.appendChild(span);
                });
            }
        }
    };

    if (gameNombre) gameNombre.addEventListener('input', window.actualizarPrevisualizacionJuego);
    if (gameDesc) gameDesc.addEventListener('input', window.actualizarPrevisualizacionJuego);
    if (gameUrl) gameUrl.addEventListener('input', window.actualizarPrevisualizacionJuego);
    if (gameNota) gameNota.addEventListener('input', window.actualizarPrevisualizacionJuego);
    if (gameDev) gameDev.addEventListener('change', window.actualizarPrevisualizacionJuego);
    if (gameTags) gameTags.addEventListener('input', window.actualizarPrevisualizacionJuego);
}

// Inicialización de validaciones y envíos de formulario
function inicializarFormularios() {
    const formDev = document.getElementById('form-desarrolladora');
    const formGame = document.getElementById('form-videojuego');

    if (formDev) {
        formDev.addEventListener('submit', (e) => {
            e.preventDefault();
            
            const nombre = document.getElementById('desarrolladora-nombre').value.trim();
            const pais = document.getElementById('desarrolladora-pais').value.trim();
            const urlImagen = document.getElementById('desarrolladora-urlImagen').value.trim();

            if (!nombre || !pais) {
                showToast('Por favor, completa los campos obligatorios de la desarrolladora.', 'error');
                return;
            }

            const bodyData = {
                nombre: nombre,
                pais: pais,
                urlImagen: urlImagen || 'placeholder'
            };

            const API_URL = 'http://localhost:8080/api/desarrolladoras';

            fetch(API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(bodyData)
            })
            .then(async response => {
                if (!response.ok) {
                    const text = await response.text();
                    throw new Error(text || 'Error al guardar la desarrolladora');
                }
                return response.json();
            })
            .then(data => {
                showToast(`¡Desarrolladora "${data.nombre}" registrada correctamente!`, 'success');
                formDev.reset();
                
                // Forzar evento input para actualizar la previsualización vacía
                const event = new Event('input');
                document.getElementById('desarrolladora-nombre').dispatchEvent(event);
                
                // Recargar el select de desarrolladoras
                cargarDesarrolladoras();
            })
            .catch(error => {
                console.error('Error al registrar desarrolladora:', error);
                showToast(error.message || 'Error al guardar desarrolladora en la API.', 'error');
            });
        });
    }

    if (formGame) {
        formGame.addEventListener('submit', (e) => {
            e.preventDefault();

            const nombre = document.getElementById('videojuego-nombre').value.trim();
            const descripcion = document.getElementById('videojuego-descripcion').value.trim();
            const urlImagen = document.getElementById('videojuego-urlImagen').value.trim();
            const notaMediaVal = document.getElementById('videojuego-notaMedia').value;
            const idDesarrolladoraVal = document.getElementById('videojuego-desarrolladora').value;
            const tagsVal = document.getElementById('videojuego-tags').value.trim();

            if (!nombre || !descripcion || !urlImagen || !notaMediaVal || !idDesarrolladoraVal) {
                showToast('Por favor, completa todos los campos obligatorios del videojuego.', 'error');
                return;
            }

            const notaMedia = parseFloat(notaMediaVal);
            if (isNaN(notaMedia) || notaMedia < 0 || notaMedia > 10) {
                showToast('La nota media debe ser un número entre 0 y 10.', 'error');
                return;
            }

            const idDesarrolladora = parseInt(idDesarrolladoraVal, 10);
            
            // Parsear tags a array de strings
            const tags = tagsVal ? tagsVal.split(',').map(t => t.trim()).filter(t => t.length > 0) : [];

            const bodyData = {
                nombre: nombre,
                descripcion: descripcion,
                urlImagen: urlImagen,
                notaMedia: notaMedia,
                idDesarrolladora: idDesarrolladora,
                tags: tags
            };

            const API_URL = 'http://localhost:8080/api/videojuegos';

            fetch(API_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(bodyData)
            })
            .then(async response => {
                if (!response.ok) {
                    const text = await response.text();
                    throw new Error(text || 'Error al guardar el videojuego');
                }
                return response.json();
            })
            .then(data => {
                showToast(`¡Videojuego "${data.nombre}" registrado correctamente!`, 'success');
                formGame.reset();
                
                // Forzar evento input para limpiar y actualizar previsualización
                if (window.actualizarPrevisualizacionJuego) {
                    window.actualizarPrevisualizacionJuego();
                }
            })
            .catch(error => {
                console.error('Error al registrar videojuego:', error);
                showToast(error.message || 'Error al guardar el videojuego en la API.', 'error');
            });
        });
    }
}

// Sistema Toast de Notificaciones
function showToast(message, type = 'success') {
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
    
    closeBtn.addEventListener('click', () => {
        toast.classList.add('toast-fade-out');
        setTimeout(() => toast.remove(), 300);
    });

    toast.appendChild(content);
    toast.appendChild(closeBtn);
    toastContainer.appendChild(toast);

    // Auto-remove despues de 4.5 segundos
    setTimeout(() => {
        if (toast.parentNode) {
            toast.classList.add('toast-fade-out');
            setTimeout(() => toast.remove(), 300);
        }
    }, 4500);
}
