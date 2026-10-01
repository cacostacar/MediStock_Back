package br.com.fiap.medistockbackend.dto;

import br.com.fiap.medistockbackend.model.ItemEstoque;
import br.com.fiap.medistockbackend.model.NivelEstoque;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Corresponde aos cards da tela "Estoque": nome, atual, min, status, local, barra de progresso. */
public record ItemEstoqueResponse(
        Long id,
        String nome,
        Integer quantidadeAtual,
        Integer quantidadeMinima,
        String unidadeMedida,
        String localArmazenamento,
        Long hospitalId,
        String hospitalNome,
        LocalDate validade,
        BigDecimal custoUnitario,
        boolean altoCustoBaixaDemanda,
        NivelEstoque nivel,
        boolean vencido,
        boolean validadeProxima,
        long diasParaVencer,
        double percentualEstoque // usado para a barra de progresso no front (0-100+)
) {
    public static ItemEstoqueResponse fromEntity(ItemEstoque item) {
        double percentual = item.getQuantidadeMinima() == 0
                ? 100
                : Math.min(100.0, (item.getQuantidadeAtual() * 100.0) / (item.getQuantidadeMinima() * 2.0));

        return new ItemEstoqueResponse(
                item.getId(),
                item.getNome(),
                item.getQuantidadeAtual(),
                item.getQuantidadeMinima(),
                item.getUnidadeMedida(),
                item.getLocalArmazenamento(),
                item.getHospital().getId(),
                item.getHospital().getNome(),
                item.getValidade(),
                item.getCustoUnitario(),
                item.isAltoCustoBaixaDemanda(),
                item.calcularNivel(),
                item.isVencido(),
                item.isValidadeProxima(),
                item.diasParaVencer(),
                percentual
        );
    }
}
