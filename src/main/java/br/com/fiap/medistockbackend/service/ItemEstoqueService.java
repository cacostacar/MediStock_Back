package br.com.fiap.medistockbackend.service;

import br.com.fiap.medistockbackend.dto.ItemEstoqueRequest;
import br.com.fiap.medistockbackend.dto.ItemEstoqueResponse;
import br.com.fiap.medistockbackend.dto.ResumoEstoqueResponse;
import br.com.fiap.medistockbackend.event.EstoqueAlteradoEvent;
import br.com.fiap.medistockbackend.exception.ResourceNotFoundException;
import br.com.fiap.medistockbackend.model.Hospital;
import br.com.fiap.medistockbackend.model.ItemEstoque;
import br.com.fiap.medistockbackend.model.NivelEstoque;
import br.com.fiap.medistockbackend.repository.ItemEstoqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemEstoqueService {

    private final ItemEstoqueRepository itemEstoqueRepository;
    private final HospitalService hospitalService;
    private final ApplicationEventPublisher eventPublisher;

    public List<ItemEstoqueResponse> listar(Long hospitalId, NivelEstoque nivel) {
        List<ItemEstoque> itens = hospitalId != null
                ? itemEstoqueRepository.findByHospitalId(hospitalId)
                : itemEstoqueRepository.findAll();

        return itens.stream()
                .filter(item -> nivel == null || item.calcularNivel() == nivel)
                .map(ItemEstoqueResponse::fromEntity)
                .toList();
    }

    public ItemEstoqueResponse buscarPorId(Long id) {
        return ItemEstoqueResponse.fromEntity(buscarEntidade(id));
    }

    @Transactional
    public ItemEstoqueResponse criar(ItemEstoqueRequest request) {
        Hospital hospital = hospitalService.buscarEntidade(request.hospitalId());

        ItemEstoque item = ItemEstoque.builder()
                .nome(request.nome())
                .quantidadeAtual(request.quantidadeAtual())
                .quantidadeMinima(request.quantidadeMinima())
                .unidadeMedida(request.unidadeMedida())
                .localArmazenamento(request.localArmazenamento())
                .hospital(hospital)
                .validade(request.validade())
                .custoUnitario(request.custoUnitario())
                .altoCustoBaixaDemanda(request.altoCustoBaixaDemanda())
                .build();

        ItemEstoque salvo = itemEstoqueRepository.save(item);
        publicarAlteracao(salvo);
        return ItemEstoqueResponse.fromEntity(salvo);
    }

    @Transactional
    public ItemEstoqueResponse atualizar(Long id, ItemEstoqueRequest request) {
        ItemEstoque item = buscarEntidade(id);
        Hospital hospital = hospitalService.buscarEntidade(request.hospitalId());

        item.setNome(request.nome());
        item.setQuantidadeAtual(request.quantidadeAtual());
        item.setQuantidadeMinima(request.quantidadeMinima());
        item.setUnidadeMedida(request.unidadeMedida());
        item.setLocalArmazenamento(request.localArmazenamento());
        item.setHospital(hospital);
        item.setValidade(request.validade());
        item.setCustoUnitario(request.custoUnitario());
        item.setAltoCustoBaixaDemanda(request.altoCustoBaixaDemanda());

        publicarAlteracao(item);
        return ItemEstoqueResponse.fromEntity(item);
    }

    @Transactional
    public void excluir(Long id) {
        if (!itemEstoqueRepository.existsById(id)) {
            throw new ResourceNotFoundException("Item de estoque nao encontrado: id " + id);
        }
        itemEstoqueRepository.deleteById(id);
    }

    public ResumoEstoqueResponse resumo() {
        List<ItemEstoque> todos = itemEstoqueRepository.findAll();

        long criticos = todos.stream().filter(i -> i.calcularNivel() == NivelEstoque.CRITICO).count();
        long atencao = todos.stream().filter(i -> i.calcularNivel() == NivelEstoque.ATENCAO).count();
        long validadeProxima = todos.stream().filter(ItemEstoque::isValidadeProxima).count();

        return new ResumoEstoqueResponse(todos.size(), criticos, atencao, validadeProxima);
    }

    private void publicarAlteracao(ItemEstoque item) {
        eventPublisher.publishEvent(new EstoqueAlteradoEvent(item.getId(), item.getHospital().getId()));
    }

    ItemEstoque buscarEntidade(Long id) {
        return itemEstoqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item de estoque nao encontrado: id " + id));
    }
}

