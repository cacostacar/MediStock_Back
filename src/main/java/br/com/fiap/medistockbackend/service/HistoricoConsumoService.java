package br.com.fiap.medistockbackend.service;

import br.com.fiap.medistockbackend.dto.IaDtos.HistoricoConsumoRequest;
import br.com.fiap.medistockbackend.exception.BusinessRuleException;
import br.com.fiap.medistockbackend.model.Hospital;
import br.com.fiap.medistockbackend.model.HistoricoConsumo;
import br.com.fiap.medistockbackend.model.ItemEstoque;
import br.com.fiap.medistockbackend.persistence.HistoricoConsumoWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class HistoricoConsumoService {

    private final HistoricoConsumoWriter historicoConsumoWriter;
    private final ItemEstoqueService itemEstoqueService;
    private final HospitalService hospitalService;

    @Transactional
    public void registrar(HistoricoConsumoRequest request) {
        validar(request);

        LocalDate mesReferencia = parseMes(request.mesReferencia());

        ItemEstoque item = itemEstoqueService.buscarEntidade(request.itemEstoqueId());
        Hospital hospital = hospitalService.buscarEntidade(request.hospitalId());

        HistoricoConsumo historico = HistoricoConsumo.builder()
                .itemEstoque(item)
                .hospital(hospital)
                .mesReferencia(mesReferencia)
                .quantidadeConsumida(request.quantidadeConsumida())
                .build();

        historicoConsumoWriter.salvar(historico);
    }

    private void validar(HistoricoConsumoRequest request) {
        if (request == null) {
            throw new BusinessRuleException("Dados da requisição são obrigatórios");
        }
        if (request.itemEstoqueId() == null) {
            throw new BusinessRuleException("O ID do item de estoque é obrigatório");
        }
        if (request.itemEstoqueId() <= 0) {
            throw new BusinessRuleException("O ID do item de estoque deve ser positivo");
        }
        if (request.hospitalId() == null) {
            throw new BusinessRuleException("O ID do hospital é obrigatório");
        }
        if (request.hospitalId() <= 0) {
            throw new BusinessRuleException("O ID do hospital deve ser positivo");
        }
        if (request.quantidadeConsumida() == null) {
            throw new BusinessRuleException("A quantidade consumida é obrigatória");
        }
        if (request.quantidadeConsumida() < 0) {
            throw new BusinessRuleException("A quantidade consumida não pode ser negativa");
        }
        if (request.mesReferencia() == null || request.mesReferencia().isBlank()) {
            throw new BusinessRuleException("O mês de referência é obrigatório");
        }
    }

    private LocalDate parseMes(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new BusinessRuleException("O mês de referência é obrigatório");
        }
        String v = valor.trim();
        try {
            if (v.length() == 7) {
                return LocalDate.parse(v + "-01");
            } else if (v.length() == 10) {
                return LocalDate.parse(v, DateTimeFormatter.ISO_LOCAL_DATE);
            } else {
                throw new BusinessRuleException("Formato de data inválido para mês de referência. Use yyyy-MM ou yyyy-MM-dd");
            }
        } catch (DateTimeParseException ex) {
            throw new BusinessRuleException("Data inválida para mês de referência: " + valor);
        }
    }
}
