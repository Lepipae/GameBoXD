package org.palomafp.apijuegos.api.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Clase principal de configuración para Spring Security.
 * Aquí decidimos qué rutas son públicas, qué rutas requieren estar logeados,
 * el sistema de encriptación de contraseñas y cómo manejar la autenticación (stateless/sin sesión).
 * @author Andrés López
 */
@Configuration
@EnableWebSecurity
// Imprescindible: sin esta anotacion los @PreAuthorize de los controladores se
// ignoran silenciosamente y la autorizacion por rol no se aplica.
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtRequestFilter jwtRequestFilter;

    /**
     * Define el tipo de encriptación a usar para las contraseñas (BCrypt).
     * @return Bean de PasswordEncoder para inyectar en otras partes.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Define el AuthenticationManager, que es la interfaz central de Spring para autenticar usuarios.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Configuración central de las reglas de seguridad.
     * @param http Objeto HttpSecurity para configurar la seguridad a nivel web.
     * @return Cadena de filtros de seguridad.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Habilitamos CORS y deshabilitamos CSRF porque nuestra API es Stateless usando JWT
        http.cors(org.springframework.security.config.Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
                // Configuramos las reglas de autorización por rutas
                .authorizeHttpRequests(auth -> auth
                        // ---------------------------------------------------------
                        // PÚBLICAS: solo lectura del catálogo y el registro/login
                        // ---------------------------------------------------------
                        // El preflight de CORS debe pasar siempre: el frontend vive en
                        // otro dominio (GitHub Pages) y lo dispara en cada POST/DELETE.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Rutas públicas que nos pidió el usuario: Login y Registro
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                        // Rutas públicas solicitadas: GET de Videojuegos y Desarrolladoras
                        .requestMatchers(HttpMethod.GET, "/api/videojuegos", "/api/videojuegos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/desarrolladoras", "/api/desarrolladoras/**").permitAll()
                        // Ficha de un juego: necesita saber quantas listas lo incluyen
                        .requestMatchers(HttpMethod.GET, "/api/lista/juego/**").permitAll()
                        // Perfiles públicos de usuario por id o por nombre. Ya no exponen
                        // credenciales (contrasenia es WRITE_ONLY), asi que un visitante
                        // anonimo puede ver quien ha escrito una reseña.
                        // OJO: seelistan a proposito, sin "/**", para no abrir
                        // GET /api/usuarios (listado completo), que exige administrador.
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/*", "/api/usuarios/nombre/*").permitAll()
                        // Para acceder a Swagger/OpenAPI si es necesario que estén públicas
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // ---------------------------------------------------------
                        // TODO LO DEMÁS REQUIERE TOKEN
                        // ---------------------------------------------------------
                        // Incluye las escrituras (POST de juegos y desarrolladoras) y
                        // toda la gestión de listas. Los endpoints destructivos y el
                        // listado de usuarios se restringen aún más con @PreAuthorize
                        // en cada método (@EnableMethodSecurity).
                        .anyRequest().authenticated()
                )
                // Indicamos que queremos manejar sesiones de forma Stateless (Sin Guardar Sesión en el servidor)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        // Añadimos nuestro filtro JWT justo antes del filtro de usuario/contraseña estándar de Spring
        // De esta forma interceptamos el JWT y validamos al usuario antes de comprobar contraseñas.
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
