package org.palomafp.apijuegos.api.controladores;

import org.palomafp.apijuegos.api.modelo.dto.AuthRequest;
import org.palomafp.apijuegos.api.modelo.dto.AuthResponse;
import org.palomafp.apijuegos.api.security.CustomUserDetailsService;
import org.palomafp.apijuegos.api.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para gestionar el inicio de sesión de los usuarios.
 * Recibe el usuario y contraseña, lo valida con la base de datos
 * y devuelve un token JWT en caso de ser correcto.
 * @author Andrés López
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Endpoint público para iniciar sesión en la API.
     * @param authRequest Objeto con nombre de usuario y contraseña proporcionados en formato JSON.
     * @return Una respuesta HTTP (200 OK con el token JWT si acierta, o error 400+ si falla).
     * @throws Exception Si las credenciales son incorrectas lanza BadCredentialsException.
     */
    @PostMapping("/login")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthRequest authRequest) {

        try {
            // Intentamos autenticar al usuario usando el gestor de autenticación de Spring Security.
            // Esto llamará internamente a CustomUserDetailsService -> loadUserByUsername para verificar.
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getNombre(), authRequest.getContrasenia())
            );
        } catch (BadCredentialsException e) {
            // Se relanza como BadCredentialsException (no como Exception genérica) para que
            // ManejadorErrores lo traduzca a un 401 Unauthorized. Antes acababa en un 500.
            throw e;
        }

        // Si la autenticación es exitosa, procedemos a generar el Token JWT
        
        // Cargamos los datos del usuario (roles, permisos, etc si tuviéramos)
        final UserDetails userDetails = userDetailsService
                .loadUserByUsername(authRequest.getNombre());

        // Generamos la cadena del Token con nuestro JwtUtil
        final String jwt = jwtUtil.generateToken(userDetails);

        // Devolvemos el token envuelto en un objeto (JSON) de respuesta al cliente.
        // El cliente (Postman o frontend) deberá guardar este token y enviarlo en el header "Authorization: Bearer <token>"
        return ResponseEntity.ok(new AuthResponse(jwt));
    }
}
