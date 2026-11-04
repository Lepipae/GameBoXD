package org.palomafp.apijuegos.api.controladores;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Sonda mínima para servicios externos de vigilancia (cron-job.org,
 * UptimeRobot, el keep-alive de GitHub Actions...).
 *
 * <p>Existe por dos motivos concretos:</p>
 * <ul>
 *   <li><b>Mantener despierta la instancia de Render.</b> El plan free apaga el
 *       servicio tras 15 minutos sin tráfico. Cualquier petición HTTP lo evita,
 *       y esta es la más barata posible.</li>
 *   <li><b>Distinguir dos fallos distintos.</b> Si {@code /api/ping} responde,
 *       la API está viva; si además {@code /actuator/health} responde, Atlas
 *       también. Sin esta separación, una caída de la base de datos haría
 *       parecer caída la API y el monitor de uptime daría una falsa alarma.</li>
 * </ul>
 *
 * <p>No toca MongoDB a propósito: responde aunque Atlas esté lento o caído, que
 * es justo cuando interesa saber que el contenedor sigue en pie. El coste es de
 * microsegundos (ni consulta, ni plantilla, ni compresión: el cuerpo queda por
 * debajo del umbral de {@code min-response-size}).</p>
 *
 * <p><b>No se cachea en ningún punto.</b> Si un CDN o un proxy sirviera esta
 * respuesta de su caché, la petición no llegaría a Render y el ping dejaría de
 * cumplir su función sin avisar.</p>
 *
 * @author Andrés López
 */
@RestController
@RequestMapping("/api/ping")
public class PingController {

    /**
     * Responde siempre 200 mientras el proceso esté escuchando.
     *
     * <p>El campo {@code hora} no es decorativo: permite comprobar de un vistazo
     * que la respuesta es nueva y no la de una caché intermedia, y da al monitor
     * una forma de medir la latencia si lo necesita.</p>
     *
     * <p>Se mapea con y sin barra final: Spring ya no ignora la barra, y un
     * monitor configurado como {@code /api/ping/} recibiría 401 en lugar de 200.</p>
     *
     * @return JSON con el estado, el nombre del servicio y la marca de tiempo.
     */
    @GetMapping({"", "/"})
    public ResponseEntity<Map<String, String>> ping() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "status", "ok",
                        "servicio", "gameboxd-api",
                        "hora", Instant.now().toString()
                ));
    }
}
