package org.palomafp.apijuegos.api.controladores;

import org.palomafp.apijuegos.api.modelo.dto.AuthRequest;
import org.palomafp.apijuegos.api.modelo.dto.AuthResponse;
import org.palomafp.apijuegos.api.security.CustomUserDetailsService;
import org.palomafp.apijuegos.api.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    /**
     * Inyección por constructor: las tres dependencias quedan {@code final}.
     *
     * @param authenticationManager Gestor de autenticación de Spring Security.
     * @param userDetailsService    Servicio que carga el usuario desde MongoDB.
     * @param jwtUtil               Utilidad que emite el token.
     */
    public AuthController(AuthenticationManager authenticationManager,
                          CustomUserDetailsService userDetailsService,
                          JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
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
    public ResponseEntity<AuthResponse> createAuthenticationToken(@RequestBody AuthRequest authRequest) {
        // Intentamos autenticar al usuario usando el gestor de autenticación de Spring Security.
        // Esto llamará internamente a CustomUserDetailsService -> loadUserByUsername para verificar.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.nombre(), authRequest.contrasenia())
        );

        // Si la autenticación es exitosa, procedemos a generar el Token JWT.
        // Cargamos los datos del usuario (roles, permisos, etc si tuviéramos)
        UserDetails userDetails = userDetailsService.loadUserByUsername(authRequest.nombre());

        // Generamos la cadena del Token con nuestro JwtUtil y la devolvemos envuelta en un
        // objeto JSON. El cliente deberá guardarse este token y enviarlo en la cabecera
        // "Authorization: Bearer <token>".
        return ResponseEntity.ok(new AuthResponse(jwtUtil.generateToken(userDetails)));
    }
}
