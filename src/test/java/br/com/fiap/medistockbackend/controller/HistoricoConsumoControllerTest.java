package br.com.fiap.medistockbackend.controller;

import br.com.fiap.medistockbackend.dto.IaDtos.HistoricoConsumoRequest;
import br.com.fiap.medistockbackend.exception.BusinessRuleException;
import br.com.fiap.medistockbackend.exception.GlobalExceptionHandler;
import br.com.fiap.medistockbackend.exception.ResourceNotFoundException;
import br.com.fiap.medistockbackend.service.HistoricoConsumoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class HistoricoConsumoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private HistoricoConsumoService historicoConsumoService;

    @InjectMocks
    private HistoricoConsumoController historicoConsumoController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(historicoConsumoController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void deveRetornar201SemCorpoNoSucesso() throws Exception {
        HistoricoConsumoRequest request = new HistoricoConsumoRequest(1L, 2L, "2026-05", 100);

        doNothing().when(historicoConsumoService).registrar(any(HistoricoConsumoRequest.class));

        mockMvc.perform(post("/api/historico-consumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        verify(historicoConsumoService).registrar(any(HistoricoConsumoRequest.class));
    }

    @Test
    void deveRetornar400QuandoRegraDeNegocioViolada() throws Exception {
        HistoricoConsumoRequest request = new HistoricoConsumoRequest(1L, 2L, "2026-05", -10);

        doThrow(new BusinessRuleException("A quantidade consumida não pode ser negativa"))
                .when(historicoConsumoService).registrar(any(HistoricoConsumoRequest.class));

        mockMvc.perform(post("/api/historico-consumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("A quantidade consumida não pode ser negativa"));
    }

    @Test
    void deveRetornar404QuandoRecursoNaoEncontrado() throws Exception {
        HistoricoConsumoRequest request = new HistoricoConsumoRequest(999L, 2L, "2026-05", 10);

        doThrow(new ResourceNotFoundException("Item de estoque nao encontrado: id 999"))
                .when(historicoConsumoService).registrar(any(HistoricoConsumoRequest.class));

        mockMvc.perform(post("/api/historico-consumo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Item de estoque nao encontrado: id 999"));
    }
}
