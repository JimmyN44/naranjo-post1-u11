package com.tienda.pedidos.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Manejo centralizado de errores: reemplaza el try/catch disperso del codigo de partida,
 * que silenciaba la excepcion con println y devolvia null (200 OK vacio).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RecursoNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError handleNotFound(RecursoNoEncontradoException ex,
                                   HttpServletRequest req) {
        // El WARN con el id ya lo registra PedidoServiceImpl; aqui solo seguimiento tecnico
        log.debug("Respondiendo 404 en path={}: {}", req.getRequestURI(), ex.getMessage());
        return new ApiError(404, "Not Found", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleValidation(MethodArgumentNotValidException ex,
                                     HttpServletRequest req) {
        String errores = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Solicitud invalida en path={}: {}", req.getRequestURI(), errores);
        return new ApiError(400, "Bad Request", errores, req.getRequestURI());
    }

    // Cuerpo JSON mal formado o parametro de ruta con tipo invalido (ej. /api/pedidos/abc):
    // es un error del cliente (400), no debe caer en el manejador generico como 500
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleSolicitudMalFormada(Exception ex, HttpServletRequest req) {
        String mensaje = (ex instanceof MethodArgumentTypeMismatchException tme)
                ? "El parametro '" + tme.getName() + "' tiene un formato invalido"
                : "El cuerpo de la solicitud no es un JSON valido";
        log.warn("Solicitud mal formada en path={}: {}", req.getRequestURI(), mensaje);
        return new ApiError(400, "Bad Request", mensaje, req.getRequestURI());
    }

    // Excepciones de Spring MVC que ya traen su propio codigo HTTP
    // (ruta inexistente 404, metodo no soportado 405, media type no soportado 415):
    // se conserva ese codigo para no convertirlas en 500
    @ExceptionHandler({NoResourceFoundException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class})
    public ResponseEntity<ApiError> handleErrorSpringMvc(Exception ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.valueOf(((ErrorResponse) ex).getStatusCode().value());
        log.warn("Solicitud no soportada: method={}, path={}, status={}",
                req.getMethod(), req.getRequestURI(), status.value());
        ApiError body = new ApiError(status.value(), status.getReasonPhrase(),
                "Solicitud no soportada: " + req.getMethod() + " " + req.getRequestURI(),
                req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    // Ultima red de seguridad: nunca expone el mensaje ni la traza de la excepcion al cliente
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleGeneral(Exception ex, HttpServletRequest req) {
        // La traza completa queda solo en el log del servidor (ultimo argumento = excepcion)
        log.error("Error inesperado en method={}, path={}", req.getMethod(), req.getRequestURI(), ex);
        return new ApiError(500, "Internal Server Error",
                "Error inesperado. Contactar soporte.", req.getRequestURI());
    }
}
