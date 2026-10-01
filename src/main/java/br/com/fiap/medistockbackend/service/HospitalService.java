package br.com.fiap.medistockbackend.service;

import br.com.fiap.medistockbackend.dto.HospitalDtos.HospitalRequest;
import br.com.fiap.medistockbackend.dto.HospitalDtos.HospitalResponse;
import br.com.fiap.medistockbackend.exception.ResourceNotFoundException;
import br.com.fiap.medistockbackend.model.Hospital;
import br.com.fiap.medistockbackend.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    @Transactional(readOnly = true)
    public List<HospitalResponse> listarTodos() {
        return hospitalRepository.findAll().stream()
                .map(HospitalResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public HospitalResponse buscarPorId(Long id) {
        return HospitalResponse.fromEntity(buscarEntidade(id));
    }

    @Transactional
    public HospitalResponse criar(HospitalRequest request) {
        Hospital hospital = Hospital.builder()
                .nome(request.nome())
                .endereco(request.endereco())
                .cidade(request.cidade())
                .estado(request.estado())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();

        return HospitalResponse.fromEntity(hospitalRepository.save(hospital));
    }

    @Transactional
    public HospitalResponse atualizar(Long id, HospitalRequest request) {
        Hospital hospital = buscarEntidade(id);
        hospital.setNome(request.nome());
        hospital.setEndereco(request.endereco());
        hospital.setCidade(request.cidade());
        hospital.setEstado(request.estado());
        hospital.setLatitude(request.latitude());
        hospital.setLongitude(request.longitude());
        return HospitalResponse.fromEntity(hospital);
    }

    @Transactional
    public void excluir(Long id) {
        if (!hospitalRepository.existsById(id)) {
            throw new ResourceNotFoundException("Hospital nao encontrado: id " + id);
        }
        hospitalRepository.deleteById(id);
    }

    Hospital buscarEntidade(Long id) {
        return hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital nao encontrado: id " + id));
    }
}
