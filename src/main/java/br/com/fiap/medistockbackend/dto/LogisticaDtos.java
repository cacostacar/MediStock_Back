package br.com.fiap.medistockbackend.dto;

import br.com.fiap.medistockbackend.model.Entrega;
import br.com.fiap.medistockbackend.model.StatusLogistico;
import br.com.fiap.medistockbackend.model.Transferencia;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class LogisticaDtos {

    // Entregas

    public record EntregaRequest(
            @NotNull Long itemEstoqueId,
            @NotNull Long hospitalDestinoId,
            @NotNull @Positive Integer quantidade,
            LocalDate dataPrevista,
            String transportadora
    ) {}

    public record EntregaResponse(
            Long id,
            String itemNome,
            String hospitalDestinoNome,
            Integer quantidade,
            StatusLogistico status,
            LocalDate dataPrevista,
            String transportadora
    ) {
        public static EntregaResponse fromEntity(Entrega e) {
            return new EntregaResponse(
                    e.getId(),
                    e.getItemEstoque().getNome(),
                    e.getHospitalDestino().getNome(),
                    e.getQuantidade(),
                    e.getStatus(),
                    e.getDataPrevista(),
                    e.getTransportadora()
            );
        }
    }

    // Transferencias

    public record TransferenciaRequest(
            @NotNull Long itemEstoqueId,
            @NotNull Long hospitalOrigemId,
            @NotNull Long hospitalDestinoId,
            @NotNull @Positive Integer quantidade,
            @Size(max = Transferencia.TAMANHO_MAXIMO_MOTIVO) String motivo
    ) {}

    public record TransferenciaResponse(
            Long id,
            String itemNome,
            String hospitalOrigemNome,
            String hospitalDestinoNome,
            Integer quantidade,
            StatusLogistico status,
            Double distanciaKm,
            Double tempoEstimadoMinutos,
            String motivo,
            boolean geradoPorIa
    ) {
        public static TransferenciaResponse fromEntity(Transferencia t) {
            return new TransferenciaResponse(
                    t.getId(),
                    t.getItemEstoque().getNome(),
                    t.getHospitalOrigem().getNome(),
                    t.getHospitalDestino().getNome(),
                    t.getQuantidade(),
                    t.getStatus(),
                    t.getDistanciaKm(),
                    t.getTempoEstimadoMinutos(),
                    t.getMotivo(),
                    t.isGeradoPorIa()
            );
        }
    }

    // Mapa (tela Logistica) 

    public record HospitalMapaPonto(
            Long id,
            String nome,
            String cidade,
            Double latitude,
            Double longitude,
            long itensCriticos
    ) {}

    public record TransferenciaMapaResponse(
            Long id,
            String itemNome,
            HospitalMapaPonto origem,
            HospitalMapaPonto destino,
            double distanciaKm,
            double tempoEstimadoMinutos,
            StatusLogistico status,
            boolean geradoPorIa,
            String motivo
    ) {}

    public record LogisticaMapaResponse(
            List<HospitalMapaPonto> hospitais,
            List<TransferenciaMapaResponse> transferenciasAtivas
    ) {}
}