package org.palomafp.apijuegos.api;

import org.palomafp.apijuegos.api.excepciones.RecursoDuplicadoException;
import org.palomafp.apijuegos.api.excepciones.RecursoNoEncontradoException;
import org.palomafp.apijuegos.api.modelo.dto.RespuestaError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Manejador global de errores de la API.
 *
 * <p>Sin esta clase, cualquier {@code IllegalArgumentException} de los modelos
 * o servicios se traducía en un <b>500 Internal Server Error</b>, de modo que
 * un error de negocio corriente ("ese juego ya existe", "la nota debe estar
 * entre 0 y 10") era indistinguible de una caída real del servidor.</p>
 *
 * <p>El formato de la respuesta es siempre el mismo, y lo fija el record
 * {@link RespuestaError}:</p>
 *
 * <pre>{@code
 * { "error": "mensaje legible", "status": 409, "timestamp": "2026-09-28T10:00:00Z" }
 * }</pre>
 *
 * @author Andrés López
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger logger = LoggerFactory.getLogger(ManejadorErrores.class);

    /**
     * Construye la respuesta de error con el formato común de la API.
     *
     * @param estado  Código HTTP a devolver.
     * @param mensaje Mensaje legible para el cliente.
     * @return Respuesta con el cuerpo de error.
     */
    private ResponseEntity<RespuestaError> error(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(RespuestaError.de(mensaje, estado.value()));
    }

    /**
     * 409: el recurso ya existe con esos mismos datos.
     * @param ex Excepción de duplicado.
     * @return Respuesta 409.
     */
    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<RespuestaError> duplicado(RecursoDuplicadoException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * 404: el recurso solicitado no existe.
     * @param ex Excepción de recurso ausente.
     * @return Respuesta 404.
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> noEncontrado(RecursoNoEncontradoException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * 403: se ha autenticado pero no se tiene permiso, o la entrada pertenece
     * a otro usuario. Se declara explícitamente porque, al existir este
     * manejador, el DispatcherServlet resolvería la excepción antes de que
     * llegase al filtro de Spring Security y el 403 se perdería.
     * @param ex Excepción de acceso denegado.
     * @return Respuesta 403.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespuestaError> accesoDenegado(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * 401: credenciales incorrectas al iniciar sesión.
     * @param ex Excepción de credenciales inválidas.
     * @return Respuesta 401.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<RespuestaError> credencialesInvalidas(BadCredentialsException ex) {
        return error(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos");
    }

    /**
     * 400: los datos no son válidos (campos vacíos, nota fuera de rango...).
     * @param ex Excepción de validación.
     * @return Respuesta 400.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RespuestaError> datoInvalido(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * 400: el cuerpo JSON no se pudo interpretar.
     * @param ex Excepción de deserialización.
     * @return Respuesta 400.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> cuerpoInvalido(HttpMessageNotReadableException ex) {
        // Cuando los setters del modelo rechazan un campo (por ejemplo un nombre
        // vacio), Jackson envuelve el IllegalArgumentException y el mensaje
        // concreto se perderia. Se recupera para que el cliente sepa qué corregir.
        var causa = ex.getMostSpecificCause();
        logger.warn("Cuerpo de la petición no interpretable: {}", causa.getMessage());

        // Pattern matching: la variable `ilegal` solo existe dentro de la rama
        // en la que la causa es realmente un IllegalArgumentException.
        if (causa instanceof IllegalArgumentException ilegal && ilegal.getMessage() != null) {
            return error(HttpStatus.BAD_REQUEST, ilegal.getMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos");
    }

    /**
     * 400: un parámetro de la URL tiene un tipo incorrecto (por ejemplo /api/videojuegos/abc).
     * @param ex Excepción de tipo no coincidente.
     * @return Respuesta 400.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RespuestaError> tipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST,
                "El valor '" + ex.getValue() + "' no es válido para " + ex.getName());
    }

    /**
     * 400: falta un parámetro obligatorio.
     * @param ex Excepción de parámetro ausente.
     * @return Respuesta 400.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<RespuestaError> parametroAusente(MissingServletRequestParameterException ex) {
        return error(HttpStatus.BAD_REQUEST,
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "'");
    }

    /**
     * 500: cualquier otro fallo. Se registra entero en el servidor pero al
     * cliente solo se le devuelve un mensaje genérico, para no filtrar detalles
     * internos de la base de datos ni trazas de pila.
     * @param ex Excepción inesperada.
     * @return Respuesta 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> errorInesperado(Exception ex) {
        logger.error("Error no controlado en la API", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
    }
}
