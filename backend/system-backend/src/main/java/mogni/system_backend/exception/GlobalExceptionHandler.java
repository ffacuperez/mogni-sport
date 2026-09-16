package mogni.system_backend.exception;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RegistroInvalidoException.class)
    public ResponseEntity<Map<String, String>> handleRegistroInvalido(RegistroInvalidoException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}