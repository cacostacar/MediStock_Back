package br.com.fiap.medistockbackend.persistence;

import br.com.fiap.medistockbackend.model.HistoricoConsumo;
import br.com.fiap.medistockbackend.repository.HistoricoConsumoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!oracle")
@RequiredArgsConstructor
public class HistoricoConsumoJpaWriter implements HistoricoConsumoWriter {

    private final HistoricoConsumoRepository historicoConsumoRepository;

    @Override
    public void salvar(HistoricoConsumo historico) {
        historicoConsumoRepository.save(historico);
    }
}
