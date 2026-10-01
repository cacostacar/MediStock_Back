package br.com.fiap.medistockbackend.controller;

import br.com.fiap.medistockbackend.dto.HospitalDtos.HospitalRequest;
import br.com.fiap.medistockbackend.dto.HospitalDtos.HospitalResponse;
import br.com.fiap.medistockbackend.service.HospitalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospitais")
@RequiredArgsConstructor
@Tag(name = "Hospitais", description = "Rede de hospitais usada no estoque e na logistica/IA de redistribuicao")
public class HospitalController {

    private final HospitalService hospitalService;

    @GetMapping
    @Operation(summary = "Lista todos os hospitais da rede")
    public List<HospitalResponse> listar() {
        return hospitalService.listarTodos();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um hospital por id")
    public HospitalResponse buscar(@PathVariable Long id) {
        return hospitalService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo hospital na rede")
    public ResponseEntity<HospitalResponse> criar(@Valid @RequestBody HospitalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hospitalService.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de um hospital")
    public HospitalResponse atualizar(@PathVariable Long id, @Valid @RequestBody HospitalRequest request) {
        return hospitalService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um hospital da rede")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        hospitalService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
