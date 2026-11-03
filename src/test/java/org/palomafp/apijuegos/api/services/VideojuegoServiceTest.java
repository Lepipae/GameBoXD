package org.palomafp.apijuegos.api.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.palomafp.apijuegos.api.modelo.Videojuego;
import org.palomafp.apijuegos.api.modelo.dto.PaginaVideojuegos;
import org.palomafp.apijuegos.api.modelo.dto.ResumenVideojuego;
import org.palomafp.apijuegos.api.repositories.VideojuegoRepo;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.bson.Document;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VideojuegoServiceTest {

    @Mock
    private VideojuegoRepo videojuegoRepo;

    @Mock
    private EntradaListaService entradaListaService;

    @Mock
    private MongoTemplate mongoTemplate;

    /** Caché real: así los tests cubren también la invalidación al escribir. */
    @Spy
    private CacheTemporal cacheTemporal = new CacheTemporal();

    @InjectMocks
    private VideojuegoService videojuegoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void obtenerTodos() {
        Videojuego v1 = new Videojuego();
        Videojuego v2 = new Videojuego();
        when(videojuegoRepo.findAll()).thenReturn(Arrays.asList(v1, v2));

        List<Videojuego> result = videojuegoService.obtenerTodos();
        assertEquals(2, result.size());
        verify(videojuegoRepo, times(1)).findAll();
    }

    @Test
    void obtenerPorId() {
        Videojuego v = new Videojuego();
        v.setMiId(1L);
        when(videojuegoRepo.findByMiId(1L)).thenReturn(v);

        Videojuego result = videojuegoService.obtenerPorId(1L);
        assertNotNull(result);
        assertEquals(1L, result.getMiId());
    }

    @Test
    void obtenerPorNombre() {
        Videojuego v = new Videojuego();
        v.setNombre("Juego");
        when(videojuegoRepo.findByNombre("Juego")).thenReturn(v);

        Videojuego result = videojuegoService.obtenerPorNombre("Juego");
        assertNotNull(result);
        assertEquals("Juego", result.getNombre());
    }

    @Test
    void obtenerPorTags() {
        Videojuego v = new Videojuego();
        when(videojuegoRepo.findByTags("Accion")).thenReturn(Arrays.asList(v));

        List<Videojuego> result = videojuegoService.obtenerPorTags("Accion");
        assertEquals(1, result.size());
    }

    @Test
    void obtenerPorDesarrolladora() {
        Videojuego v = new Videojuego();
        when(videojuegoRepo.findByIdDesarrolladora(1)).thenReturn(Arrays.asList(v));

        List<Videojuego> result = videojuegoService.obtenerPorDesarrolladora(1);
        assertEquals(1, result.size());
    }

    @Test
    void borrarVideojuego() {
        doNothing().when(entradaListaService).borrarPorVideojuego(1L);
        doNothing().when(videojuegoRepo).deleteByMiId(1L);
        videojuegoService.borrarVideojuego(1L);
        verify(entradaListaService, times(1)).borrarPorVideojuego(1L);
        verify(videojuegoRepo, times(1)).deleteByMiId(1L);
    }

    @Test
    void guardarNuevoExito() {
        Videojuego nuevo = new Videojuego();
        nuevo.setNombre("Nuevo");

        Videojuego ultimo = new Videojuego();
        ultimo.setMiId(5L);

        when(videojuegoRepo.encontrarUltimoId()).thenReturn(ultimo);
        when(videojuegoRepo.findByNombre("Nuevo")).thenReturn(null);
        when(videojuegoRepo.save(any(Videojuego.class))).thenReturn(nuevo);

        Videojuego result = videojuegoService.guardar(nuevo);
        assertEquals(6L, nuevo.getMiId());
        assertNotNull(result);
        verify(videojuegoRepo).save(nuevo);
    }

    @Test
    void guardarNuevoSinUltimo() {
        Videojuego nuevo = new Videojuego();
        nuevo.setNombre("Nuevo");

        when(videojuegoRepo.encontrarUltimoId()).thenReturn(null);
        when(videojuegoRepo.findByNombre("Nuevo")).thenReturn(null);
        when(videojuegoRepo.save(any(Videojuego.class))).thenReturn(nuevo);

        videojuegoService.guardar(nuevo);
        assertEquals(1L, nuevo.getMiId());
        verify(videojuegoRepo).save(nuevo);
    }

    @Test
    void guardarNuevoNombreRepetidoExcepcion() {
        Videojuego nuevo = new Videojuego();
        nuevo.setNombre("Repetido");

        Videojuego repetido = new Videojuego();
        repetido.setNombre("Repetido");

        when(videojuegoRepo.encontrarUltimoId()).thenReturn(null);
        when(videojuegoRepo.findByNombre("Repetido")).thenReturn(repetido);

        assertThrows(IllegalArgumentException.class, () -> videojuegoService.guardar(nuevo));
        verify(videojuegoRepo, never()).save(any());
    }

    @Test
    void guardarExistenteExito() {
        Videojuego existente = new Videojuego();
        existente.setId("1");
        existente.setNombre("Existente");

        when(videojuegoRepo.findByNombre("Existente")).thenReturn(null);
        when(videojuegoRepo.save(any())).thenReturn(existente);

        videojuegoService.guardar(existente);
        verify(videojuegoRepo).save(existente);
    }

    @Test
    void guardarExistenteNombreDeOtroExcepcion() {
        Videojuego aActualizar = new Videojuego();
        aActualizar.setId("1");
        aActualizar.setNombre("Ocupado");

        Videojuego otro = new Videojuego();
        otro.setId("2");
        otro.setNombre("Ocupado");

        when(videojuegoRepo.findByNombre("Ocupado")).thenReturn(otro);

        assertThrows(IllegalArgumentException.class, () -> videojuegoService.guardar(aActualizar));
    }

    // ------------------------------------------------------------------
    // Búsqueda paginada, recomendaciones y resolución por lote
    // ------------------------------------------------------------------

    @Test
    void buscarDevuelveSoloLaPaginaYElTotal() {
        Videojuego v = new Videojuego();
        v.setMiId(3L);
        v.setNombre("Hades");
        v.setUrlImagen("https://img/hades.jpg");
        v.setTags(List.of("roguelike"));

        when(mongoTemplate.count(any(Query.class), eq(Videojuego.class), eq("Videojuegos"))).thenReturn(1L);
        when(mongoTemplate.find(any(Query.class), eq(Videojuego.class), eq("Videojuegos")))
                .thenReturn(List.of(v));

        PaginaVideojuegos pagina = videojuegoService.buscar("hades", null, null, 0, 24);

        assertEquals(1, pagina.content().size());
        assertEquals(1L, pagina.totalElements());
        assertEquals(1, pagina.totalPages());
        assertEquals("Hades", pagina.content().get(0).nombre());
        // La proyección debe excluir la descripción: es el campo que inflaba el JSON.
        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(Videojuego.class), eq("Videojuegos"));
        assertNotNull(captor.getValue().getFieldsObject());
        assertEquals(0, captor.getValue().getFieldsObject().get("descripcion", 0));
    }

    @Test
    void buscarAcotaElTamanoDePaginaYNormalizaLaPaginaNegativa() {
        when(mongoTemplate.count(any(Query.class), eq(Videojuego.class), eq("Videojuegos"))).thenReturn(0L);
        when(mongoTemplate.find(any(Query.class), eq(Videojuego.class), eq("Videojuegos"))).thenReturn(List.of());

        PaginaVideojuegos pagina = videojuegoService.buscar(null, null, null, -5, 9999);

        assertEquals(0, pagina.page());
        assertEquals(VideojuegoService.MAX_TAMANO_PAGINA, pagina.size());
        assertEquals(0, pagina.totalPages());
    }

    @Test
    void recomendacionesUsaAggregationYSample() {
        Document doc = new Document("_id", "abc")
                .append("miId", 7L)
                .append("nombre", "Celeste")
                .append("urlImagen", "https://img/celeste.jpg")
                .append("notaMedia", 9.1)
                .append("idDesarrolladora", 12)
                .append("tags", List.of("plataformas"));

        AggregationResults<Document> resultados = new AggregationResults<>(List.of(doc), new Document());
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("Videojuegos"), eq(Document.class)))
                .thenReturn(resultados);

        List<ResumenVideojuego> recomendaciones = videojuegoService.recomendaciones(4);

        assertEquals(1, recomendaciones.size());
        assertEquals("Celeste", recomendaciones.get(0).nombre());
        assertEquals(7L, recomendaciones.get(0).miId());
        assertEquals("abc", recomendaciones.get(0).id());
        assertEquals(9.1, recomendaciones.get(0).notaMedia());

        ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(mongoTemplate).aggregate(captor.capture(), eq("Videojuegos"), eq(Document.class));
        String pipeline = captor.getValue().toString();
        assertTrue(pipeline.contains("$sample"), "El pipeline debe usar $sample: " + pipeline);
    }

    @Test
    void resumenPorIdsDeduplicaYConsultaUnaSolaVez() {
        Videojuego v = new Videojuego();
        v.setMiId(2L);
        v.setNombre("Hollow Knight");

        when(mongoTemplate.find(any(Query.class), eq(Videojuego.class), eq("Videojuegos")))
                .thenReturn(List.of(v));

        List<ResumenVideojuego> resumenes = videojuegoService.resumenPorIds(List.of(2L, 2L, 5L));

        assertEquals(1, resumenes.size());
        assertEquals(2L, resumenes.get(0).miId());

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, times(1)).find(captor.capture(), eq(Videojuego.class), eq("Videojuegos"));
        String consulta = captor.getValue().getQueryObject().toJson();
        assertTrue(consulta.contains("$in"), "Debe resolverse con un único $in: " + consulta);
    }

    @Test
    void resumenPorIdsVacioNoConsultaMongo() {
        assertTrue(videojuegoService.resumenPorIds(List.of()).isEmpty());
        assertTrue(videojuegoService.resumenPorIds(null).isEmpty());
        verifyNoInteractions(mongoTemplate);
    }
}
