package br.com.fiap.medistockbackend.service;

import br.com.fiap.medistockbackend.dto.IaDtos.HistoricoConsumoRequest;
import br.com.fiap.medistockbackend.exception.BusinessRuleException;
import br.com.fiap.medistockbackend.model.Hospital;
import br.com.fiap.medistockbackend.model.HistoricoConsumo;
import br.com.fiap.medistockbackend.model.ItemEstoque;
import br.com.fiap.medistockbackend.persistence.HistoricoConsumoWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoricoConsumoServiceTest {

    @Mock
    private HistoricoConsumoWriter historicoConsumoWriter;

    @Mock
    private ItemEstoqueService itemEstoqueService;

    @Mock
    private HospitalService hospitalService;

    @InjectMocks
    private HistoricoConsumoService historicoConsumoService;

    private ItemEstoque item;
    private Hospital hospital;

    @BeforeEach
    void setUp() {
        item = ItemEstoque.builder().id(10L).nome("Item Teste").build();
        hospital = Hospital.builder().id(20L).nome("Hospital Teste").build();
    }

    @Test
    void deveRegistrarConsumoComMesFormatoAnoMes() {
        when(itemEstoqueService.buscarEntidade(10L)).thenReturn(item);
        when(hospitalService.buscarEntidade(20L)).thenReturn(hospital);

        HistoricoConsumoRequest request = new HistoricoConsumoRequest(
                10L, 20L, "2026-05", 100
        );

        historicoConsumoService.registrar(request);

        ArgumentCaptor<HistoricoConsumo> captor = ArgumentCaptor.forClass(HistoricoConsumo.class);
        verify(historicoConsumoWriter, times(1)).salvar(captor.capture());

        HistoricoConsumo salvo = captor.getValue();
        assertEquals(item, salvo.getItemEstoque());
        assertEquals(hospital, salvo.getHospital());
        assertEquals(LocalDate.of(2026, 5, 1), salvo.getMesReferencia());
        assertEquals(100, salvo.getQuantidadeConsumida());
    }

    @Test
    void deveRegistrarConsumoComMesFormatoAnoMesDia() {
        when(itemEstoqueService.buscarEntidade(10L)).thenReturn(item);
        when(hospitalService.buscarEntidade(20L)).thenReturn(hospital);

        HistoricoConsumoRequest request = new HistoricoConsumoRequest(
                10L, 20L, "2026-05-15", 50
        );

        historicoConsumoService.registrar(request);

        ArgumentCaptor<HistoricoConsumo> captor = ArgumentCaptor.forClass(HistoricoConsumo.class);
        verify(historicoConsumoWriter).salvar(captor.capture());

        HistoricoConsumo salvo = captor.getValue();
        assertEquals(LocalDate.of(2026, 5, 15), salvo.getMesReferencia());
        assertEquals(50, salvo.getQuantidadeConsumida());
    }

    @Test
    void devePermitirQuantidadeZero() {
        when(itemEstoqueService.buscarEntidade(10L)).thenReturn(item);
        when(hospitalService.buscarEntidade(20L)).thenReturn(hospital);

        HistoricoConsumoRequest request = new HistoricoConsumoRequest(
                10L, 20L, "2026-05", 0
        );

        assertDoesNotThrow(() -> historicoConsumoService.registrar(request));
        verify(historicoConsumoWriter, times(1)).salvar(any());
    }

    @Test
    void deveLancarBusinessRuleExceptionQuandoRequestNulo() {
        assertThrows(BusinessRuleException.class, () -> historicoConsumoService.registrar(null));
        verifyNoInteractions(historicoConsumoWriter);
    }

    @Test
    void deveLancarBusinessRuleExceptionQuandoItemEstoqueIdNuloOuInvalido() {
        assertThrows(BusinessRuleException.class, () ->
                historicoConsumoService.registrar(new HistoricoConsumoRequest(null, 20L, "2026-05", 10)));
        assertThrows(BusinessRuleException.class, () ->
                historicoConsumoService.registrar(new HistoricoConsumoRequest(0L, 20L, "2026-05", 10)));
        assertThrows(BusinessRuleException.class, () ->
                historicoConsumoService.registrar(new HistoricoConsumoRequest(-1L, 20L, "2026-05", 10)));
        verifyNoInteractions(historicoConsumoWriter);
    }

    @Test
    void deveLancarBusinessRuleExceptionQuandoHospitalIdNuloOuInvalido() {
        assertThrows(BusinessRuleException.class, () ->
                historicoConsumoService.registrar(new HistoricoConsumoRequest(10L, null, "2026-05", 10)));
        assertThrows(BusinessRuleException.class, () ->
                historicoConsumoService.registrar(new HistoricoConsumoRequest(10L, 0L, "2026-05", 10)));
        assertThrows(BusinessRuleException.class, () ->
                historicoConsumoService.registrar(new HistoricoConsumoRequest(10L, -5L, "2026-05", 10)));
        verifyNoInteractions(historicoConsumoWriter);
    }

    @Test
    void deveLancarBusinessRuleExceptionQuandoQuantidadeNulaOuNegativa() {
        assertThrows(BusinessRuleException.class, () ->
                historicoConsumoService.registrar(new HistoricoConsumoRequest(10L, 20L, "2026-05", null)));
        assertThrows(BusinessRuleException.class, () ->
                historicoConsumoService.registrar(new HistoricoConsumoRequest(10L, 20L, "2026-05", -1)));
        verifyNoInteractions(historicoConsumoWriter);
    }

    @Test
    void deveLancarBusinessRuleExceptionQuandoMesReferenciaInvalido() {
        assertThrows(BusinessRuleException.class, () ->
                historicoConsumoService.registrar(new HistoricoConsumoRequest(10L, 20L, null, 10)));
        assertThrows(BusinessRuleException.class, () ->
                historicoConsumoService.registrar(new HistoricoConsumoRequest(10L, 20L, "   ", 10)));
        assertThrows(BusinessRuleException.class, () ->
                historicoConsumoService.registrar(new HistoricoConsumoRequest(10L, 20L, "2026-13", 10)));
        assertThrows(BusinessRuleException.class, () ->
                historicoConsumoService.registrar(new HistoricoConsumoRequest(10L, 20L, "2026-02-31", 10)));
        assertThrows(BusinessRuleException.class, () ->
                historicoConsumoService.registrar(new HistoricoConsumoRequest(10L, 20L, "data-invalida", 10)));
        verifyNoInteractions(historicoConsumoWriter);
    }
}
