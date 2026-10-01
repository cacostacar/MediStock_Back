package br.com.fiap.medistockbackend.controller;

import br.com.fiap.medistockbackend.dto.ItemEstoqueRequest;
import br.com.fiap.medistockbackend.dto.ItemEstoqueResponse;
import br.com.fiap.medistockbackend.dto.ResumoEstoqueResponse;
import br.com.fiap.medistockbackend.model.NivelEstoque;
import br.com.fiap.medistockbackend.service.ItemEstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estoque")
@RequiredArgsConstructor
@Tag(name = "Estoque", description = "CRUD de insumos hospitalares + tela 'Estoque' / cards da Home")
public class ItemEstoqueController {

    private final ItemEstoqueService itemEstoqueService;

    @GetMapping
    @Operation(summary = "Lista os itens de estoque (filtros opcionais por hospital e nivel)")
    public List<ItemEstoqueResponse> listar(
            @RequestParam(required = false) Long hospitalId,
            @RequestParam(required = false) NivelEstoque nivel) {
        return itemEstoqueService.listar(hospitalId, nivel);
    }

    @GetMapping("/resumo")
    @Operation(summary = "Contadores para a Home: total de itens, criticos, atencao, validade proxima")
    public ResumoEstoqueResponse resumo() {
        return itemEstoqueService.resumo();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um item de estoque por id")
    public ItemEstoqueResponse buscar(@PathVariable Long id) {
        return itemEstoqueService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo item de estoque")
    public ResponseEntity<ItemEstoqueResponse> criar(@Valid @RequestBody ItemEstoqueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemEstoqueService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um item de estoque (ex: dar entrada/saida de quantidade)")
    public ItemEstoqueResponse atualizar(@PathVariable Long id, @Valid @RequestBody ItemEstoqueRequest request) {
        return itemEstoqueService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um item de estoque")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        itemEstoqueService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
