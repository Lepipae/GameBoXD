package org.palomafp.apijuegos.api.controladores;

import org.palomafp.apijuegos.api.modelo.dto.AuthRequest;
import org.palomafp.apijuegos.api.modelo.dto.AuthResponse;
import org.palomafp.apijuegos.api.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para gestionar el inicio de sesión de los usuarios.
 * Recibe el usuario y contraseña, lo valida con la base de datos
 * y devuelve un token JWT en caso de ser correcto.
 * @author Andrés López
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /**
     * Inyección por constructor: ambas dependencias quedan {@code final}.
     *
     * @param authenticationManager Gestor de autenticación de Spring Security.
     * @param jwtUtil               Utilidad que emite el token.
     */
    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Endpoint público para iniciar sesión en la API.
     *
     * <p>Si las credenciales no cuadran deja escapar la
     * {@code BadCredentialsException} del gestor de autenticación: es el tipo que
     * {@code ManejadorErrores} traduce a un 401. Antes se relanzaba dentro de un
     * {@code try/catch} que solo la reenviaba, y si el fallo era otro tipo de
     * excepción acababa en un 500.</p>
     *
     * @param authRequest Objeto con nombre de usuario y contraseña proporcionados en formato JSON.
     * @return 200 OK con el token JWT si acierta.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> createAuthenticationToken(@Valid @RequestBody AuthRequest authRequest) {
        // Intentamos autenticar al usuario usando el gestor de autenticación de Spring Security.
        // Esto llamará internamente a CustomUserDetailsService -> loadUserByUsername para verificar.
        var autenticacion = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.nombre(), authRequest.contrasenia())
        );

        // La autenticación ya trae el UserDetails cargado: reutilizarlo evita una
        // segunda consulta a MongoDB por login.
        UserDetails userDetails = (UserDetails) autenticacion.getPrincipal();

        // Generamos la cadena del Token con nuestro JwtUtil y la devolvemos envuelta en un
        // objeto JSON. El cliente deberá guardarse este token y enviarlo en la cabecera
        // "Authorization: Bearer <token>".
        return ResponseEntity.ok(new AuthResponse(jwtUtil.generateToken(userDetails)));
    }
}
