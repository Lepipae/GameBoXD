package org.palomafp.apijuegos.api.controladores;

import org.palomafp.apijuegos.api.excepciones.RecursoNoEncontradoException;
import org.palomafp.apijuegos.api.modelo.Videojuego;
import org.palomafp.apijuegos.api.modelo.dto.PaginaVideojuegos;
import org.palomafp.apijuegos.api.modelo.dto.ResumenVideojuego;
import org.palomafp.apijuegos.api.services.VideojuegoService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

/**
 * Controlador de la clase videojuego que interactua con la clase
 * videojuegoService para insertar eliminar y revisar datos
 * @author Andrés López
 */
@RestController
@RequestMapping("/api/videojuegos")
public class VideojuegoController {

    private final VideojuegoService videojuegoService;

    /**
     * Inyección por constructor: la dependencia queda {@code final}.
     *
     * @param videojuegoService Servicio de videojuegos.
     */
    public VideojuegoController(VideojuegoService videojuegoService) {
        this.videojuegoService = videojuegoService;
    }

    /**
     * Metodo que devuelve todos los videojuegos en la base de datos
     * @return  Lista conteniendo todos los videojuegos
     */
    @GetMapping
    public ResponseEntity<List<Videojuego>> obtenerTodos() {
        // Se mantiene por compatibilidad (clientes antiguos), pero con caché
        // pública: el catálogo cambia poco y no lleva datos de usuario.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(videojuegoService.obtenerTodos());
    }

    /**
     * Búsqueda paginada del catálogo, con filtro por texto, etiqueta o
     * desarrolladora.
     *
     * <p>Es el endpoint que usa `busqueda.html`. Devolver solo la página pedida
     * y los campos de tarjeta baja la respuesta de ~100 KB a ~2-3 KB.</p>
     *
     * @param q     Texto libre (nombre o etiqueta).
     * @param tag   Etiqueta exacta.
     * @param dev   Id de desarrolladora.
     * @param page  Página, empezando en 0.
     * @param size  Juegos por página.
     * @return Página de resúmenes más la cabecera {@code X-Total-Count}.
     */
    @GetMapping("/pagina")
    public ResponseEntity<PaginaVideojuegos> buscar(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "tag", required = false) String tag,
            @RequestParam(name = "dev", required = false) Integer dev,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "24") int size) {

        PaginaVideojuegos pagina = videojuegoService.buscar(q, tag, dev, page, size);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic())
                .header("X-Total-Count", String.valueOf(pagina.totalElements()))
                .body(pagina);
    }

    /**
     * Recomendaciones aleatorias para la portada.
     *
     * @param limit Número de juegos deseados (por defecto 4).
     * @return Lista de resúmenes aleatorios.
     */
    @GetMapping("/recomendaciones")
    public ResponseEntity<List<ResumenVideojuego>> recomendaciones(
            @RequestParam(name = "limit", defaultValue = "4") int limit) {

        return ResponseEntity.ok()
                // Aleatorio por petición: caché corta para no repetir siempre
                // las mismas cuatro tarjetas, pero suficiente para ráfagas.
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic())
                .body(videojuegoService.recomendaciones(limit));
    }

    /**
     * Resuelve varios juegos en una sola llamada ({@code ?ids=1,2,3}).
     *
     * <p>Lo usa "Mi lista" para pintar la colección sin una petición por juego.</p>
     *
     * @param ids Ids públicos separados por comas.
     * @return Resúmenes de los juegos encontrados.
     */
    @GetMapping("/lote")
    public ResponseEntity<List<ResumenVideojuego>> lote(
            @RequestParam(name = "ids", required = false) String ids) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(2)).cachePublic())
                .body(videojuegoService.resumenPorIds(parsearIds(ids)));
    }

    /**
     * Convierte la lista de ids de la query string en números, ignorando los
     * valores no numéricos en lugar de fallar con un 400.
     *
     * @param ids Texto {@code "1,2,3"} (puede ser {@code null}).
     * @return Lista de ids válidos.
     */
    private static List<Long> parsearIds(String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .map(valor -> {
                    try {
                        return Long.parseLong(valor);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    /**
     * Metodo que devuelve un videojuego asociado a su id interno
     * @param miId  Id interno de la base de datos
     * @return      Videojuego asociado a ese id
     */
    @GetMapping("/{miId}")
    public ResponseEntity<Videojuego> obtenerPorId(@PathVariable long miId) {
        var juego = videojuegoService.obtenerPorId(miId);
        if (juego == null) {
            throw new RecursoNoEncontradoException("No existe ningun videojuego con el id " + miId);
        }
        // Ficha de juego: misma respuesta para todos y cambia poco. La caché
        // evita repetir la consulta al volver atrás o recargar.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .body(juego);
    }

    /**
     * Metodo que devuelve un videojuego con el nombre indicado
     * @param nombre    Nombre del videojuego que se busca
     * @return          Videojuego con el nombre asociado
     */
    @GetMapping("/nombre/{nombre}")
    public Videojuego obtenerPorNombre(@PathVariable String nombre) {
        var juego = videojuegoService.obtenerPorNombre(nombre);
        if (juego == null) {
            throw new RecursoNoEncontradoException("No existe ningun videojuego llamado " + nombre);
        }
        return juego;
    }

    /**
     * Metodo que devuelve una lista de videojuegos por sus tags
     * @param tag   Tag o categoria a buscar
     * @return      Lista de videojuegos con ese tag
     */
    @GetMapping("/tag/{tag}")
    public List<Videojuego> obtenerPorTags(@PathVariable String tag) {
        return videojuegoService.obtenerPorTags(tag);
    }

    /**
     * Metodo que devuelve los videojuegos de una desarrolladora especifica
     * @param desarrollador Id interno de la desarrolladora
     * @return              Lista de videojuegos creados por esa desarrolladora
     */
    @GetMapping("/desarrolladora/{desarrollador}")
    public List<Videojuego> obtenerPorDesarrolladora(@PathVariable int desarrollador) {
        return videojuegoService.obtenerPorDesarrolladora(desarrollador);
    }

    /**
     * Metodo que devuelve el videojuego al que pertenecen más entradas de lista
     * (lo más reseñado en el momento). Alimenta el hero del index.
     *
     * <p>Público como el resto de lecturas de catálogo: no expone datos de
     * usuarios, solo el juego más popular.</p>
     *
     * @return Juego con más entradas de lista
     */
    @GetMapping("/mas-resenyas")
    public ResponseEntity<Videojuego> obtenerMasResenyado() {
        var juego = videojuegoService.obtenerMasResenyado();
        if (juego == null) {
            throw new RecursoNoEncontradoException("Todavia no hay ninguna entrada de lista");
        }
        // Cambia solo cuando alguien puntúa un juego, no en cada visita: una
        // caché corta absorbe las recargas y los rerenders sin servir datos
        // viejos durante mucho tiempo.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                .body(juego);
    }

    /**
     * Metodo para guardar un videojuego en la base de datos
     * @param videojuego    Objeto videojuego a guardar
     * @return              Objeto guardado
     */
    @PostMapping
    public Videojuego guardar(@Valid @RequestBody Videojuego videojuego) {
        return videojuegoService.guardar(videojuego);
    }

    /**
     * Metodo para eliminar un videojuego de la base de datos
     * @param miId  Id interno del videojuego que se quiere borrar
     */
    @DeleteMapping("/{miId}")
    @PreAuthorize("hasRole('administrador')")
    public void borrarVideojuego(@PathVariable long miId) {
        videojuegoService.borrarVideojuego(miId);
    }

}
