package br.com.fiap.medistockbackend.persistence;

import br.com.fiap.medistockbackend.exception.BusinessRuleException;
import br.com.fiap.medistockbackend.exception.ResourceNotFoundException;
import br.com.fiap.medistockbackend.model.Hospital;
import br.com.fiap.medistockbackend.model.HistoricoConsumo;
import br.com.fiap.medistockbackend.model.ItemEstoque;
import oracle.jdbc.pool.OracleDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class HistoricoConsumoOracleRealDbTest {

    private HistoricoConsumoOracleWriter oracleWriter;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() throws Exception {
        OracleDataSource ds = new OracleDataSource();
        ds.setURL("jdbc:oracle:thin:@//localhost:1521/FREEPDB1");
        ds.setUser("MEDISTOCK");
        ds.setPassword("dbusers");

        this.jdbcTemplate = new JdbcTemplate(ds);
        this.oracleWriter = new HistoricoConsumoOracleWriter(jdbcTemplate);
    }

    @Test
    void testExecucaoComSucessoNoOracle() throws Exception {
        LocalDate mes = LocalDate.of(2026, 5, 1);
        int quantidade = 42;

        ItemEstoque item = ItemEstoque.builder().id(1L).build();
        Hospital hospital = Hospital.builder().id(1L).build();

        HistoricoConsumo historico = HistoricoConsumo.builder()
                .itemEstoque(item)
                .hospital(hospital)
                .mesReferencia(mes)
                .quantidadeConsumida(quantidade)
                .build();

        assertDoesNotThrow(() -> oracleWriter.salvar(historico));

        // Verificar se foi inserido no Oracle
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM HISTORICO_CONSUMO WHERE ITEM_ESTOQUE_ID = 1 AND HOSPITAL_ID = 1 AND QUANTIDADE_CONSUMIDA = 42 AND MES_REFERENCIA = ?",
                Integer.class,
                Date.valueOf(mes)
        );

        assertNotNull(count);
        assertTrue(count > 0, "Deveria ter inserido o registro na tabela HISTORICO_CONSUMO via procedure Oracle");
    }

    @Test
    void testErro20004ItemNaoEncontradoNoOracle() {
        ItemEstoque itemInexistente = ItemEstoque.builder().id(999999L).build();
        Hospital hospital = Hospital.builder().id(1L).build();

        HistoricoConsumo historico = HistoricoConsumo.builder()
                .itemEstoque(itemInexistente)
                .hospital(hospital)
                .mesReferencia(LocalDate.of(2026, 5, 1))
                .quantidadeConsumida(10)
                .build();

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> oracleWriter.salvar(historico));
        assertTrue(ex.getMessage().contains("Item de estoque nao encontrado"), "Mensagem: " + ex.getMessage());
    }

    @Test
    void testErro20005HospitalNaoEncontradoNoOracle() {
        ItemEstoque item = ItemEstoque.builder().id(1L).build();
        Hospital hospitalInexistente = Hospital.builder().id(999999L).build();

        HistoricoConsumo historico = HistoricoConsumo.builder()
                .itemEstoque(item)
                .hospital(hospitalInexistente)
                .mesReferencia(LocalDate.of(2026, 5, 1))
                .quantidadeConsumida(10)
                .build();

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> oracleWriter.salvar(historico));
        assertTrue(ex.getMessage().contains("Hospital nao encontrado"), "Mensagem: " + ex.getMessage());
    }

    @Test
    void testErro20001IdInvalidoNoOracle() {
        ItemEstoque item = ItemEstoque.builder().id(-1L).build();
        Hospital hospital = Hospital.builder().id(1L).build();

        HistoricoConsumo historico = HistoricoConsumo.builder()
                .itemEstoque(item)
                .hospital(hospital)
                .mesReferencia(LocalDate.of(2026, 5, 1))
                .quantidadeConsumida(10)
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> oracleWriter.salvar(historico));
        assertTrue(ex.getMessage().contains("Informe IDs inteiros positivos"), "Mensagem: " + ex.getMessage());
    }

    @Test
    void testErro20002QuantidadeNegativaNoOracle() {
        ItemEstoque item = ItemEstoque.builder().id(1L).build();
        Hospital hospital = Hospital.builder().id(1L).build();

        HistoricoConsumo historico = HistoricoConsumo.builder()
                .itemEstoque(item)
                .hospital(hospital)
                .mesReferencia(LocalDate.of(2026, 5, 1))
                .quantidadeConsumida(-5)
                .build();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> oracleWriter.salvar(historico));
        assertTrue(ex.getMessage().contains("Quantidade deve ser um inteiro nao negativo"), "Mensagem: " + ex.getMessage());
    }
}
