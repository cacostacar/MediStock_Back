package br.com.fiap.medistockbackend.service;

import br.com.fiap.medistockbackend.dto.AlertaResponse;
import br.com.fiap.medistockbackend.repository.PlsqlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile("oracle")
@RequiredArgsConstructor
public class FonteAlertasEstoqueOracle implements FonteAlertasEstoque {

    private final PlsqlRepository plsqlRepository;

    @Override
    public List<AlertaResponse> listar() {
        return plsqlRepository.listarAlertasVigentes();
    }
}
