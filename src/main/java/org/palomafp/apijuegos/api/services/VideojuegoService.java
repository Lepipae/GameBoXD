package org.palomafp.apijuegos.api.services;

import org.palomafp.apijuegos.api.excepciones.RecursoDuplicadoException;
import org.palomafp.apijuegos.api.modelo.Videojuego;
import org.palomafp.apijuegos.api.modelo.ValidadorUrlImagen;
import org.palomafp.apijuegos.api.modelo.dto.PaginaVideojuegos;
import org.palomafp.apijuegos.api.modelo.dto.ResumenVideojuego;
import org.palomafp.apijuegos.api.repositories.VideojuegoRepo;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Servicio que gestiona la logica de negocio de Videojuego
 * @author Andrés López
 */
@Service
public class VideojuegoService {

    /** Tamaño máximo de página aceptado: evita que un cliente pida el catálogo entero "de una". */
    public static final int MAX_TAMANO_PAGINA = 60;

    /** Tope de ids que se pueden pedir en una sola llamada al endpoint de lote. */
    public static final int MAX_IDS_LOTE = 200;

    /** Orden estable por id público: sin él, MongoDB puede devolver las páginas desordenadas. */
    private static final Sort ORDEN_POR_ID = Sort.by(Sort.Direction.ASC, "miId");

    /** Campos que necesita una tarjeta; el resto del documento no viaja al cliente. */
    private static final String[] CAMPOS_RESUMEN = {
            "miId", "nombre", "urlImagen", "notaMedia", "idDesarrolladora", "tags"
    };

    /** Colección real en Atlas (el nombre del documento va en mayúscula). */
    private static final String COLECCION = "Videojuegos";

    /** Tamaño de página por defecto cuando el cliente no pide uno. */
    private static final int TAMANO_PAGINA_POR_DEFECTO = 24;

    private final VideojuegoRepo videojuegoRepo;
    private final EntradaListaService entradaListaService;
    private final MongoTemplate mongoTemplate;

    /**
     * Inyección por constructor: las dependencias quedan {@code final}.
     *
     * @param videojuegoRepo    Repositorio de videojuegos.
     * @param entradaListaService Servicio de entradas de lista, para la cascada de borrado.
     * @param mongoTemplate     Plantilla de MongoDB para las consultas paginadas y proyecciones.
     */
    public VideojuegoService(VideojuegoRepo videojuegoRepo, EntradaListaService entradaListaService,
                             MongoTemplate mongoTemplate) {
        this.videojuegoRepo = videojuegoRepo;
        this.entradaListaService = entradaListaService;
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Metodo que devuelve todos los videojuegos de la base de datos
     * @return  Lista de todos los juegos
     */
    public List<Videojuego> obtenerTodos() {
        return videojuegoRepo.findAll();
    }

    /**
     * Metodo que obtiene un videojuego asociado a su id interno
     * @param idVideojuego  id interno
     * @return  Videojuego asociado al id
     */
    public Videojuego obtenerPorId(long idVideojuego) {
        return videojuegoRepo.findByMiId(idVideojuego);
    }

    /**
     * Obtiene un videojuego a partir de su nombre
     * @param nombreVideojuego Nombre del videojuego
     * @return Videojuego encontrado o null
     */
    public Videojuego obtenerPorNombre(String nombreVideojuego) {
        return videojuegoRepo.findByNombre(nombreVideojuego);
    }

    /**
     * Obtiene una lista de videojuegos a partir de un tag o categoria
     * @param tag Categoria a buscar
     * @return Lista de videojuegos con el tag
     */
    public List<Videojuego> obtenerPorTags(String tag) {
        return videojuegoRepo.findByTags(tag);
    }

    /**
     * Obtiene una lista de videojuegos creados por una desarrolladora
     * @param desarrolladora Id interno de la desarrolladora
     * @return Lista de videojuegos de la desarrolladora
     */
    public List<Videojuego> obtenerPorDesarrolladora(int desarrolladora) {
        return videojuegoRepo.findByIdDesarrolladora(desarrolladora);
    }

    /**
     * Metodo que obtiene el videojuego con más entradas de lista, de cara al
     * hero del index (lo más reseñado en el momento).
     *
     * @return Juego más reseñado, o null si todavía no hay entradas.
     */
    public Videojuego obtenerMasResenyado() {
        return entradaListaService.juegoConMasEntradas();
    }

    /**
     * Busca videojuegos con filtros opcionales y devuelve solo la página pedida.
     *
     * <p>Sustituye al patrón anterior, en el que el frontend descargaba el
     * catálogo completo (101 KB) para filtrarlo en el navegador. Aquí el filtro
     * y el corte de página los hace MongoDB, y el cliente recibe únicamente los
     * campos que pinta una tarjeta.</p>
     *
     * <p>Se excluyen los juegos sin nombre o sin portada utilizable: la rejilla
     * de búsqueda nunca los mostraba, así que devolverlos solo gastaría ancho de
     * banda y descuadraría los totales.</p>
     *
     * @param texto             Texto libre a buscar en el nombre o en las etiquetas ({@code null} o vacío = sin filtro).
     * @param tag               Etiqueta exacta (sin distinguir mayúsculas), o {@code null}.
     * @param idDesarrolladora  Id de la desarrolladora, o {@code null}.
     * @param page              Número de página, empezando en 0.
     * @param size              Tamaño de página (se acota a {@link #MAX_TAMANO_PAGINA}).
     * @return Página de resúmenes con los totales para paginar.
     */
    public PaginaVideojuegos buscar(String texto, String tag, Integer idDesarrolladora, int page, int size) {
        int pagina = Math.max(0, page);
        int tamano = size <= 0 ? TAMANO_PAGINA_POR_DEFECTO : Math.min(size, MAX_TAMANO_PAGINA);

        List<Criteria> filtros = new ArrayList<>();

        if (texto != null && !texto.isBlank()) {
            // Pattern.quote evita que un texto con paréntesis o puntos se
            // interprete como expresión regular (y que un cliente pueda colgar
            // la consulta con un patrón costoso).
            String patron = Pattern.quote(texto.trim());
            filtros.add(new Criteria().orOperator(
                    Criteria.where("nombre").regex(patron, "i"),
                    Criteria.where("tags").regex(patron, "i")
            ));
        }
        if (tag != null && !tag.isBlank()) {
            filtros.add(Criteria.where("tags").regex("^" + Pattern.quote(tag.trim()) + "$", "i"));
        }
        if (idDesarrolladora != null) {
            filtros.add(Criteria.where("idDesarrolladora").is(idDesarrolladora));
        }

        // Solo juegos pintables: nombre no vacío y portada que no sea el
        // marcador heredado "placeholder".
        filtros.add(Criteria.where("nombre").ne(null).ne(""));
        filtros.add(Criteria.where("urlImagen").nin(null, "", ValidadorUrlImagen.VALOR_HEREDADO));

        Criteria criterios = new Criteria().andOperator(filtros.toArray(new Criteria[0]));

        long total = mongoTemplate.count(new Query(criterios), Videojuego.class, COLECCION);

        Query consulta = new Query(criterios).with(ORDEN_POR_ID)
                .skip((long) pagina * tamano)
                .limit(tamano);
        consulta.fields().include(CAMPOS_RESUMEN);

        List<ResumenVideojuego> contenido = mongoTemplate
                .find(consulta, Videojuego.class, COLECCION)
                .stream()
                .map(ResumenVideojuego::de)
                .toList();

        return PaginaVideojuegos.de(contenido, pagina, tamano, total);
    }

    /**
     * Devuelve una muestra aleatoria de juegos para el index.
     *
     * <p>Antes el index descargaba los 198 juegos para pintar 4 tarjetas. Con
     * {@code $sample} MongoDB elige los documentos servidor-side y solo viajan
     * los cuatro resúmenes: la portada deja de costar ~100 KB y pasa a costar
     * ~2 KB.</p>
     *
     * @param limite Número de juegos deseados (se acota a {@link #MAX_TAMANO_PAGINA}).
     * @return Lista de resúmenes aleatorios; vacía si no hay juegos válidos.
     */
    public List<ResumenVideojuego> recomendaciones(int limite) {
        int cantidad = Math.max(1, Math.min(limite, MAX_TAMANO_PAGINA));

        Criteria criterios = new Criteria().andOperator(
                Criteria.where("nombre").ne(null).ne(""),
                Criteria.where("urlImagen").nin(null, "", ValidadorUrlImagen.VALOR_HEREDADO)
        );

        Aggregation agregacion = Aggregation.newAggregation(
                Aggregation.match(criterios),
                Aggregation.sample(cantidad),
                Aggregation.project(CAMPOS_RESUMEN)
        );

        return mongoTemplate
                .aggregate(agregacion, COLECCION, Document.class)
                .getMappedResults()
                .stream()
                .map(VideojuegoService::aResumen)
                .toList();
    }

    /**
     * Devuelve los resúmenes de varios juegos en una sola consulta.
     *
     * <p>Pensado para "Mi lista": antes el frontend pedía cada juego uno a uno
     * (N peticiones) y ahora resuelve todos los ids de una vez.</p>
     *
     * @param ids Ids públicos de los juegos (se deduplican y se acotan a {@link #MAX_IDS_LOTE}).
     * @return Resúmenes encontrados; los ids inexistentes simplemente no aparecen.
     */
    public List<ResumenVideojuego> resumenPorIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Long> unicos = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .limit(MAX_IDS_LOTE)
                .toList();

        if (unicos.isEmpty()) {
            return List.of();
        }

        Query consulta = new Query(Criteria.where("miId").in(unicos));
        consulta.fields().include(CAMPOS_RESUMEN);

        return mongoTemplate.find(consulta, Videojuego.class, COLECCION)
                .stream()
                .map(ResumenVideojuego::de)
                .toList();
    }

    /**
     * Convierte un documento de agregación en el resumen que consume el frontend.
     *
     * @param doc Documento devuelto por MongoDB.
     * @return Resumen ligero del juego.
     */
    private static ResumenVideojuego aResumen(Document doc) {
        Object id = doc.get("_id");
        Object miId = doc.get("miId");
        Object nota = doc.get("notaMedia");
        Object desarrolladora = doc.get("idDesarrolladora");

        List<String> tags = List.of();
        if (doc.get("tags") instanceof List<?> lista) {
            tags = lista.stream().map(String::valueOf).toList();
        }

        return new ResumenVideojuego(
                id == null ? null : id.toString(),
                miId instanceof Number numero ? numero.longValue() : null,
                doc.getString("nombre"),
                doc.getString("urlImagen"),
                nota instanceof Number numero ? numero.doubleValue() : 0d,
                desarrolladora instanceof Number numero ? numero.intValue() : 0,
                tags
        );
    }

    /**
     * Guarda un videojuego en la base de datos
     * @param videojuego Objeto a guardar
     * @return Videojuego guardado
     * @throws RecursoDuplicadoException si el videojuego ya existe
     */
    public Videojuego guardar(Videojuego videojuego) {
        if (videojuego.getId() == null) {
            Videojuego ultimo = videojuegoRepo.encontrarUltimoId();
            videojuego.setMiId(ultimo != null ? ultimo.getMiId() + 1 : 1);
        }

        // Igual que en UsuarioService: al actualizar, el registro con el mismo
        // nombre solo es duplicado si es OTRO juego distinto del que se guarda.
        Videojuego previo = videojuegoRepo.findByNombre(videojuego.getNombre());
        boolean esOtro = previo != null
                && (videojuego.getId() == null || !previo.getId().equals(videojuego.getId()));
        if (esOtro) {
            throw new RecursoDuplicadoException("Ya existe un videojuego con ese nombre");
        }

        return videojuegoRepo.save(videojuego);
    }

    /**
     * Borra un videojuego a partir de su id interno y elimina sus entradas asociadas
     * @param id Id interno del videojuego
     */
    public void borrarVideojuego(long id) {
        entradaListaService.borrarPorVideojuego(id);
        videojuegoRepo.deleteByMiId(id);
    }
}
