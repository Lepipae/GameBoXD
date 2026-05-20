package org.palomafp.apijuegos.api.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                        // Rutas públicas que nos pidió el usuario: Login y Registro
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                        // Rutas públicas solicitadas: GET de Videojuegos y Desarrolladoras
                        .requestMatchers(HttpMethod.GET, "/api/videojuegos", "/api/videojuegos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/desarrolladoras", "/api/desarrolladoras/**").permitAll()
                        // Para acceder a Swagger/OpenAPI si es necesario que estén públicas
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Cualquier otra petición no especificada arriba requerirá estar autenticado (con token válido)
                        .anyRequest().permitAll()
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
