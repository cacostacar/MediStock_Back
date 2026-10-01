package br.com.fiap.medistockbackend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(erro ->
                campos.put(erro.getField(), erro.getDefaultMessage()));

        return construirResposta(HttpStatus.BAD_REQUEST, "Erro de validacao", campos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(ConstraintViolationException ex) {
        return construirResposta(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleParametroInvalido(MethodArgumentTypeMismatchException ex) {
        return construirResposta(HttpStatus.BAD_REQUEST, "Parametro invalido: " + ex.getName(), null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleParametroAusente(MissingServletRequestParameterException ex) {
        return construirResposta(HttpStatus.BAD_REQUEST, "Parametro obrigatorio ausente: " + ex.getParameterName(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violacao de integridade: {}", ex.getMostSpecificCause().getMessage());
        return construirResposta(HttpStatus.CONFLICT,
                "Operacao nao permitida: existem registros vinculados a este recurso", null);
    }

    @ExceptionHandler(EmailCadastradoException.class)
    public ResponseEntity<Map<String, Object>> handleEmailDuplicado(EmailCadastradoException ex) {
        return construirResposta(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleRegraDeNegocio(BusinessRuleException ex) {
        return construirResposta(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRecursoNaoEncontrado(ResourceNotFoundException ex) {
        return construirResposta(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler({CredenciaisInvalidasException.class, BadCredentialsException.class})
    public ResponseEntity<Map<String, Object>> handleCredenciaisInvalidas(RuntimeException ex) {
        return construirResposta(HttpStatus.UNAUTHORIZED, "E-mail institucional ou senha invalidos", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenerico(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return construirResposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", null);
    }

    private ResponseEntity<Map<String, Object>> construirResposta(HttpStatus status, String mensagem, Map<String, String> campos) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("timestamp", LocalDateTime.now());
        corpo.put("status", status.value());
        corpo.put("erro", status.getReasonPhrase());
        corpo.put("mensagem", mensagem);
        if (campos != null && !campos.isEmpty()) {
            corpo.put("campos", campos);
        }
        return ResponseEntity.status(status).body(corpo);
    }
}

