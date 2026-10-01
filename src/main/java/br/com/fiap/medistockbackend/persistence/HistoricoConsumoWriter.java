package br.com.fiap.medistockbackend.persistence;

import br.com.fiap.medistockbackend.model.HistoricoConsumo;

public interface HistoricoConsumoWriter {
    void salvar(HistoricoConsumo historico);
}
