package br.com.fiap.medistockbackend.controller;

import br.com.fiap.medistockbackend.dto.PlsqlDtos.AlertaRegistradoResponse;
import br.com.fiap.medistockbackend.dto.PlsqlDtos.IndicadorEstoqueResponse;
import br.com.fiap.medistockbackend.dto.PlsqlDtos.ProcessamentoAlertasResponse;
import br.com.fiap.medistockbackend.dto.PlsqlDtos.RelatorioConsumoResponse;
import br.com.fiap.medistockbackend.service.PlsqlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/plsql")
@Profile("oracle")
@RequiredArgsConstructor
@Tag(name = "PL/SQL (Oracle)", description = "Procedures e functions executadas no Oracle via JDBC")
public class PlsqlController {

    private final PlsqlService plsqlService;

    @PostMapping("/alertas/processar")
    @Operation(summary = "Executa prc_registrar_alertas_criticos (hospitalId opcional; sem ele processa todos)")
    public ProcessamentoAlertasResponse processarAlertas(@RequestParam(required = false) Long hospitalId) {
        return plsqlService.processarAlertas(hospitalId);
    }

    @GetMapping("/alertas")
    @Operation(summary = "Lista os alertas persistidos pela procedure na tabela ALERTAS")
    public List<AlertaRegistradoResponse> listarAlertas(@RequestParam(required = false) Long hospitalId) {
        return plsqlService.listarAlertas(hospitalId);
    }

    @GetMapping("/estoque/indicadores")
    @Operation(summary = "Consulta SQL com fn_dias_cobertura_estoque e fn_status_estoque_formatado")
    public List<IndicadorEstoqueResponse> listarIndicadores(@RequestParam(required = false) Long hospitalId) {
        return plsqlService.listarIndicadores(hospitalId);
    }

    @GetMapping("/relatorios/consumo/{hospitalId}")
    @Operation(summary = "Executa prc_relatorio_consumo_hospital (mes no formato AAAA-MM; padrao: mes atual)")
    public RelatorioConsumoResponse relatorioConsumo(
            @PathVariable Long hospitalId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return plsqlService.gerarRelatorioConsumo(hospitalId, mes);
    }
}
