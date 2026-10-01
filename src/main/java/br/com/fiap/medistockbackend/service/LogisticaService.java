package br.com.fiap.medistockbackend.service;

import br.com.fiap.medistockbackend.dto.LogisticaDtos.*;
import br.com.fiap.medistockbackend.exception.ResourceNotFoundException;
import br.com.fiap.medistockbackend.model.*;
import br.com.fiap.medistockbackend.repository.EntregaRepository;
import br.com.fiap.medistockbackend.repository.HospitalRepository;
import br.com.fiap.medistockbackend.repository.ItemEstoqueRepository;
import br.com.fiap.medistockbackend.repository.TransferenciaRepository;
import br.com.fiap.medistockbackend.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogisticaService {

    private final EntregaRepository entregaRepository;
    private final TransferenciaRepository transferenciaRepository;
    private final HospitalRepository hospitalRepository;
    private final ItemEstoqueRepository itemEstoqueRepository;
    private final ItemEstoqueService itemEstoqueService;
    private final HospitalService hospitalService;

    @Transactional(readOnly = true)
    public List<EntregaResponse> listarEntregas() {
        return entregaRepository.findAll().stream().map(EntregaResponse::fromEntity).toList();
    }

    @Transactional
    public EntregaResponse criarEntrega(EntregaRequest request) {
        ItemEstoque item = itemEstoqueService.buscarEntidade(request.itemEstoqueId());
        Hospital destino = hospitalService.buscarEntidade(request.hospitalDestinoId());

        Entrega entrega = Entrega.builder()
                .itemEstoque(item)
                .hospitalDestino(destino)
                .quantidade(request.quantidade())
                .dataPrevista(request.dataPrevista())
                .transportadora(request.transportadora())
                .status(StatusLogistico.EM_ROTA)
                .build();

        return EntregaResponse.fromEntity(entregaRepository.save(entrega));
    }

    @Transactional
    public EntregaResponse atualizarStatusEntrega(Long id, StatusLogistico novoStatus) {
        Entrega entrega = entregaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entrega nao encontrada: id " + id));
        entrega.setStatus(novoStatus);
        return EntregaResponse.fromEntity(entrega);
    }

    @Transactional(readOnly = true)
    public List<TransferenciaResponse> listarTransferencias() {
        return transferenciaRepository.findAll().stream().map(TransferenciaResponse::fromEntity).toList();
    }

    /** Dados prontos para um mapa: marcadores de hospitais e linhas de transferencias em aberto. */
    @Transactional(readOnly = true)
    public LogisticaMapaResponse obterMapa() {
        Map<Long, HospitalMapaPonto> hospitaisPorId = hospitalRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Hospital::getId,
                        hospital -> new HospitalMapaPonto(
                                hospital.getId(), hospital.getNome(), hospital.getCidade(),
                                hospital.getLatitude(), hospital.getLongitude(),
                                itemEstoqueRepository.findByHospitalId(hospital.getId()).stream()
                                        .filter(item -> item.calcularNivel() == NivelEstoque.CRITICO)
                                        .count()
                        )
                ));

        List<TransferenciaMapaResponse> transferenciasAtivas = transferenciaRepository.findAll().stream()
                .filter(transferencia -> transferencia.getStatus() == StatusLogistico.PENDENTE
                        || transferencia.getStatus() == StatusLogistico.EM_ROTA)
                .map(transferencia -> new TransferenciaMapaResponse(
                        transferencia.getId(),
                        transferencia.getItemEstoque().getNome(),
                        hospitaisPorId.get(transferencia.getHospitalOrigem().getId()),
                        hospitaisPorId.get(transferencia.getHospitalDestino().getId()),
                        transferencia.getDistanciaKm(),
                        transferencia.getTempoEstimadoMinutos(),
                        transferencia.getStatus(),
                        transferencia.isGeradoPorIa(),
                        transferencia.getMotivo()
                ))
                .toList();

        return new LogisticaMapaResponse(List.copyOf(hospitaisPorId.values()), transferenciasAtivas);
    }

    @Transactional
    public TransferenciaResponse criarTransferencia(TransferenciaRequest request) {
        ItemEstoque item = itemEstoqueService.buscarEntidade(request.itemEstoqueId());
        Hospital origem = hospitalService.buscarEntidade(request.hospitalOrigemId());
        Hospital destino = hospitalService.buscarEntidade(request.hospitalDestinoId());

        double distanciaKm = GeoUtils.distanciaKm(
                origem.getLatitude(), origem.getLongitude(),
                destino.getLatitude(), destino.getLongitude());
        double tempoMin = GeoUtils.tempoEstimadoMinutos(distanciaKm);

        Transferencia transferencia = Transferencia.builder()
                .itemEstoque(item)
                .hospitalOrigem(origem)
                .hospitalDestino(destino)
                .quantidade(request.quantidade())
                .distanciaKm(distanciaKm)
                .tempoEstimadoMinutos(tempoMin)
                .motivo(request.motivo())
                .geradoPorIa(false)
                .status(StatusLogistico.PENDENTE)
                .build();

        return TransferenciaResponse.fromEntity(transferenciaRepository.save(transferencia));
    }

    @Transactional
    public TransferenciaResponse atualizarStatusTransferencia(Long id, StatusLogistico novoStatus) {
        Transferencia transferencia = transferenciaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transferencia nao encontrada: id " + id));
        transferencia.setStatus(novoStatus);
        return TransferenciaResponse.fromEntity(transferencia);
    }

    @Transactional
    public Transferencia registrarTransferenciaDaIa(ItemEstoque item, Hospital origem, Hospital destino,
                                                      int quantidade, double distanciaKm, double tempoMin,
                                                      String motivo) {
        Transferencia transferencia = Transferencia.builder()
                .itemEstoque(item)
                .hospitalOrigem(origem)
                .hospitalDestino(destino)
                .quantidade(quantidade)
                .distanciaKm(distanciaKm)
                .tempoEstimadoMinutos(tempoMin)
                .motivo(limitarMotivo(motivo))
                .geradoPorIa(true)
                .status(StatusLogistico.PENDENTE)
                .build();

        return transferenciaRepository.save(transferencia);
    }

    private String limitarMotivo(String motivo) {
        if (motivo == null || motivo.length() <= Transferencia.TAMANHO_MAXIMO_MOTIVO) {
            return motivo;
        }
        return motivo.substring(0, Transferencia.TAMANHO_MAXIMO_MOTIVO - 3) + "...";
    }
}
