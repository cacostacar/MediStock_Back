package br.com.fiap.medistockbackend.service;

import br.com.fiap.medistockbackend.dto.PlsqlDtos.AlertaRegistradoResponse;
import br.com.fiap.medistockbackend.dto.PlsqlDtos.IndicadorEstoqueResponse;
import br.com.fiap.medistockbackend.dto.PlsqlDtos.ProcessamentoAlertasResponse;
import br.com.fiap.medistockbackend.dto.PlsqlDtos.RelatorioConsumoResponse;
import br.com.fiap.medistockbackend.event.EstoqueAlteradoEvent;
import br.com.fiap.medistockbackend.exception.ResourceNotFoundException;
import br.com.fiap.medistockbackend.repository.PlsqlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.sql.SQLException;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Profile("oracle")
@RequiredArgsConstructor
public class PlsqlService {

    private static final Set<Integer> CODIGOS_NAO_ENCONTRADO = Set.of(20101, 20120);

    private final PlsqlRepository plsqlRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void aoAlterarEstoque(EstoqueAlteradoEvent evento) {
        try {
            int total = plsqlRepository.registrarAlertasCriticos(evento.hospitalId());
            log.info("prc_registrar_alertas_criticos acionada pelo item {} (hospital {}): {} alerta(s) novo(s)",
                    evento.itemEstoqueId(), evento.hospitalId(), total);
        } catch (DataAccessException ex) {
            log.error("Falha ao acionar prc_registrar_alertas_criticos para o hospital {}",
                    evento.hospitalId(), ex);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "${medistock.plsql.alertas.cron:0 5 0 * * *}")
    @Transactional
    public void processarRotinaDeAlertas() {
        try {
            int total = plsqlRepository.registrarAlertasCriticos(null);
            log.info("Rotina de alertas PL/SQL executada: {} alerta(s) novo(s)", total);
        } catch (DataAccessException ex) {
            log.error("Falha na rotina de alertas PL/SQL", ex);
        }
    }

    @Transactional
    public ProcessamentoAlertasResponse processarAlertas(Long hospitalId) {
        return new ProcessamentoAlertasResponse(hospitalId, plsqlRepository.registrarAlertasCriticos(hospitalId));
    }

    @Transactional(readOnly = true)
    public List<AlertaRegistradoResponse> listarAlertas(Long hospitalId) {
        return plsqlRepository.listarAlertas(hospitalId);
    }

    @Transactional(readOnly = true)
    public List<IndicadorEstoqueResponse> listarIndicadores(Long hospitalId) {
        return plsqlRepository.listarIndicadores(hospitalId);
    }

    @Transactional(readOnly = true)
    public RelatorioConsumoResponse gerarRelatorioConsumo(Long hospitalId, YearMonth mes) {
        try {
            return plsqlRepository.gerarRelatorioConsumo(hospitalId, mes != null ? mes : YearMonth.now());
        } catch (DataAccessException ex) {
            throw traduzirErroPlsql(ex);
        }
    }

    private RuntimeException traduzirErroPlsql(DataAccessException ex) {
        if (ex.getCause() instanceof SQLException sql
                && CODIGOS_NAO_ENCONTRADO.contains(sql.getErrorCode())) {
            return new ResourceNotFoundException(mensagemPlsql(sql));
        }
        return ex;
    }

    private String mensagemPlsql(SQLException sql) {
        String primeiraLinha = sql.getMessage().lines().findFirst().orElse("");
        int separador = primeiraLinha.indexOf(": ");
        return separador >= 0 ? primeiraLinha.substring(separador + 2) : primeiraLinha;
    }
}
