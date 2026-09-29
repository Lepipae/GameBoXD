package org.palomafp.apijuegos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.palomafp.apijuegos.api.modelo.Usuario;
import org.palomafp.apijuegos.api.modelo.enums.Rol;
import org.palomafp.apijuegos.api.repositories.UsuarioRepo;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.palomafp.apijuegos.api.repositories.VideojuegoRepo;
import org.palomafp.apijuegos.api.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Comprueba que los errores de negocio devuelven el codigo HTTP correcto.
 *
 * <p>Antes de crear {@link ManejadorErrores}, todo {@code IllegalArgumentException}
 * acababa en un 500 indistinguible de una caida real del servidor.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ManejadorErroresTest {

    @Autowired
    private MockMvc mockMvc;

    /** Spring Boot 4 ya no registra un bean ObjectMapper, asi que usamos uno local. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private VideojuegoRepo videojuegoRepo;

    @MockitoBean
    private UsuarioRepo usuarioRepo;

    @Autowired
    private JwtUtil jwtUtil;

    /** Token JWT de un usuario normal, necesario para las rutas de escritura. */
    private String tokenNormal() {
        // El filtro JWT resuelve el token contra el repositorio, asi que el
        // usuario del token tiene que existir ahi.
        Usuario escritor = new Usuario("escritor", "https://x.com/a.png", "clave12345", Rol.usuarioNormal, 1);
        when(usuarioRepo.findByNombre("escritor")).thenReturn(escritor);

        UserDetails detalles = new User("escritor", "clave12345",
                List.of(new SimpleGrantedAuthority("ROLE_usuarioNormal")));
        return jwtUtil.generateToken(detalles);
    }

    @Test
    @DisplayName("Un recurso inexistente devuelve 404 con mensaje legible")
    void recursoInexistenteDevuelve404() throws Exception {
        when(videojuegoRepo.findByMiId(anyLong())).thenReturn(null);

        String cuerpo = mockMvc.perform(get("/api/videojuegos/77"))
                .andExpect(status -> assertEquals(404, status.getResponse().getStatus()))
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("No existe ningun videojuego"));
        assertTrue(cuerpo.contains("\"status\":404"));
    }

    @Test
    @DisplayName("Un nombre repetido devuelve 409 Conflict, no 500")
    void nombreRepetidoDevuelve409() throws Exception {
        when(videojuegoRepo.findByNombre("Minecraft")).thenReturn(new org.palomafp.apijuegos.api.modelo.Videojuego());

        String cuerpo = mockMvc.perform(post("/api/videojuegos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tokenNormal())
                        .content("{\"nombre\":\"Minecraft\",\"descripcion\":\"d\",\"urlImagen\":\"https://x.com/a.png\",\"notaMedia\":8,\"idDesarrolladora\":1,\"tags\":[\"RPG\"]}"))
                .andExpect(status -> assertEquals(409, status.getResponse().getStatus()))
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("Ya existe un videojuego"));
    }

    @Test
    @DisplayName("Un dato invalido del modelo devuelve 400, no 500")
    void datoInvalidoDevuelve400() throws Exception {
        // El setter de Videojuego lanza IllegalArgumentException si el nombre va vacio.
        // Se mandan todos los campos primitivos (notaMedia, idDesarrolladora) porque
        // si falta alguno Jackson falla antes, al no poder mapear null a un primitivo.
        String cuerpo = mockMvc.perform(post("/api/videojuegos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tokenNormal())
                        .content("{\"nombre\":\"\",\"descripcion\":\"d\",\"urlImagen\":\"https://x.com/a.png\","
                                + "\"notaMedia\":7.0,\"idDesarrolladora\":1,\"tags\":[\"RPG\"]}"))
                .andExpect(status -> assertEquals(400, status.getResponse().getStatus()))
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("El nombre es requerido"), cuerpo);
    }

    @Test
    @DisplayName("Un JSON mal formado devuelve 400 con mensaje limpio")
    void jsonMalFormadoDevuelve400() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/videojuegos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + tokenNormal())
                        .content("{ esto no es json "))
                .andExpect(status -> assertEquals(400, status.getResponse().getStatus()))
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("no son válidos"), cuerpo);
    }

    @Test
    @DisplayName("Un cuerpo al que le falta un campo obligatorio devuelve 400 explaining que")
    void campoObligatorioAusenteDevuelve400() throws Exception {
        // Usuario.miId es un int primitivo: si no viene en el JSON el backend no
        // puede deserializar. Antes de este manejador la respuesta era un 500 opaco.
        String cuerpo = mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"nuevo\",\"contrasenia\":\"clave12345\"}"))
                .andExpect(status -> assertEquals(400, status.getResponse().getStatus()))
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("no son válidos"), cuerpo);
    }

    @Test
    @DisplayName("Un id con tipo equivocado devuelve 400 y no rompe la API")
    void idConTipoEquivocadoDevuelve400() throws Exception {
        mockMvc.perform(get("/api/videojuegos/abc"))
                .andExpect(status -> assertEquals(400, status.getResponse().getStatus()));
    }

    @Test
    @DisplayName("Un usuario repetido devuelve 409")
    void usuarioRepetidoDevuelve409() throws Exception {
        Usuario existente = new Usuario("repetido", "https://x.com/a.png", "clave12345", Rol.usuarioNormal, 1);
        when(usuarioRepo.findByNombre("repetido")).thenReturn(existente);

        var res = mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"repetido\",\"urlImagen\":\"https://x.com/a.png\",\"contrasenia\":\"clave12345\",\"rol\":\"usuarioNormal\",\"miId\":0}"))
                .andReturn().getResponse();

        assertEquals(409, res.getStatus(), res.getContentAsString());
        assertTrue(res.getContentAsString().contains("Ya existe un usuario"));
    }

    @Test
    @DisplayName("La respuesta de error tiene siempre la misma forma")
    void formaDeLaRespuestaDeError() throws Exception {
        when(videojuegoRepo.findByMiId(anyLong())).thenReturn(null);

        String cuerpo = mockMvc.perform(get("/api/videojuegos/77"))
                .andReturn().getResponse().getContentAsString();

        var json = objectMapper.readTree(cuerpo);
        assertTrue(json.has("error"));
        assertTrue(json.has("status"));
        assertTrue(json.has("timestamp"));
    }
}
