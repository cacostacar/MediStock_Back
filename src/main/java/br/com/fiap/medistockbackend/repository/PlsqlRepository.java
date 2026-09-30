package br.com.fiap.medistockbackend.repository;

import br.com.fiap.medistockbackend.dto.PlsqlDtos.AlertaRegistradoResponse;
import br.com.fiap.medistockbackend.dto.PlsqlDtos.IndicadorEstoqueResponse;
import br.com.fiap.medistockbackend.dto.PlsqlDtos.ItemConsumoResponse;
import br.com.fiap.medistockbackend.dto.PlsqlDtos.RelatorioConsumoResponse;
import br.com.fiap.medistockbackend.model.AlertaTipo;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@Repository
@Profile("oracle")
public class PlsqlRepository {

    private static final String SQL_INDICADORES = """
            SELECT ie.id,
                   ie.nome,
                   h.nome                             AS hospital_nome,
                   fn_dias_cobertura_estoque(ie.id)   AS dias_cobertura,
                   fn_status_estoque_formatado(ie.id) AS status_formatado
              FROM itens_estoque ie
              JOIN hospitais h ON h.id = ie.hospital_id
            """;

    private static final String SQL_ALERTAS = """
            SELECT a.id, a.item_estoque_id, ie.nome AS item_nome, a.hospital_id,
                   a.tipo, a.mensagem, a.origem, a.criado_em
              FROM alertas a
              LEFT JOIN itens_estoque ie ON ie.id = a.item_estoque_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final SimpleJdbcCall registrarAlertasCall;
    private final SimpleJdbcCall relatorioConsumoCall;

    public PlsqlRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

        this.registrarAlertasCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("PRC_REGISTRAR_ALERTAS_CRITICOS")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_hospital_id", Types.NUMERIC),
                        new SqlOutParameter("p_total_alertas", Types.NUMERIC));

        this.relatorioConsumoCall = new SimpleJdbcCall(jdbcTemplate)
                .withProcedureName("PRC_RELATORIO_CONSUMO_HOSPITAL")
                .withoutProcedureColumnMetaDataAccess()
                .declareParameters(
                        new SqlParameter("p_hospital_id", Types.NUMERIC),
                        new SqlParameter("p_mes_referencia", Types.DATE),
                        new SqlOutParameter("p_total_itens", Types.NUMERIC),
                        new SqlOutParameter("p_total_consumido", Types.NUMERIC),
                        new SqlOutParameter("p_custo_total", Types.NUMERIC),
                        new SqlOutParameter("p_cursor", Types.REF_CURSOR, this::mapearItemConsumo));
    }

    public int registrarAlertasCriticos(Long hospitalId) {
        Map<String, Object> saida = registrarAlertasCall.execute(new MapSqlParameterSource()
                .addValue("p_hospital_id", hospitalId));
        return numero(saida.get("p_total_alertas")).intValue();
    }

    @SuppressWarnings("unchecked")
    public RelatorioConsumoResponse gerarRelatorioConsumo(Long hospitalId, YearMonth mes) {
        Map<String, Object> saida = relatorioConsumoCall.execute(new MapSqlParameterSource()
                .addValue("p_hospital_id", hospitalId)
                .addValue("p_mes_referencia", Date.valueOf(mes.atDay(1))));

        return new RelatorioConsumoResponse(
                hospitalId,
                mes,
                numero(saida.get("p_total_itens")).intValue(),
                numero(saida.get("p_total_consumido")).longValue(),
                numero(saida.get("p_custo_total")),
                (List<ItemConsumoResponse>) saida.get("p_cursor"));
    }

    public List<IndicadorEstoqueResponse> listarIndicadores(Long hospitalId) {
        String ordenacao = " ORDER BY NVL(fn_dias_cobertura_estoque(ie.id), 999999), ie.nome";
        if (hospitalId == null) {
            return jdbcTemplate.query(SQL_INDICADORES + ordenacao, this::mapearIndicador);
        }
        return jdbcTemplate.query(SQL_INDICADORES + " WHERE ie.hospital_id = ?" + ordenacao,
                this::mapearIndicador, hospitalId);
    }

    public List<AlertaRegistradoResponse> listarAlertas(Long hospitalId) {
        String ordenacao = " ORDER BY a.criado_em DESC, a.id DESC";
        if (hospitalId == null) {
            return jdbcTemplate.query(SQL_ALERTAS + ordenacao, this::mapearAlerta);
        }
        return jdbcTemplate.query(SQL_ALERTAS + " WHERE a.hospital_id = ?" + ordenacao,
                this::mapearAlerta, hospitalId);
    }

    private IndicadorEstoqueResponse mapearIndicador(ResultSet rs, int linha) throws SQLException {
        return new IndicadorEstoqueResponse(
                rs.getLong("id"),
                rs.getString("nome"),
                rs.getString("hospital_nome"),
                rs.getBigDecimal("dias_cobertura"),
                rs.getString("status_formatado"));
    }

    private ItemConsumoResponse mapearItemConsumo(ResultSet rs, int linha) throws SQLException {
        return new ItemConsumoResponse(
                rs.getString("item"),
                rs.getInt("quantidade"),
                rs.getString("unidade"),
                rs.getBigDecimal("custo_unitario"),
                rs.getBigDecimal("custo_total"));
    }

    private AlertaRegistradoResponse mapearAlerta(ResultSet rs, int linha) throws SQLException {
        Timestamp criadoEm = rs.getTimestamp("criado_em");
        return new AlertaRegistradoResponse(
                rs.getLong("id"),
                rs.getObject("item_estoque_id", Long.class),
                rs.getString("item_nome"),
                rs.getObject("hospital_id", Long.class),
                AlertaTipo.valueOf(rs.getString("tipo")),
                rs.getString("mensagem"),
                rs.getString("origem"),
                criadoEm == null ? null : criadoEm.toLocalDateTime());
    }

    private BigDecimal numero(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }
}
