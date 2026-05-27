document.addEventListener('DOMContentLoaded', () => {
    inicializarSesion();
    inicializarTabs();
    inicializarFormularios();
});

// Comprobar si hay sesión iniciada y mostrar la vista correspondiente
function inicializarSesion() {
    const authView = document.getElementById('auth-view');
    const listView = document.getElementById('list-view');
    const userSession = localStorage.getItem('currentUser');

    if (!userSession) {
        authView.classList.remove('hidden');
        listView.classList.add('hidden');
    } else {
        authView.classList.add('hidden');
        listView.classList.remove('hidden');
        
        try {
            const user = JSON.parse(userSession);
            
            // Configurar perfil en la cabecera
            const userNameSpan = document.getElementById('user-name-span');
            if (userNameSpan) userNameSpan.textContent = user.nombre;

            const userAvatar = document.getElementById('user-avatar');
            const avatarPlaceholder = document.getElementById('user-avatar-placeholder');
            
            if (userAvatar && avatarPlaceholder) {
                if (user.urlImagen && user.urlImagen !== 'placeholder') {
                    userAvatar.src = user.urlImagen;
                    userAvatar.style.display = 'block';
                    avatarPlaceholder.style.display = 'none';
                    
                    userAvatar.onerror = () => {
                        userAvatar.style.display = 'none';
                        avatarPlaceholder.style.display = 'flex';
                        avatarPlaceholder.textContent = user.nombre[0].toUpperCase();
                    };
                } else {
                    userAvatar.style.display = 'none';
                    avatarPlaceholder.style.display = 'flex';
                    avatarPlaceholder.textContent = user.nombre[0].toUpperCase();
                }
            }

            // Cargar colección del usuario
            cargarMiLista(user.miId);
        } catch (e) {
            console.error('Error parseando sesión local:', e);
            localStorage.removeItem('currentUser');
            location.reload();
        }
    }

    // Configurar Logout
    const btnLogout = document.getElementById('btn-logout');
    if (btnLogout) {
        btnLogout.addEventListener('click', () => {
            localStorage.removeItem('currentUser');
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
            const AUTH_URL = 'http://localhost:8080/api/auth/login';

            fetch(AUTH_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(bodyData)
            })
            .then(async response => {
                if (!response.ok) {
                    const text = await response.text();
                    throw new Error(text || 'Usuario o contraseña incorrectos');
                }
                return response.json();
            })
            .then(authData => {
                // Autenticado con éxito. Ahora traemos el ID y foto de perfil del usuario por su nombre.
                const USUARIO_URL = `http://localhost:8080/api/usuarios/nombre/${nombre}`;
                
                return fetch(USUARIO_URL)
                    .then(response => {
                        if (!response.ok) throw new Error('Error al cargar perfil de usuario');
                        return response.json();
                    })
                    .then(userProfile => {
                        // Guardar datos completos en localStorage
                        const sessionObj = {
                            token: authData.jwt,
                            miId: userProfile.miId,
                            nombre: userProfile.nombre,
                            urlImagen: userProfile.urlImagen
                        };
                        localStorage.setItem('currentUser', JSON.stringify(sessionObj));
                        
                        showToast(`¡Bienvenido de nuevo, ${userProfile.nombre}!`, 'success');
                        setTimeout(() => location.reload(), 1000);
                    });
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
                urlImagen: urlImagen || 'placeholder',
                rol: 'usuario' // Rol por defecto
            };

            const REGISTER_URL = 'http://localhost:8080/api/usuarios';

            fetch(REGISTER_URL, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(bodyData)
            })
            .then(async response => {
                if (!response.ok) {
                    const text = await response.text();
                    throw new Error(text || 'Error al crear la cuenta. ¿El usuario ya existe?');
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
function cargarMiLista(idUsuario) {
    const listLoader = document.getElementById('list-loader');
    const emptyListMessage = document.getElementById('empty-list-message');
    const gameGrid = document.getElementById('game-grid');

    if (!gameGrid) return;

    const LISTA_URL = `http://localhost:8080/api/lista/${idUsuario}`;

    fetch(LISTA_URL)
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

            // Cargar datos en paralelo de los videojuegos asociados a las entradas
            for (const entrada of listaEntradas) {
                await renderizarEntradaJuego(entrada, gameGrid);
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

// Renderizar una tarjeta individual consultando la API de videojuegos
async function renderizarEntradaJuego(entrada, container) {
    const JUEGO_URL = `http://localhost:8080/api/videojuegos/${entrada.idVideojuego}`;

    try {
        const response = await fetch(JUEGO_URL);
        if (!response.ok) throw new Error('Juego no encontrado');
        const game = await response.json();

        // Crear la tarjeta
        const card = document.createElement('div');
        card.className = 'game-list-card';
        card.id = `card-entry-${entrada.miId}`;

        card.innerHTML = `
            <div class="game-card-img-container">
                <img src="${game.urlImagen || 'https://gameboxd.s3.us-east-1.amazonaws.com/web/helldivers.jpg'}" alt="${game.nombre}" onerror="this.src='https://gameboxd.s3.us-east-1.amazonaws.com/web/helldivers.jpg'">
            </div>
            <div class="game-card-content">
                <div class="game-card-header">
                    <div class="game-title-area">
                        <h3>${game.nombre}</h3>
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
                  <button class="delete-entry-button" type="button" onclick="eliminarDeLista(${entrada.miId})">Eliminar de mi lista</button>
                </div>
            </div>
        `;

        container.appendChild(card);

        // Cargar nombre de la desarrolladora en segundo plano
        if (game.idDesarrolladora) {
            cargarNombreDesarrolladora(game.idDesarrolladora, entrada.miId);
        } else {
            const devLabel = document.getElementById(`dev-name-${entrada.miId}`);
            if (devLabel) devLabel.textContent = 'Estudio Desconocido';
        }

    } catch (e) {
        console.error(`Error cargando videojuego con id ${entrada.idVideojuego}:`, e);
        // Tarjeta de fallback si el juego fue eliminado pero queda la entrada
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
    }
}

// Carga el nombre de la desarrolladora desde la API y lo inyecta en la tarjeta
function cargarNombreDesarrolladora(idDev, miIdEntrada) {
    const DEV_URL = `http://localhost:8080/api/desarrolladoras/id/${idDev}`;
    const label = document.getElementById(`dev-name-${miIdEntrada}`);
    if (!label) return;

    fetch(DEV_URL)
        .then(response => {
            if (!response.ok) throw new Error('Estudio no encontrado');
            return response.json();
        })
        .then(devData => {
            label.textContent = devData.nombre;
        })
        .catch(error => {
            console.error('Error al cargar desarrolladora:', error);
            label.textContent = 'Estudio Desconocido';
        });
}

// Eliminar un videojuego de la colección personal del usuario (Llamada Global)
window.eliminarDeLista = function(idEntrada) {
    if (!confirm('¿Estás seguro de que quieres eliminar este videojuego de tu lista personal?')) return;

    const DELETE_URL = `http://localhost:8080/api/lista/${idEntrada}`;

    fetch(DELETE_URL, {
        method: 'DELETE'
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
