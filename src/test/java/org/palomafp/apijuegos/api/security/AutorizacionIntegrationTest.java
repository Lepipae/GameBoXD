package org.palomafp.apijuegos.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.palomafp.apijuegos.api.modelo.EntradaLista;
import org.palomafp.apijuegos.api.modelo.Usuario;
import org.palomafp.apijuegos.api.modelo.enums.Rol;
import org.palomafp.apijuegos.api.repositories.UsuarioRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de autorizacion de la API. Verifican de extremo a extremo la politica
 * de acceso: token -> filtro -> rol -> decision.
 *
 * <p>La configuracion anterior era {@code anyRequest().permitAll()}, asi que
 * cualquier peticion (incluso sin token) podia leer y BORRAR datos.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AutorizacionIntegrationTest {

    private static final int MI_ID_PROPIO = 1;
    private static final int MI_ID_AJENO = 2;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private UsuarioRepo usuarioRepo;

    /**
     * Registra un usuario en el repositorio simulado y devuelve un token JWT real
     * para que el filtro de Spring lo procese de verdad.
     */
    private String tokenPara(String nombre, int miId, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setContrasenia("clave12345");
        usuario.setRol(rol);
        usuario.setMiId(miId);
        when(usuarioRepo.findByNombre(nombre)).thenReturn(usuario);

        UserDetails detalles = new User(nombre, usuario.getContrasenia(),
                List.of(new SimpleGrantedAuthority("ROLE_" + rol.name())));
        return jwtUtil.generateToken(detalles);
    }

    @BeforeEach
    void setUp() {
        when(usuarioRepo.findByNombre("normal")).thenReturn(null);
    }

    // ------------------------------------------------------------------
    // Lecturas publicas: el catalogo sigue abierto a visitantes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Anonimo puede ver el catalogo de juegos")
    void anonimoLeeVideojuegos() throws Exception {
        mockMvc.perform(get("/api/videojuegos"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Anonimo puede ver las desarrolladoras")
    void anonimoLeeDesarrolladoras() throws Exception {
        mockMvc.perform(get("/api/desarrolladoras"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Anonimo puede ver un perfil publico de usuario (lo necesita la ficha del juego)")
    void anonimoLeePerfilPublico() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNombre("revisor");
        usuario.setContrasenia("clave12345");
        usuario.setRol(Rol.usuarioNormal);
        usuario.setMiId(1);
        when(usuarioRepo.findByMiId(1)).thenReturn(usuario);

        mockMvc.perform(get("/api/usuarios/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Un id de usuario inexistente devuelve 404, no 200 con cuerpo vacio")
    void usuarioInexistenteDevuelve404() throws Exception {
        when(usuarioRepo.findByMiId(999)).thenReturn(null);

        mockMvc.perform(get("/api/usuarios/999"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // Escrituras: requieren token
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Anonimo NO puede borrar un juego")
    void anonimoNoBorraVideojuego() throws Exception {
        mockMvc.perform(delete("/api/videojuegos/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Anonimo NO puede crear un juego")
    void anonimoNoCreaVideojuego() throws Exception {
        mockMvc.perform(post("/api/videojuegos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Un usuario normal puede crear un juego")
    void usuarioNormalCreaVideojuego() throws Exception {
        String token = tokenPara("normal", MI_ID_PROPIO, Rol.usuarioNormal);

        // Llega al controlador: lo que devuelve depende de los repositorios simulados.
        mockMvc.perform(post("/api/videojuegos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"X\"}"))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(
                        401, result.getResponse().getStatus(), "No debe responder 401"));
    }

    // ------------------------------------------------------------------
    // Rol de administrador
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Un usuario normal NO puede borrar un juego")
    void usuarioNormalNoBorraVideojuego() throws Exception {
        String token = tokenPara("normal", MI_ID_PROPIO, Rol.usuarioNormal);

        mockMvc.perform(delete("/api/videojuegos/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un administrador SI puede borrar un juego")
    void administradorBorraVideojuego() throws Exception {
        String token = tokenPara("admin", MI_ID_PROPIO, Rol.administrador);

        mockMvc.perform(delete("/api/videojuegos/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(
                        403, result.getResponse().getStatus(), "No debe responder 403"));
    }

    @Test
    @DisplayName("El listado de usuarios completo es solo de administradores")
    void listadoDeUsuariosSoloParaAdmins() throws Exception {
        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isUnauthorized());

        String tokenNormal = tokenPara("normal", MI_ID_PROPIO, Rol.usuarioNormal);
        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenNormal))
                .andExpect(status().isForbidden());

        String tokenAdmin = tokenPara("admin", MI_ID_PROPIO, Rol.administrador);
        mockMvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(
                        403, result.getResponse().getStatus(), "No debe responder 403"));
    }

    // ------------------------------------------------------------------
    // Propiedad de las listas
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Un usuario NO puede escribir una entrada en la lista de otro")
    void usuarioNoEscribeEnListaAjena() throws Exception {
        String token = tokenPara("normal", MI_ID_PROPIO, Rol.usuarioNormal);

        String cuerpo = new ObjectMapper().writeValueAsString(
                new EntradaLista(9, 10, 7.5, "Reseña", null, 1, MI_ID_AJENO));

        mockMvc.perform(post("/api/lista")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un usuario NO puede leer la lista de otro")
    void usuarioNoLeeListaAjena() throws Exception {
        String token = tokenPara("normal", MI_ID_PROPIO, Rol.usuarioNormal);

        mockMvc.perform(get("/api/lista/" + MI_ID_AJENO)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un usuario SI puede leer su propia lista")
    void usuarioLeeSuLista() throws Exception {
        String token = tokenPara("normal", MI_ID_PROPIO, Rol.usuarioNormal);

        mockMvc.perform(get("/api/lista/" + MI_ID_PROPIO)
                        .header("Authorization", "Bearer " + token))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(
                        403, result.getResponse().getStatus(), "No debe responder 403"));
    }
}
