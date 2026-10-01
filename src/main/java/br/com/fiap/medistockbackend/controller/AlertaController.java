package br.com.fiap.medistockbackend.controller;

import br.com.fiap.medistockbackend.dto.AlertaResponse;
import br.com.fiap.medistockbackend.dto.ResumoAlertasResponse;
import br.com.fiap.medistockbackend.model.AlertaTipo;
import br.com.fiap.medistockbackend.service.AlertaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alertas")
@RequiredArgsConstructor
@Tag(name = "Alertas", description = "Tela 'Alertas' (Todos/Criticos/Atencao/Info) -- derivada do estoque em tempo real")
public class AlertaController {

    private final AlertaService alertaService;

    @GetMapping
    @Operation(summary = "Lista os alertas ativos (tipo opcional: CRITICO, ATENCAO ou INFO)")
    public List<AlertaResponse> listar(@RequestParam(required = false) AlertaTipo tipo) {
        return alertaService.listar(tipo);
    }

    @GetMapping("/resumo")
    @Operation(summary = "Contadores por tipo, usados no badge 'Alertas ativos' da Home")
    public ResumoAlertasResponse resumo() {
        return alertaService.resumo();
    }

}
