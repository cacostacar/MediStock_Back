package br.com.fiap.medistockbackend.service;

import br.com.fiap.medistockbackend.dto.AlertaResponse;
import br.com.fiap.medistockbackend.model.AlertaTipo;
import br.com.fiap.medistockbackend.model.ItemEstoque;
import br.com.fiap.medistockbackend.model.NivelEstoque;
import br.com.fiap.medistockbackend.repository.ItemEstoqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("!oracle")
@RequiredArgsConstructor
public class FonteAlertasEstoqueCalculada implements FonteAlertasEstoque {

    private static final long DIAS_LIMITE_ATENCAO_VALIDADE = 15;

    private final ItemEstoqueRepository itemEstoqueRepository;

    @Override
    public List<AlertaResponse> listar() {
        List<AlertaResponse> alertas = new ArrayList<>();
        for (ItemEstoque item : itemEstoqueRepository.findAll()) {
            alertas.addAll(gerarAlertasDoItem(item));
        }
        return alertas;
    }

    private List<AlertaResponse> gerarAlertasDoItem(ItemEstoque item) {
        List<AlertaResponse> alertas = new ArrayList<>();
        String hospitalNome = item.getHospital().getNome();

        NivelEstoque nivel = item.calcularNivel();
        if (nivel == NivelEstoque.CRITICO) {
            alertas.add(new AlertaResponse(item.getId(), item.getNome(), AlertaTipo.CRITICO,
                    "Estoque critico: %d %s (minimo %d)".formatted(
                            item.getQuantidadeAtual(), unidade(item), item.getQuantidadeMinima()),
                    hospitalNome, item.getLocalArmazenamento()));
        } else if (nivel == NivelEstoque.ATENCAO) {
            alertas.add(new AlertaResponse(item.getId(), item.getNome(), AlertaTipo.ATENCAO,
                    "Estoque em atencao: %d %s (minimo %d)".formatted(
                            item.getQuantidadeAtual(), unidade(item), item.getQuantidadeMinima()),
                    hospitalNome, item.getLocalArmazenamento()));
        }

        if (item.isVencido()) {
            alertas.add(new AlertaResponse(item.getId(), item.getNome(), AlertaTipo.CRITICO,
                    "Item vencido em " + item.getValidade(),
                    hospitalNome, item.getLocalArmazenamento()));
        } else if (item.isValidadeProxima()) {
            long dias = item.diasParaVencer();
            AlertaTipo tipoValidade = dias <= DIAS_LIMITE_ATENCAO_VALIDADE ? AlertaTipo.ATENCAO : AlertaTipo.INFO;
            alertas.add(new AlertaResponse(item.getId(), item.getNome(), tipoValidade,
                    "Validade proxima: vence em " + dias + " dia(s)",
                    hospitalNome, item.getLocalArmazenamento()));
        }

        return alertas;
    }

    private String unidade(ItemEstoque item) {
        return item.getUnidadeMedida() == null || item.getUnidadeMedida().isBlank()
                ? "un."
                : item.getUnidadeMedida();
    }
}
