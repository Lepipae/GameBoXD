// Acceso al gestor centralizado de imágenes definido en api.js
import { showToast, hideToast } from '../ui.js';
import {
    login,
    logout,
    getToken,
    getCurrentUser,
    isAuthenticated,
    clearSession,
} from '../auth.js';

const { aplicarImagen } = window.GameBoXDImagenes;

/**
 * Caché de juegos por ID (idVideojuego → datos del juego).
 * Garantiza que cada juego se pida a la API a lo sumo una vez, eliminando
 * el problema N+1 al renderizar varias entradas que referencean el mismo juego.
 */
export const cacheJuegos = new Map();

/**
 * Caché de desarrolladoras por ID (idDesarrolladora → datos de la desarrolladora).
 * Las desarrolladoras repetidas no se vuelven a fetch.
 */
export const cacheDesarrolladoras = new Map();

/**
 * Petición GET a la API que devuelve el cuerpo parseado como JSON.
 * Lanza si la respuesta no es OK.
 */
async function fetchJson(url) {
    const response = await fetch(url);
    if (!response.ok) throw new Error(`Error ${response.status} al obtener ${url}`);
    return response.json();
}

/**
 * Carga en lote los recursos identificados por `ids`, guardando cada uno en
 * `cache` bajo su id.
 *
 * - Los ids repetidos se piden una sola vez (dedup mediante Set).
 * - Las peticiones se disparan en paralelo con Promise.all, no de forma
 *   secuencial una por entrada.
 * - Los ids ya presentes en caché no se vuelven a pedir.
 * - Un fallo individual no cancela el resto: se registra el error, se guarda
 *   `null` para ese id y el lote sigue adelante (una entrada fallida no
 *   rompe la lista completa).
 */
export async function cargarLotePorId(ids, urlParaId, cache) {
    const idsUnicos = [...new Set(ids.filter((id) => id != null))];
    const pendientes = idsUnicos.filter((id) => !cache.has(id));

    await Promise.all(
        pendientes.map(async (id) => {
            try {
                cache.set(id, await fetchJson(urlParaId(id)));
            } catch (error) {
                console.error(`Error al cargar ${urlParaId(id)}:`, error);
                cache.set(id, null);
            }
        })
    );
}

// Al cargar el dom se ejecutan los scripts
document.addEventListener('DOMContentLoaded', () => {
    inicializarSesion();
    inicializarTabs();
    inicializarFormularios();
    inicializarModal();
});

// Comprobar si hay sesión iniciada y mostrar la vista correspondiente
function inicializarSesion() {
    // Obtenemos los elementos del dom
    const authView = document.getElementById('auth-view');
    const listView = document.getElementById('list-view');
    const user = getCurrentUser();
    const token = getToken();

    // Comprobamos si hay sesión iniciada
    if (!user || !token) {
        authView.classList.remove('hidden');
        listView.classList.add('hidden');
    } else { // Si hay sesion iniciada
        authView.classList.add('hidden'); // Ocultamos la vista de auth
        listView.classList.remove('hidden'); // Mostramos la vista de lista

        try {
            // Configurar perfil en la cabecera
            const userNameSpan = document.getElementById('user-name-span');
            if (userNameSpan) userNameSpan.textContent = user.nombre;

            const userAvatar = document.getElementById('user-avatar');
            const avatarPlaceholder = document.getElementById('user-avatar-placeholder');

            if (userAvatar && avatarPlaceholder) {
                userAvatar.style.display = 'none';
                avatarPlaceholder.style.display = 'flex';
                avatarPlaceholder.textContent = user.nombre[0].toUpperCase();
            }

            // Cargar colección del usuario
            cargarMiLista(user.miId);
        } catch (e) {
            console.error('Error parseando sesión local:', e);
            clearSession();
            location.reload();
        }
    }

    // Configurar Logout
    const btnLogout = document.getElementById('btn-logout');
    if (btnLogout) {
        btnLogout.addEventListener('click', () => {
            logout();
            showToast('¡Sesión cerrada correctamente!', 'success');
            setTimeout(() => location.reload(), 800);
        });
    }
}

// Configurar pestañas (tabs) de login/registro
function inicializarTabs() {
    const tabLogin = document.getElementById('tab-login');
    const tabRegister = document.getElementById('tab-register');
    const formLogin = document.getElementById('form-login');
    const formRegister = document.getElementById('form-register');

    if (tabLogin && tabRegister && formLogin && formRegister) {
        tabLogin.addEventListener('click', () => {
            tabLogin.classList.add('active');
            tabRegister.classList.remove('active');
            formLogin.classList.remove('hidden');
            formRegister.classList.add('hidden');
        });

        tabRegister.addEventListener('click', () => {
            tabRegister.classList.add('active');
            tabLogin.classList.remove('active');
            formRegister.classList.remove('hidden');
            formLogin.classList.add('hidden');
        });
    }
}

// Controlar el registro y logueo
function inicializarFormularios() {
    const formLogin = document.getElementById('form-login');
    const formRegister = document.getElementById('form-register');

    // Login
    if (formLogin) {
        formLogin.addEventListener('submit', (e) => {
            e.preventDefault();

            const nombre = document.getElementById('login-username').value.trim();
            const contrasenia = document.getElementById('login-password').value;

            if (!nombre || !contrasenia) {
                showToast('Por favor, introduce tu usuario y contraseña.', 'error');
                return;
            }

            const bodyData = { nombre, contrasenia };
            const AUTH_URL = `${GameBoXDApi.BASE_URL}/auth/login`;

            login(nombre, contrasenia)
                .then(session => {
                    if (!session) {
                        throw new Error('Usuario o contraseña incorrectos');
                    }
                    const userName = session.user?.nombre || nombre;
                    showToast(`¡Bienvenido de nuevo, ${userName}!`, 'success');
                    setTimeout(() => {
                        const redirect = window.GameBoXDRedirect;
                        const returnUrl = redirect
                            ? (redirect.getReturnUrl() ||
                               redirect.getLoginRedirectUrl() ||
                               'index.html')
                            : 'index.html';
                        if (redirect) {
                            redirect.clearReturnUrl();
                        }
                        window.location.href = returnUrl;
                    }, 1000);
                })
                .catch(error => {
                    console.error('Error en login:', error);
                    showToast(error.message || 'Error al iniciar sesión. Verifica tus credenciales.', 'error');
                });
        });
    }

    // Registro
    if (formRegister) {
        formRegister.addEventListener('submit', (e) => {
            e.preventDefault();

            const nombre = document.getElementById('register-username').value.trim();
            const contrasenia = document.getElementById('register-password').value;
            const urlImagen = document.getElementById('register-avatar').value.trim();

            if (!nombre || !contrasenia) {
                showToast('Por favor, rellena todos los campos obligatorios.', 'error');
                return;
            }

            if (contrasenia.length < 8) {
                showToast('La contraseña debe tener al menos 8 caracteres.', 'error');
                return;
            }

            const bodyData = {
                nombre: nombre,
                contrasenia: contrasenia,
                // Sin avatar se envia null: el backend ya no guarda el texto "placeholder".
                urlImagen: urlImagen || null,
                // El enum Rol solo admite 'administrador' y 'usuarioNormal'.
                // Antes se mandaba 'usuario', que la API rechazaba con un 400.
                rol: 'usuarioNormal',
                // Usuario.miId es un int primitivo: si no se envia, el backend no
                // puede deserializar el cuerpo y responde 400.
                miId: 0
            };

            const REGISTER_URL = `${GameBoXDApi.BASE_URL}/usuarios`;

            fetch(REGISTER_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(bodyData)
            })
                .then(async response => {
                    if (!response.ok) {
                        throw new Error(await GameBoXDErrores.mensajeError(response, 'Error al crear la cuenta. ¿El usuario ya existe?'));
                    }
                    return response.json();
                })
                .then(data => {
                    showToast(`¡Cuenta registrada correctamente para "${data.nombre}"! Ya puedes iniciar sesión.`, 'success');
                    formRegister.reset();

                    // Conmutar pestaña a login
                    const tabLogin = document.getElementById('tab-login');
                    if (tabLogin) tabLogin.click();
                })
                .catch(error => {
                    console.error('Error en registro:', error);
                    showToast(error.message || 'Error al crear la cuenta de usuario.', 'error');
                });
        });
    }
}

// Cargar la lista del usuario y pintar las tarjetas
export function cargarMiLista(idUsuario) {
    const listLoader = document.getElementById('list-loader');
    const emptyListMessage = document.getElementById('empty-list-message');
    const gameGrid = document.getElementById('game-grid');

    if (!gameGrid) return;

    const LISTA_URL = `${GameBoXDApi.BASE_URL}/lista/${idUsuario}`;

    // La API restringe cada lista a su propietario, asi que hace falta el token.
    const token = getToken();
    const cabeceras = {};
    if (token) {
        cabeceras['Authorization'] = `Bearer ${token}`;
    }

    return fetch(LISTA_URL, { headers: cabeceras })
        .then(response => {
            if (!response.ok) throw new Error('Error al conectar con la base de datos de tu lista');
            return response.json();
        })
        .then(async listaEntradas => {
            if (listLoader) listLoader.style.display = 'none';

            if (listaEntradas.length === 0) {
                if (emptyListMessage) emptyListMessage.classList.remove('hidden');
                gameGrid.innerHTML = '';
                return;
            }

            if (emptyListMessage) emptyListMessage.classList.add('hidden');
            gameGrid.innerHTML = ''; // Limpiar grid

            // Inicializar el diccionario global de entradas para la edición
            window.miListaEntradas = {};

            // --- Lote 1: cada juego ÚNICO se fetch una sola vez, en paralelo (no una petición por entrada) ---
            const idsJuego = listaEntradas.map((entrada) => entrada.idVideojuego);
            await cargarLotePorId(
                idsJuego,
                (id) => `${GameBoXDApi.BASE_URL}/videojuegos/${id}`,
                cacheJuegos
            );

            // Renderizar cada entrada leyendo los datos del juego del caché.
            const idsDesarrolladora = [];
            for (const entrada of listaEntradas) {
                try {
                    const juego = cacheJuegos.get(entrada.idVideojuego);
                    const idDesarrolladora = renderizarEntradaJuego(entrada, juego, gameGrid);
                    if (idDesarrolladora) {
                        idsDesarrolladora.push(idDesarrolladora);
                    }
                } catch (error) {
                    console.error(`Error renderizando la entrada ${entrada.miId}:`, error);
                }
            }

            // --- Lote 2: cada desarrolladora ÚNICA se fetch una sola vez, en paralelo (dedup por caché) ---
            await cargarLotePorId(
                idsDesarrolladora,
                (id) => `${GameBoXDApi.BASE_URL}/desarrolladoras/id/${id}`,
                cacheDesarrolladoras
            );

            // Inyectar los nombres de desarrolladora ya resueltos en sus tarjetas.
            for (const entrada of listaEntradas) {
                const juego = cacheJuegos.get(entrada.idVideojuego);
                const label = document.getElementById(`dev-name-${entrada.miId}`);
                if (!label) continue;
                if (!juego || !juego.idDesarrolladora) {
                    label.textContent = 'Estudio Desconocido';
                    continue;
                }
                const desarrolladora = cacheDesarrolladoras.get(juego.idDesarrolladora);
                label.textContent = desarrolladora ? desarrolladora.nombre : 'Estudio Desconocido';
            }
        })
        .catch(error => {
            console.error('Error al cargar colección:', error);
            if (listLoader) listLoader.style.display = 'none';
            showToast('Error al conectar con la API de tu colección.', 'error');
        });
}

// Formatear texto del Enum de Estado
function formatEstado(estado) {
    switch (estado) {
        case 'noEmpezado': return 'No Empezado';
        case 'empezado': return 'En Progreso';
        case 'terminado': return 'Terminado';
        default: return estado;
    }
}

// Renderizar una tarjeta individual a partir de los datos del juego ya cargados
// en el caché por cargarMiLista (no hay fetch por entrada aquí).
export function renderizarEntradaJuego(entrada, juego, container) {
    // Juego no disponible: tarjeta de fallback sin romper el resto de la lista
    if (!juego) {
        const fallbackCard = document.createElement('div');
        fallbackCard.className = 'game-list-card';
        fallbackCard.id = `card-entry-${entrada.miId}`;
        fallbackCard.innerHTML = `
            <div class="game-card-content">
                <div class="game-card-header">
                    <div class="game-title-area">
                        <h3>Videojuego No Disponible (ID: ${entrada.idVideojuego})</h3>
                        <span class="game-developer">El juego fue removido de la base de datos general.</span>
                    </div>
                </div>
                <div class="game-card-actions">
                  <button class="delete-entry-button" type="button" onclick="eliminarDeLista(${entrada.miId})">Limpiar Entrada Huérfana</button>
                </div>
            </div>
        `;
        container.appendChild(fallbackCard);
        return undefined;
    }

    const idDesarrolladora = juego.idDesarrolladora;

    // Guardar en el diccionario global para la edición
    window.miListaEntradas = window.miListaEntradas || {};
    window.miListaEntradas[entrada.miId] = {
        entrada: entrada,
        nombreJuego: juego.nombre
    };

    // Crear la tarjeta
    const card = document.createElement('div');
    card.className = 'game-list-card';
    card.id = `card-entry-${entrada.miId}`;

    card.innerHTML = `
        <div class="game-card-img-container">
            <img alt="${juego.nombre}">
        </div>
        <div class="game-card-content">
            <div class="game-card-header">
                <div class="game-title-area">
                    <h3>${juego.nombre}</h3>
                    <span class="game-developer" id="dev-name-${entrada.miId}">Cargando desarrolladora...</span>
                </div>
                <span class="status-badge status-${entrada.estado}">${formatEstado(entrada.estado)}</span>
            </div>
            
            <div class="user-log-stats">
              <div class="stat-item">
                <span class="stat-label">Mi Nota</span>
                <span class="stat-value score">★ ${entrada.nota.toFixed(1)}/10</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">Horas de Juego</span>
                <span class="stat-value">${entrada.horasJugadas} h</span>
              </div>
            </div>
            
            ${entrada.resenya ? `
            <div class="game-review-box">
              <span class="stat-label">Mi Reseña</span>
              <p class="review-text">"${entrada.resenya}"</p>
            </div>
            ` : ''}
            
            <div class="game-card-actions">
              <button class="edit-entry-button" type="button" onclick="abrirModalEdicion(${entrada.miId})">Editar</button>
              <button class="delete-entry-button" type="button" onclick="eliminarDeLista(${entrada.miId})">Eliminar de mi lista</button>
            </div>
        </div>
    `;

    // La portada se asigna con el gestor centralizado, que valida la URL y
    // recurre a la imagen local si el enlace remoto ha muerto.
    aplicarImagen(card.querySelector('.game-card-img-container img'), juego.urlImagen);

    container.appendChild(card);

    // La desarrolladora se rellena desde el caché tras cargar el lote de desarrolladoras.
    return idDesarrolladora;
}

// Eliminar un videojuego de la colección personal del usuario (Llamada Global)
window.eliminarDeLista = function (idEntrada) {
    if (!confirm('¿Estás seguro de que quieres eliminar este videojuego de tu lista personal?')) return;

    const DELETE_URL = `${GameBoXDApi.BASE_URL}/lista/${idEntrada}`;

    // La API solo deja borrar entradas de la lista propia, asi que hace falta el token.
    const token = getToken();
    const cabeceras = {};
    if (token) {
        cabeceras['Authorization'] = `Bearer ${token}`;
    }

    fetch(DELETE_URL, {
        method: 'DELETE',
        headers: cabeceras
    })
        .then(response => {
            if (!response.ok) throw new Error('No se pudo eliminar el videojuego de tu lista.');

            showToast('¡Videojuego eliminado con éxito de tu colección!', 'success');

            // Animación suave de desvanecimiento y remoción del DOM
            const card = document.getElementById(`card-entry-${idEntrada}`);
            if (card) {
                card.style.opacity = '0';
                card.style.transform = 'translateY(15px)';
                card.style.transition = 'all 0.3s ease';

                setTimeout(() => {
                    card.remove();

                    // Si ya no quedan tarjetas en el grid, mostrar mensaje de lista vacía
                    const gameGrid = document.getElementById('game-grid');
                    const emptyListMessage = document.getElementById('empty-list-message');
                    if (gameGrid && gameGrid.children.length === 0 && emptyListMessage) {
                        emptyListMessage.classList.remove('hidden');
                    }
                }, 300);
            }
        })
        .catch(error => {
            console.error('Error al eliminar entrada:', error);
            showToast(error.message || 'Error al eliminar el videojuego.', 'error');
        });
};

// Las notificaciones usan el showToast compartido importado de ../ui.js.

// Abrir modal de edición con los datos precargados de la entrada
window.abrirModalEdicion = function (miId) {
    const entryData = window.miListaEntradas[miId];
    if (!entryData) return;

    const entrada = entryData.entrada;
    const nombreJuego = entryData.nombreJuego;

    document.getElementById('modal-game-title').textContent = `Editar: ${nombreJuego}`;
    document.getElementById('edit-entry-id').value = entrada.id || '';
    document.getElementById('edit-entry-miid').value = entrada.miId;
    document.getElementById('edit-entry-gameid').value = entrada.idVideojuego;
    document.getElementById('edit-entry-userid').value = entrada.idUsuario;

    // Seleccionar el radio correcto del estado
    const statusRadios = document.getElementsByName('edit-status');
    for (const radio of statusRadios) {
        if (radio.value === entrada.estado) {
            radio.checked = true;
            break;
        }
    }

    // Nota / Slider
    const ratingInput = document.getElementById('edit-rating-input');
    const ratingValue = document.getElementById('rating-slider-value');
    ratingInput.value = entrada.nota;
    ratingValue.textContent = `★ ${entrada.nota.toFixed(1)}/10`;

    // Horas
    document.getElementById('edit-hours-input').value = entrada.horasJugadas;

    // Reseña
    document.getElementById('edit-review-input').value = entrada.resenya || '';

    // Mostrar modal
    const modal = document.getElementById('edit-modal');
    modal.classList.remove('hidden');
};

// Inicializar eventos del modal de edición
function inicializarModal() {
    const modal = document.getElementById('edit-modal');
    const form = document.getElementById('edit-game-form');
    const btnCancel = document.getElementById('btn-cancel-edit');
    const btnClose = document.getElementById('close-modal-btn');
    const ratingInput = document.getElementById('edit-rating-input');
    const ratingValue = document.getElementById('rating-slider-value');

    if (!modal || !form) return;

    // Actualizar valor visual del slider de forma dinámica
    ratingInput.addEventListener('input', (e) => {
        const val = parseFloat(e.target.value);
        ratingValue.textContent = `★ ${val.toFixed(1)}/10`;
    });

    const cerrarModal = () => {
        modal.classList.add('hidden');
    };

    btnCancel.addEventListener('click', cerrarModal);
    btnClose.addEventListener('click', cerrarModal);

    // Cerrar si hace clic fuera del contenido del modal
    modal.addEventListener('click', (e) => {
        if (e.target === modal) {
            cerrarModal();
        }
    });

    // Guardar cambios al enviar el formulario
    form.addEventListener('submit', (e) => {
        e.preventDefault();

        const id = document.getElementById('edit-entry-id').value;
        const miId = parseInt(document.getElementById('edit-entry-miid').value);
        const idVideojuego = parseInt(document.getElementById('edit-entry-gameid').value);
        const idUsuario = parseInt(document.getElementById('edit-entry-userid').value);

        let estado = 'noEmpezado';
        const statusRadios = document.getElementsByName('edit-status');
        for (const radio of statusRadios) {
            if (radio.checked) {
                estado = radio.value;
                break;
            }
        }

        const nota = parseFloat(ratingInput.value);
        const horasJugadas = parseInt(document.getElementById('edit-hours-input').value) || 0;
        const resenya = document.getElementById('edit-review-input').value.trim();

        // Crear objeto de entrada actualizada
        const entradaActualizada = {
            id: id || null,
            miId: miId,
            horasJugadas: horasJugadas,
            nota: nota,
            resenya: resenya,
            estado: estado,
            idVideojuego: idVideojuego,
            idUsuario: idUsuario
        };

        const token = getToken();
        const headers = {
            'Content-Type': 'application/json'
        };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        fetch(`${GameBoXDApi.BASE_URL}/lista`, {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(entradaActualizada)
        })
            .then(async response => {
                if (!response.ok) {
                    throw new Error(await GameBoXDErrores.mensajeError(response, 'Error al actualizar la entrada'));
                }
                return response.json();
            })
            .then(data => {
                showToast('¡Entrada de juego actualizada con éxito!', 'success');
                cerrarModal();
                // Volver a cargar la colección del usuario para reflejar los cambios
                cargarMiLista(idUsuario);
            })
            .catch(error => {
                console.error('Error al actualizar entrada:', error);
                showToast(error.message || 'Error al guardar los cambios.', 'error');
            });
    });
}
