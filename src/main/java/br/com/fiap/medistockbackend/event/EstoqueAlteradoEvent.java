package br.com.fiap.medistockbackend.event;

public record EstoqueAlteradoEvent(Long itemEstoqueId, Long hospitalId) {}
