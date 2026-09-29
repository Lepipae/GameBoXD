package org.palomafp.apijuegos.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.palomafp.apijuegos.api.modelo.Usuario;
import org.palomafp.apijuegos.api.modelo.enums.Rol;
import org.palomafp.apijuegos.api.repositories.UsuarioRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Comprueba que el valor heredado "placeholder" ha desaparecido de las respuestas
 * de la API y que el campo urlImagen admite null.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UrlImagenTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioRepo usuarioRepo;

    /** Simula un usuario guardado por la version antigua, con el literal en el campo. */
    private Usuario usuarioConValorHeredado(String nombre) {
        Usuario usuario = new Usuario();
        usuario.setNombre(nombre);
        usuario.setContrasenia("$2a$10$hashfalsohashfalsohashfalsohash");
        usuario.setRol(Rol.usuarioNormal);
        usuario.setMiId(1);
        // Se escribe el valor heredado por la via interna que usaba el backend antes.
        usuario.setUrlImagen("placeholder");
        return usuario;
    }

    @Test
    @DisplayName("Un usuario con el valor heredado se serializa con urlImagen null")
    void valorHeredadoSeSerializaComoNull() throws Exception {
        when(usuarioRepo.findByMiId(1)).thenReturn(usuarioConValorHeredado("testuser4"));

        String cuerpo = mockMvc.perform(get("/api/usuarios/1"))
                .andReturn().getResponse().getContentAsString();

        assertFalse(cuerpo.contains("placeholder"),
                "El literal placeholder sigue apareciendo en la API: " + cuerpo);
        assertTrue(cuerpo.contains("\"urlImagen\":null"),
                "Se esperaba urlImagen null: " + cuerpo);
    }

    @Test
    @DisplayName("Un usuario sin imagen se serializa con urlImagen null")
    void usuarioSinImagenSeSerializaComoNull() throws Exception {
        Usuario usuario = usuarioConValorHeredado("pipae");
        usuario.setUrlImagen(null);
        when(usuarioRepo.findByMiId(1)).thenReturn(usuario);

        String cuerpo = mockMvc.perform(get("/api/usuarios/1"))
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("\"urlImagen\":null"), cuerpo);
    }

    @Test
    @DisplayName("Una URL de imagen real se conserva intacta")
    void urlRealSeConserva() throws Exception {
        Usuario usuario = usuarioConValorHeredado("conavatar");
        usuario.setUrlImagen("https://ejemplo.com/avatar.png");
        when(usuarioRepo.findByMiId(1)).thenReturn(usuario);

        String cuerpo = mockMvc.perform(get("/api/usuarios/1"))
                .andReturn().getResponse().getContentAsString();

        assertTrue(cuerpo.contains("https://ejemplo.com/avatar.png"), cuerpo);
    }
}
