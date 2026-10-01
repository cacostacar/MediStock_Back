package br.com.fiap.medistockbackend.dto;

import br.com.fiap.medistockbackend.model.AlertaTipo;

public record AlertaResponse(
        Long itemEstoqueId,
        String itemNome,
        AlertaTipo tipo,
        String mensagem,
        String hospitalNome,
        String localArmazenamento
) {}
