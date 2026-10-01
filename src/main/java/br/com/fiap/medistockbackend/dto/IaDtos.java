package br.com.fiap.medistockbackend.dto;

import java.util.List;
import jakarta.validation.constraints.Positive;

public class IaDtos {

    public record HistoricoConsumoRequest(
            Long itemEstoqueId,
            Long hospitalId,
            String mesReferencia, // formato "yyyy-MM-dd" ou "yyyy-MM"
            Integer quantidadeConsumida
    ) {}

    /** Corresponde ao topo da tela IA: "Analise interna 36/100", Criticos/Prioritarios/Previsoes. */
    public record AnaliseInternaResponse(
            int scoreOtimizacao,      // 0-100
            String classificacao,     // OK, ATENCAO, CRITICO
            long itensCriticos,
            long itensPrioritarios,
            long previsoesGeradas,
            List<InsightItemResponse> insights
    ) {}

    /** Corresponde a cada card de item critico na tela IA. */
    public record InsightItemResponse(
            Long itemEstoqueId,
            String itemNome,
            String hospitalNome,
            int demandaProjetadaUnidades,
            int demandaProjetadaDias,
            double mediaMovelSimples,
            int sugestaoCompraUnidades,
            int confiancaPercentual
    ) {}

    /** Um hospital candidato a armazenar o insumo, com sua pontuacao. */
    public record CandidatoHospitalResponse(
            Long hospitalId,
            String hospitalNome,
            double demandaHistoricaMedia,
            double distanciaPonderadaKm,
            double pontuacao // menor = melhor (custo de distancia ponderado pela demanda)
    ) {}

    /** Se uma transferencia precisar ser feita, a rota mais rapida sugerida. */
    public record RotaSugeridaResponse(
            Long hospitalOrigemId,
            String hospitalOrigemNome,
            Long hospitalDestinoId,
            String hospitalDestinoNome,
            double distanciaKm,
            double tempoEstimadoMinutos
    ) {}

    public record RedistribuicaoResponse(
            Long itemEstoqueId,
            String itemNome,
            Long hospitalAtualId,
            String hospitalAtualNome,
            Long hospitalIdealId,
            String hospitalIdealNome,
            boolean necessitaTransferencia,
            RotaSugeridaResponse rotaSugerida,
            List<CandidatoHospitalResponse> candidatos,
            String justificativaIA
    ) {}

    /** Confirma uma sugestao da IA. Sem quantidade, transfere o saldo atual do item. */
    public record ConfirmarRedistribuicaoRequest(@Positive Integer quantidade) {}
}
