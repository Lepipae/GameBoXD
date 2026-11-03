package org.palomafp.apijuegos.api.controladores;

import org.palomafp.apijuegos.api.excepciones.RecursoNoEncontradoException;
import org.palomafp.apijuegos.api.modelo.dto.FichaJuego;
import org.palomafp.apijuegos.api.services.FichaService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Ficha completa de un juego en una sola petición.
 *
 * <p>Agrupa el juego, su desarrolladora y sus reseñas. La página de detalle
 * hacía antes tres peticiones encadenadas (la del estudio no se podía lanzar
 * hasta tener el juego), lo que duplicaba el tiempo de carga de la ficha.</p>
 *
 * @author Andrés López
 */
@RestController
@RequestMapping("/api/ficha")
public class FichaController {

    private final FichaService fichaService;

    /**
     * Inyección por constructor.
     *
     * @param fichaService Servicio que compone la ficha.
     */
    public FichaController(FichaService fichaService) {
        this.fichaService = fichaService;
    }

    /**
     * Devuelve la ficha del juego pedido.
     *
     * @param miId Id interno del videojuego.
     * @return Juego, desarrolladora y reseñas.
     */
    @GetMapping("/{miId}")
    public ResponseEntity<FichaJuego> obtener(@PathVariable long miId) {
        FichaJuego ficha = fichaService.obtener(miId);
        if (ficha == null) {
            throw new RecursoNoEncontradoException("No existe ningun videojuego con el id " + miId);
        }
        // Datos públicos (como la ficha y las reseñas por separado) y con
        // cambios poco frecuentes: 30 s de caché pública.
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic())
                .body(ficha);
    }
}
