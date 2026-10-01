package br.com.fiap.medistockbackend.service;

import br.com.fiap.medistockbackend.dto.IaDtos.AnaliseInternaResponse;
import br.com.fiap.medistockbackend.dto.IaDtos.InsightItemResponse;
import br.com.fiap.medistockbackend.model.HistoricoConsumo;
import br.com.fiap.medistockbackend.model.ItemEstoque;
import br.com.fiap.medistockbackend.model.NivelEstoque;
import br.com.fiap.medistockbackend.repository.HistoricoConsumoRepository;
import br.com.fiap.medistockbackend.repository.ItemEstoqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnaliseInternaService {

    private static final int JANELA_MESES_MEDIA_MOVEL = 3;
    private static final int DIAS_PROJECAO = 30;

    private final ItemEstoqueRepository itemEstoqueRepository;
    private final HistoricoConsumoRepository historicoConsumoRepository;

    @Transactional(readOnly = true)
    public AnaliseInternaResponse analisar() {
        List<ItemEstoque> todos = itemEstoqueRepository.findAll();

        long criticos = todos.stream().filter(i -> i.calcularNivel() == NivelEstoque.CRITICO).count();
        long prioritarios = todos.stream()
                .filter(i -> i.isAltoCustoBaixaDemanda() || i.calcularNivel() != NivelEstoque.NORMAL)
                .count();

        List<InsightItemResponse> insights = todos.stream()
                .filter(i -> i.calcularNivel() == NivelEstoque.CRITICO)
                .map(this::gerarInsight)
                .sorted(Comparator.comparingInt(InsightItemResponse::confiancaPercentual).reversed())
                .toList();

        long previsoesGeradas = insights.stream().filter(i -> i.confiancaPercentual() > 0).count();

        int score = calcularScore(todos.size(), criticos, prioritarios);

        return new AnaliseInternaResponse(
                score,
                classificar(score),
                criticos,
                prioritarios,
                previsoesGeradas,
                insights
        );
    }

    private InsightItemResponse gerarInsight(ItemEstoque item) {
        List<HistoricoConsumo> historico = historicoConsumoRepository
                .findByItemEstoqueIdOrderByMesReferenciaDesc(item.getId())
                .stream()
                .limit(JANELA_MESES_MEDIA_MOVEL)
                .toList();

        if (historico.isEmpty()) {
            return new InsightItemResponse(item.getId(), item.getNome(), item.getHospital().getNome(),
                    0, DIAS_PROJECAO, 0.0, Math.max(0, item.getQuantidadeMinima() - item.getQuantidadeAtual()), 0);
        }

        double mediaMovel = historico.stream()
                .mapToInt(HistoricoConsumo::getQuantidadeConsumida)
                .average()
                .orElse(0.0);

        int demandaProjetada = (int) Math.round(mediaMovel);
        int sugestaoCompra = Math.max(0, (demandaProjetada + item.getQuantidadeMinima()) - item.getQuantidadeAtual());
        int confianca = Math.min(100, historico.size() * 25);

        return new InsightItemResponse(
                item.getId(), item.getNome(), item.getHospital().getNome(),
                demandaProjetada, DIAS_PROJECAO, arredondar(mediaMovel), sugestaoCompra, confianca
        );
    }

    private int calcularScore(int totalItens, long criticos, long prioritarios) {
        if (totalItens == 0) {
            return 100;
        }
        double penalidadeCriticos = (criticos / (double) totalItens) * 70;
        double penalidadePrioritarios = (prioritarios / (double) totalItens) * 30;
        int score = (int) Math.round(100 - penalidadeCriticos - penalidadePrioritarios);
        return Math.max(0, Math.min(100, score));
    }

    private String classificar(int score) {
        if (score >= 70) return "OK";
        if (score >= 40) return "ATENCAO";
        return "CRITICO";
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
