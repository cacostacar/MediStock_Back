package br.com.fiap.medistockbackend.controller;

import br.com.fiap.medistockbackend.dto.IaDtos.HistoricoConsumoRequest;
import br.com.fiap.medistockbackend.service.HistoricoConsumoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/historico-consumo")
@RequiredArgsConstructor
@Tag(name = "Historico de Consumo", description = "Alimenta os calculos da IA (media movel, demanda por hospital)")
public class HistoricoConsumoController {

    private final HistoricoConsumoService historicoConsumoService;

    @PostMapping
    @Operation(summary = "Registra um mes de consumo de um item em um hospital (usado para treinar as previsoes da IA)")
    public ResponseEntity<Void> registrar(@RequestBody HistoricoConsumoRequest request) {
        historicoConsumoService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
