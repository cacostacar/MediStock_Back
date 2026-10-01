package br.com.fiap.medistockbackend.controller;

import br.com.fiap.medistockbackend.dto.LogisticaDtos.*;
import br.com.fiap.medistockbackend.model.StatusLogistico;
import br.com.fiap.medistockbackend.service.LogisticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logistica")
@RequiredArgsConstructor
@Tag(name = "Logistica", description = "Tela 'Logistica' -- Entregas e Transferencias entre hospitais da rede")
public class LogisticaController {

    private final LogisticaService logisticaService;

    @GetMapping("/entregas")
    @Operation(summary = "Lista as entregas (fornecedor -> hospital)")
    public List<EntregaResponse> listarEntregas() {
        return logisticaService.listarEntregas();
    }

    @PostMapping("/entregas")
    @Operation(summary = "Registra uma nova entrega")
    public ResponseEntity<EntregaResponse> criarEntrega(@Valid @RequestBody EntregaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(logisticaService.criarEntrega(request));
    }

    @PatchMapping("/entregas/{id}/status")
    @Operation(summary = "Atualiza o status de uma entrega (PENDENTE, EM_ROTA, CONCLUIDA, CANCELADA)")
    public EntregaResponse atualizarStatusEntrega(@PathVariable Long id, @RequestParam StatusLogistico status) {
        return logisticaService.atualizarStatusEntrega(id, status);
    }

    @GetMapping("/transferencias")
    @Operation(summary = "Lista as transferencias entre hospitais (manuais ou geradas pela IA)")
    public List<TransferenciaResponse> listarTransferencias() {
        return logisticaService.listarTransferencias();
    }

    @GetMapping("/mapa")
    @Operation(summary = "Retorna hospitais geolocalizados e rotas das transferencias pendentes ou em rota")
    public LogisticaMapaResponse mapa() {
        return logisticaService.obterMapa();
    }

    @PostMapping("/transferencias")
    @Operation(summary = "Registra manualmente uma transferencia entre dois hospitais")
    public ResponseEntity<TransferenciaResponse> criarTransferencia(@Valid @RequestBody TransferenciaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(logisticaService.criarTransferencia(request));
    }

    @PatchMapping("/transferencias/{id}/status")
    @Operation(summary = "Atualiza o status de uma transferencia")
    public TransferenciaResponse atualizarStatusTransferencia(@PathVariable Long id, @RequestParam StatusLogistico status) {
        return logisticaService.atualizarStatusTransferencia(id, status);
    }
}
