package br.com.fiap.medistockbackend.service;

import br.com.fiap.medistockbackend.dto.AlertaResponse;
import br.com.fiap.medistockbackend.dto.ResumoAlertasResponse;
import br.com.fiap.medistockbackend.model.AlertaTipo;
import br.com.fiap.medistockbackend.repository.TransferenciaRepository;
import br.com.fiap.medistockbackend.model.StatusLogistico;
import br.com.fiap.medistockbackend.model.Transferencia;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertaService {

    private final FonteAlertasEstoque fonteAlertasEstoque;
    private final TransferenciaRepository transferenciaRepository;

    public List<AlertaResponse> listar(AlertaTipo filtro) {
        List<AlertaResponse> alertas = new ArrayList<>(fonteAlertasEstoque.listar());
        alertas.addAll(gerarAlertasLogisticos());

        if (filtro != null) {
            return alertas.stream().filter(a -> a.tipo() == filtro).toList();
        }
        return alertas;
    }

    private List<AlertaResponse> gerarAlertasLogisticos() {
        return transferenciaRepository.findAll().stream()
                .filter(transferencia -> transferencia.getStatus() == StatusLogistico.PENDENTE
                        || transferencia.getStatus() == StatusLogistico.EM_ROTA)
                .map(this::alertaDaTransferencia)
                .toList();
    }

    private AlertaResponse alertaDaTransferencia(Transferencia transferencia) {
        String status = transferencia.getStatus() == StatusLogistico.EM_ROTA ? "em rota" : "pendente";
        String origem = transferencia.getHospitalOrigem().getNome();
        String destino = transferencia.getHospitalDestino().getNome();
        String prefixo = transferencia.isGeradoPorIa() ? "Transferência sugerida pela IA" : "Transferência";

        return new AlertaResponse(
                transferencia.getItemEstoque().getId(),
                transferencia.getItemEstoque().getNome(),
                AlertaTipo.INFO,
                "%s %s: %s para %s (%d unidade(s), %s).".formatted(
                        prefixo, status, origem, destino, transferencia.getQuantidade(), status),
                destino,
                "Logística"
        );
    }

    public ResumoAlertasResponse resumo() {
        List<AlertaResponse> todos = listar(null);
        return new ResumoAlertasResponse(
                todos.stream().filter(a -> a.tipo() == AlertaTipo.CRITICO).count(),
                todos.stream().filter(a -> a.tipo() == AlertaTipo.ATENCAO).count(),
                todos.stream().filter(a -> a.tipo() == AlertaTipo.INFO).count()
        );
    }
}
