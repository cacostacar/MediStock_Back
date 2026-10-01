package br.com.fiap.medistockbackend.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemEstoqueRequest(

        @NotBlank(message = "Nome do item e obrigatorio")
        @Size(max = 150)
        String nome,

        @NotNull(message = "Quantidade atual e obrigatoria")
        @PositiveOrZero(message = "Quantidade atual nao pode ser negativa")
        Integer quantidadeAtual,

        @NotNull(message = "Quantidade minima e obrigatoria")
        @PositiveOrZero(message = "Quantidade minima nao pode ser negativa")
        Integer quantidadeMinima,

        @Size(max = 20)
        String unidadeMedida,

        @Size(max = 150)
        String localArmazenamento,

        @NotNull(message = "Hospital e obrigatorio")
        Long hospitalId,

        LocalDate validade,

        @PositiveOrZero
        BigDecimal custoUnitario,

        boolean altoCustoBaixaDemanda

) {}
