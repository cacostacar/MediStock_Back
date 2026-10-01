package br.com.fiap.medistockbackend.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void deveRetornarBadRequestParaBusinessRuleException() {
        BusinessRuleException ex = new BusinessRuleException("Regra violada");
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleRegraDeNegocio(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().get("status"));
        assertEquals("Regra violada", response.getBody().get("mensagem"));
    }

    @Test
    void deveRetornarNotFoundParaResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Recurso nao encontrado");
        ResponseEntity<Map<String, Object>> response = exceptionHandler.handleRecursoNaoEncontrado(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("Recurso nao encontrado", response.getBody().get("mensagem"));
    }
}
