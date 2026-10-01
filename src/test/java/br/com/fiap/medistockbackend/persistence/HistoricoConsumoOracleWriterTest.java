package br.com.fiap.medistockbackend.persistence;

import br.com.fiap.medistockbackend.exception.BusinessRuleException;
import br.com.fiap.medistockbackend.exception.ResourceNotFoundException;
import br.com.fiap.medistockbackend.model.Hospital;
import br.com.fiap.medistockbackend.model.HistoricoConsumo;
import br.com.fiap.medistockbackend.model.ItemEstoque;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.UncategorizedDataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.CallableStatement;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoricoConsumoOracleWriterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private CallableStatement callableStatement;

    private HistoricoConsumoOracleWriter oracleWriter;
    private HistoricoConsumo historico;

    @BeforeEach
    void setUp() {
        oracleWriter = new HistoricoConsumoOracleWriter(jdbcTemplate);

        ItemEstoque item = ItemEstoque.builder().id(1L).nome("Item").build();
        Hospital hospital = Hospital.builder().id(2L).nome("Hospital").build();

        historico = HistoricoConsumo.builder()
                .itemEstoque(item)
                .hospital(hospital)
                .mesReferencia(LocalDate.of(2026, 5, 1))
                .quantidadeConsumida(150)
                .build();
    }

    @Test
    void deveExecutarProcedureComParametrosCorretos() throws Exception {
        when(jdbcTemplate.execute(eq("{call PR_REGISTRAR_CONSUMO(?, ?, ?, ?)}"), any(CallableStatementCallback.class)))
                .thenAnswer(invocation -> {
                    CallableStatementCallback<?> callback = invocation.getArgument(1);
                    return callback.doInCallableStatement(callableStatement);
                });

        oracleWriter.salvar(historico);

        verify(callableStatement).setLong(1, 1L);
        verify(callableStatement).setLong(2, 2L);
        verify(callableStatement).setDate(3, Date.valueOf(LocalDate.of(2026, 5, 1)));
        verify(callableStatement).setInt(4, 150);
        verify(callableStatement).execute();
    }

    @Test
    void deveTraduzirOracle20001ParaBusinessRuleException() {
        SQLException sqlEx = new SQLException("ORA-20001: Informe IDs inteiros positivos.\nORA-06512: at line 1", "72000", 20001);
        DataAccessException dae = new UncategorizedDataAccessException("Error", sqlEx) {};

        when(jdbcTemplate.execute(anyString(), any(CallableStatementCallback.class))).thenThrow(dae);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> oracleWriter.salvar(historico));
        assertTrue(ex.getMessage().contains("Informe IDs inteiros positivos"));
    }

    @Test
    void deveTraduzirOracle20002ParaBusinessRuleException() {
        SQLException sqlEx = new SQLException("ORA-20002: Quantidade deve ser um inteiro nao negativo.\nORA-06512: at line 1", "72000", 20002);
        DataAccessException dae = new UncategorizedDataAccessException("Error", sqlEx) {};

        when(jdbcTemplate.execute(anyString(), any(CallableStatementCallback.class))).thenThrow(dae);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> oracleWriter.salvar(historico));
        assertTrue(ex.getMessage().contains("Quantidade deve ser um inteiro nao negativo"));
    }

    @Test
    void deveTraduzirOracle20003ParaBusinessRuleException() {
        SQLException sqlEx = new SQLException("ORA-20003: Informe a data de referencia.\nORA-06512: at line 1", "72000", 20003);
        DataAccessException dae = new UncategorizedDataAccessException("Error", sqlEx) {};

        when(jdbcTemplate.execute(anyString(), any(CallableStatementCallback.class))).thenThrow(dae);

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> oracleWriter.salvar(historico));
        assertTrue(ex.getMessage().contains("Informe a data de referencia"));
    }

    @Test
    void deveTraduzirOracle20004ParaResourceNotFoundException() {
        SQLException sqlEx = new SQLException("ORA-20004: Item de estoque nao encontrado.\nORA-06512: at line 1", "72000", 20004);
        DataAccessException dae = new UncategorizedDataAccessException("Error", sqlEx) {};

        when(jdbcTemplate.execute(anyString(), any(CallableStatementCallback.class))).thenThrow(dae);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> oracleWriter.salvar(historico));
        assertTrue(ex.getMessage().contains("Item de estoque nao encontrado"));
    }

    @Test
    void deveTraduzirOracle20005ParaResourceNotFoundException() {
        SQLException sqlEx = new SQLException("ORA-20005: Hospital nao encontrado.\nORA-06512: at line 1", "72000", 20005);
        DataAccessException dae = new UncategorizedDataAccessException("Error", sqlEx) {};

        when(jdbcTemplate.execute(anyString(), any(CallableStatementCallback.class))).thenThrow(dae);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> oracleWriter.salvar(historico));
        assertTrue(ex.getMessage().contains("Hospital nao encontrado"));
    }

    @Test
    void devePreservarErroDeInfraestrutura() {
        SQLException sqlEx = new SQLException("ORA-00942: table or view does not exist", "42000", 942);
        DataAccessException dae = new UncategorizedDataAccessException("Infrastructure error", sqlEx) {};

        when(jdbcTemplate.execute(anyString(), any(CallableStatementCallback.class))).thenThrow(dae);

        assertThrows(DataAccessException.class, () -> oracleWriter.salvar(historico));
    }
}
