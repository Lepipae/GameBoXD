package org.palomafp.apijuegos.api.controladores;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.palomafp.apijuegos.api.modelo.Usuario;
import org.palomafp.apijuegos.api.modelo.enums.Rol;
import org.palomafp.apijuegos.api.repositories.UsuarioRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del endpoint de login.
 *
 * <p>{@code AuthRequest} y {@code AuthResponse} son records: no tienen constructor
 * vacío ni setters, y Jackson los reconstruye usando el constructor canónico. Eso
 * funciona, pero si algún día se les añade un campo, el nombre del parámetro pasa a
 * ser la clave del JSON, y un error ahí solo se detecta si alguien hace login de
 * verdad. Este test es el que vigila ese punto.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String CONTRASENA = "clave12345";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UsuarioRepo usuarioRepo;

    @BeforeEach
    void registrarUsuario() {
        // La contraseña se guarda cifrada, que es como la deja UsuarioService.
        var usuario = new Usuario("jugador", "https://x.com/a.png",
                passwordEncoder.encode(CONTRASENA), Rol.usuarioNormal, 1);
        when(usuarioRepo.findByNombre("jugador")).thenReturn(usuario);
    }

    @Test
    @DisplayName("Un login correcto devuelve 200 y un token JWT")
    void loginCorrectoDevuelveToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "jugador", "contrasenia": "%s"}""".formatted(CONTRASENA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").isNotEmpty());
    }

    @Test
    @DisplayName("Una contrasena incorrecta devuelve 401, no 500")
    void contrasenaIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "jugador", "contrasenia": "equivocada"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un usuario inexistente devuelve 401")
    void usuarioInexistenteDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre": "fantasma", "contrasenia": "%s"}""".formatted(CONTRASENA)))
                .andExpect(status().isUnauthorized());
    }
}
