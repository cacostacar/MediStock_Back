package br.com.fiap.medistockbackend.persistence;

import br.com.fiap.medistockbackend.model.HistoricoConsumo;
import br.com.fiap.medistockbackend.repository.HistoricoConsumoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HistoricoConsumoJpaWriterTest {

    @Mock
    private HistoricoConsumoRepository repository;

    @InjectMocks
    private HistoricoConsumoJpaWriter jpaWriter;

    @Test
    void deveSalvarViaRepository() {
        HistoricoConsumo historico = new HistoricoConsumo();
        jpaWriter.salvar(historico);
        verify(repository).save(historico);
    }
}
