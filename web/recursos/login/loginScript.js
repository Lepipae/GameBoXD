document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('login-form');
    const registerForm = document.getElementById('register-form');
    
    const loginError = document.getElementById('login-error');
    const registerError = document.getElementById('register-error');
    const registerSuccess = document.getElementById('register-success');

    // Login Handle
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        loginError.textContent = '';
        
        const nombre = document.getElementById('login-username').value;
        const contrasenia = document.getElementById('login-password').value;
        
        try {
            const response = await fetch('http://34.196.250.38:8080/api/auth/login', {
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

    // Register Handle
    registerForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        registerError.textContent = '';
        registerSuccess.textContent = '';
        
        const nombre = document.getElementById('reg-username').value;
        const contrasenia = document.getElementById('reg-password').value;

        if (contrasenia.length < 8) {
            registerError.textContent = 'La contraseña debe tener al menos 8 caracteres.';
            return;
        }

        try {
            const response = await fetch('http://34.196.250.38:8080/api/usuarios', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                // El rol es usuarioNormal por defecto para nuevos registros
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
