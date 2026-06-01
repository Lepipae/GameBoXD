document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('login-form');    //Formulario de Inicio de Sesion
    const registerForm = document.getElementById('register-form');  // Formulario de Registro

    const loginError = document.getElementById('login-error'); // Mensajes de error/loggin
    const registerError = document.getElementById('register-error'); // Mensajes de error de registro
    const registerSuccess = document.getElementById('register-success'); // Mensajes de exito de registro

    // Script de inicio de sesion
    loginForm.addEventListener('submit', async (e) => {
        // Evita que la pagina se recargue
        e.preventDefault();
        // Limpia los mensajes de error
        loginError.textContent = '';

        // Obtiene los valores de los inputs
        const nombre = document.getElementById('login-username').value;
        const contrasenia = document.getElementById('login-password').value;

        // Comprueba si el usuario y contraseña no estan vacios
        if (!nombre || !contrasenia) {
            loginError.textContent = 'Usuario y contraseña son requeridos.';
            return;
        }

        try {
            // Peticion al servidor para iniciar sesion
            const response = await fetch('https://gameboxd.duckdns.org/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ nombre, contrasenia })
            });

            if (response.ok) {
                const data = await response.json();
                // Guardamos el JWT en localStorage para usarlo en otras peticiones
                localStorage.setItem('jwt_token', data.jwt);

                // También obtenemos el perfil completo para sincronizar con la lista y otros componentes
                try {
                    const profileResponse = await fetch(`https://gameboxd.duckdns.org/api/usuarios/nombre/${nombre}`);
                    if (profileResponse.ok) {
                        const userProfile = await profileResponse.json();
                        const sessionObj = {
                            token: data.jwt,
                            miId: userProfile.miId,
                            nombre: userProfile.nombre,
                            urlImagen: userProfile.urlImagen
                        };
                        // Guardamos el objeto de sesion en localStorage para usarlo en otras peticiones
                        localStorage.setItem('currentUser', JSON.stringify(sessionObj));
                    }
                } catch (profileError) {
                    console.error('Error fetching user profile during login:', profileError);
                }

                // Redirigir a la página principal u otra página
                window.location.href = 'index.html';
            } else {
                loginError.textContent = 'Credenciales inválidas. Inténtalo de nuevo.';
            }
        } catch (error) {
            console.error('Error in login:', error);
            loginError.textContent = 'Error de conexión con el servidor.';
        }
    });

    // Script de registro
    registerForm.addEventListener('submit', async (e) => {
        // Evita que la pagina se recargue
        e.preventDefault();
        // Limpia los mensajes de error
        registerError.textContent = '';
        // Limpia los mensajes de exito
        registerSuccess.textContent = '';

        // Obtiene los valores de los inputs
        const nombre = document.getElementById('reg-username').value;
        const contrasenia = document.getElementById('reg-password').value;

        // Comprueba si la contraseña tiene al menos 8 caracteres
        if (contrasenia.length < 8) {
            registerError.textContent = 'La contraseña debe tener al menos 8 caracteres.';
            return;
        }
        // Llamada a la api para registrar el usuario
        try {
            const response = await fetch('https://gameboxd.duckdns.org/api/usuarios', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                // El rol es usuarioNormal pq se me olvido que esto iba a ser una funcionalidad
                // a si que ahora todos iguales coño :b
                body: JSON.stringify({
                    nombre: nombre,
                    contrasenia: contrasenia,
                    rol: 'usuarioNormal',
                    miId: 0
                })
            });

            if (response.ok) {
                registerSuccess.textContent = 'Usuario creado con éxito. Ahora puedes iniciar sesión en la columna izquierda.';
                registerForm.reset();
            } else {
                registerError.textContent = 'Error al crear el usuario. Puede que el nombre ya esté en uso o los datos sean inválidos.';
            }
        } catch (error) {
            console.error('Error in registration:', error);
            registerError.textContent = 'Error de conexión con el servidor.';
        }
    });
});
