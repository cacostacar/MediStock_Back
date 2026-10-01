package br.com.fiap.medistockbackend.persistence;

import br.com.fiap.medistockbackend.exception.BusinessRuleException;
import br.com.fiap.medistockbackend.exception.ResourceNotFoundException;
import br.com.fiap.medistockbackend.model.HistoricoConsumo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.CallableStatementCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Profile("oracle")
@Slf4j
public class HistoricoConsumoOracleWriter implements HistoricoConsumoWriter {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public HistoricoConsumoOracleWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public HistoricoConsumoOracleWriter(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void salvar(HistoricoConsumo historico) {
        try {
            jdbcTemplate.execute(
                "{call PR_REGISTRAR_CONSUMO(?, ?, ?, ?)}",
                (CallableStatementCallback<Void>) cs -> {
                    cs.setLong(1, historico.getItemEstoque().getId());
                    cs.setLong(2, historico.getHospital().getId());
                    cs.setDate(3, java.sql.Date.valueOf(historico.getMesReferencia()));
                    cs.setInt(4, historico.getQuantidadeConsumida());
                    cs.execute();
                    return null;
                }
            );
            log.info("PR_REGISTRAR_CONSUMO executada");
        } catch (DataAccessException ex) {
            tratarExcecaoOracle(ex);
            throw ex;
        }
    }

    private void tratarExcecaoOracle(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SQLException sqlEx) {
                verificarSQLException(sqlEx);
            }
            current = current.getCause();
        }
    }

    private void verificarSQLException(SQLException sqlEx) {
        SQLException currentSql = sqlEx;
        while (currentSql != null) {
            int errorCode = Math.abs(currentSql.getErrorCode());
            if (errorCode == 0 && currentSql.getMessage() != null) {
                Matcher matcher = Pattern.compile("ORA-(\\d{5})").matcher(currentSql.getMessage());
                if (matcher.find()) {
                    try {
                        errorCode = Integer.parseInt(matcher.group(1));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            if (errorCode >= 20001 && errorCode <= 20003) {
                throw new BusinessRuleException(extrairMensagem(currentSql));
            } else if (errorCode == 20004 || errorCode == 20005) {
                throw new ResourceNotFoundException(extrairMensagem(currentSql));
            }

            currentSql = currentSql.getNextException();
        }
    }

    private String extrairMensagem(SQLException sqlEx) {
        String msg = sqlEx.getMessage();
        if (msg == null || msg.isBlank()) {
            return "Erro na execução da procedure Oracle";
        }
        for (String line : msg.split("\\R")) {
            line = line.trim();
            if (line.matches("^ORA-\\d{5}:.*")) {
                int colonIndex = line.indexOf(':');
                if (colonIndex != -1 && colonIndex + 1 < line.length()) {
                    String limpo = line.substring(colonIndex + 1).trim();
                    if (!limpo.isEmpty()) {
                        return limpo;
                    }
                }
                return line;
            }
        }
        return msg;
    }
}
