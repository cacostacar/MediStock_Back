package br.com.fiap.medistockbackend.dto;

import br.com.fiap.medistockbackend.model.AlertaTipo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

public class PlsqlDtos {

    public record ProcessamentoAlertasResponse(
            Long hospitalId,
            int alertasRegistrados
    ) {}

    public record AlertaRegistradoResponse(
            Long id,
            Long itemEstoqueId,
            String itemNome,
            Long hospitalId,
            AlertaTipo tipo,
            String mensagem,
            String origem,
            LocalDateTime criadoEm
    ) {}

    public record IndicadorEstoqueResponse(
            Long itemEstoqueId,
            String itemNome,
            String hospitalNome,
            BigDecimal diasCobertura,
            String statusFormatado
    ) {}

    public record ItemConsumoResponse(
            String item,
            int quantidade,
            String unidade,
            BigDecimal custoUnitario,
            BigDecimal custoTotal
    ) {}

    public record RelatorioConsumoResponse(
            Long hospitalId,
            YearMonth mesReferencia,
            int totalItens,
            long totalConsumido,
            BigDecimal custoTotal,
            List<ItemConsumoResponse> itens
    ) {}
}
