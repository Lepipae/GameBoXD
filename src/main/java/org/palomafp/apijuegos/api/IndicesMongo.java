package org.palomafp.apijuegos.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import static org.springframework.data.domain.Sort.Direction.ASC;

/**
 * Crea al arrancar los índices de MongoDB que la API necesita.
 *
 * <p>La base de datos solo tenía el índice obligatorio {@code _id}, así que cada
 * {@code findByMiId}, {@code findByNombre}, {@code findByIdUsuario} o
 * {@code findByTags} recorría la colección entera. Con Atlas al otro lado de la
 * red eso se paga en cada petición: era la causa principal de la latencia de la API.</p>
 *
 * <p>{@code ensureIndex} es idempotente: si el índice ya existe no hace nada, así
 * que este bean se puede ejecutar en cada arranque sin efectos secundarios. Un
 * fallo al crear un índice se registra pero no impide arrancar: es una mejora de
 * rendimiento, no un requisito de funcionamiento.</p>
 *
 * <p>Los índices son deliberadamente no únicos. Los datos vienen de una base
 * compartida y un índice único sobre {@code miId} haría fallar el arranque de
 * toda la API si hubiera un solo duplicado heredado.</p>
 *
 * @author Andrés López
 */
@Component
public class IndicesMongo implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(IndicesMongo.class);

    /** Colección de videojuegos (nombre real en Atlas, con mayúscula). */
    private static final String COLECCION_VIDEOJUEGOS = "Videojuegos";

    /** Colección de entradas de lista. */
    private static final String COLECCION_ENTRADAS = "EntradaLista";

    /** Colección de desarrolladoras. */
    private static final String COLECCION_DESARROLLADORAS = "Desarrolladoras";

    /** Colección de usuarios (en Atlas se llama "Users"). */
    private static final String COLECCION_USUARIOS = "Users";

    private final MongoTemplate mongoTemplate;

    /**
     * Inyección por constructor: la dependencia queda {@code final}.
     *
     * @param mongoTemplate Plantilla de acceso a MongoDB.
     */
    public IndicesMongo(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Punto de entrada: crea todos los índices y deja constancia en el log.
     *
     * @param args Argumentos de arranque (no se usan).
     */
    @Override
    public void run(ApplicationArguments args) {
        long inicio = System.currentTimeMillis();
        int creados = 0;

        // --- Videojuegos -------------------------------------------------
        // Búsquedas por id público y por nombre: las dos claves de casi todas
        // las lecturas del catálogo.
        creados += crear(COLECCION_VIDEOJUEGOS, "miId_1", indice(ASC, "miId"));
        creados += crear(COLECCION_VIDEOJUEGOS, "nombre_1", indice(ASC, "nombre"));
        // Filtro por etiqueta y por desarrolladora (busqueda.html).
        creados += crear(COLECCION_VIDEOJUEGOS, "tags_1", indice(ASC, "tags"));
        creados += crear(COLECCION_VIDEOJUEGOS, "idDesarrolladora_1", indice(ASC, "idDesarrolladora"));

        // --- EntradaLista ------------------------------------------------
        // La lista de un usuario y las reseñas de un juego son las dos consultas
        // que hacía el frontend en cada carga de página.
        creados += crear(COLECCION_ENTRADAS, "idUsuario_1", indice(ASC, "idUsuario"));
        creados += crear(COLECCION_ENTRADAS, "idVideojuego_1", indice(ASC, "idVideojuego"));
        // Comprobación de duplicado (usuario + juego) al guardar una entrada.
        creados += crear(COLECCION_ENTRADAS, "idUsuario_1_idVideojuego_1",
                indice(ASC, "idUsuario", "idVideojuego"));

        // --- Desarrolladoras ---------------------------------------------
        creados += crear(COLECCION_DESARROLLADORAS, "miId_1", indice(ASC, "miId"));
        creados += crear(COLECCION_DESARROLLADORAS, "nombre_1", indice(ASC, "nombre"));

        // --- Usuarios ----------------------------------------------------
        // Se consulta en cada login y en el filtro JWT.
        creados += crear(COLECCION_USUARIOS, "nombre_1", indice(ASC, "nombre"));
        creados += crear(COLECCION_USUARIOS, "miId_1", indice(ASC, "miId"));

        logger.info("Índices de MongoDB verificados en {} ms ({} nuevos)", System.currentTimeMillis() - inicio, creados);
    }

    /**
     * Construye un índice con nombre explícito sobre uno o varios campos.
     *
     * @param direccion Dirección de orden.
     * @param campos    Campos que componen la clave.
     * @return Índice listo para crear.
     */
    private static Index indice(Sort.Direction direccion, String... campos) {
        Index indice = new Index();
        for (String campo : campos) {
            indice.on(campo, direccion);
        }
        return indice;
    }

    /**
     * Crea un índice si no existe, sin propagar errores.
     *
     * @param coleccion Nombre de la colección.
     * @param nombre    Nombre del índice.
     * @param indice    Índice a asegurar.
     * @return 1 si había que crearlo, 0 si ya existía o falló.
     */
    private int crear(String coleccion, String nombre, Index indice) {
        try {
            var operaciones = mongoTemplate.indexOps(coleccion);
            boolean yaExiste = operaciones.getIndexInfo().stream()
                    .anyMatch(info -> nombre.equals(info.getName()));
            if (yaExiste) {
                return 0;
            }
            indice.named(nombre);
            operaciones.createIndex(indice);
            return 1;
        } catch (RuntimeException ex) {
            logger.warn("No se pudo crear el índice {} en {}: {}", nombre, coleccion, ex.getMessage());
            return 0;
        }
    }
}
