package org.palomafp.apijuegos.api.controladores;

import org.palomafp.apijuegos.api.services.ImagenService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;

/**
 * Sirve las portadas de los juegos redimensionadas y recomprimidas.
 *
 * <p>Las originales son PNG de ~730 KB; a 300 px y en JPEG pesan ~30 KB. Ese
 * es el mayor ahorro de ancho de banda de la aplicación, porque una sola
 * tarjeta costaba más que veinte respuestas de la API.</p>
 *
 * <p>El endpoint es público porque las portadas también lo son, pero solo
 * acepta URLs del CDN de SteamGridDB (ver {@link ImagenService}).</p>
 *
 * @author Andrés López
 */
@RestController
@RequestMapping("/api/imagenes")
public class ImagenController {

    private final ImagenService imagenService;

    /**
     * Inyección por constructor.
     *
     * @param imagenService Servicio de descarga y redimensionado.
     */
    public ImagenController(ImagenService imagenService) {
        this.imagenService = imagenService;
    }

    /**
     * Devuelve la portada pedida, escalada al ancho indicado.
     *
     * @param url   URL de la portada (debe pertenecer al CDN permitido).
     * @param ancho Ancho deseado en píxeles (se acota entre 80 y 800).
     * @return Bytes JPEG de la imagen.
     */
    @GetMapping
    public ResponseEntity<byte[]> obtener(
            @RequestParam(name = "url") String url,
            @RequestParam(name = "w", defaultValue = "300") int ancho) {

        if (!ImagenService.esUrlPermitida(url)) {
            return ResponseEntity.badRequest().build();
        }

        try {
            byte[] imagen = imagenService.obtener(url, ancho);
            return ResponseEntity.ok()
                    // Las portadas de un juego no cambian: se cachean una semana.
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic().immutable())
                    .contentType(MediaType.IMAGE_JPEG)
                    .header("X-Image-Width", String.valueOf(ImagenService.normalizarAncho(ancho)))
                    .body(imagen);
        } catch (IOException ex) {
            // El frontend cae a la URL original cuando este endpoint falla.
            return ResponseEntity.notFound().build();
        }
    }
}
