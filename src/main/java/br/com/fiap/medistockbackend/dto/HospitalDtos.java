package br.com.fiap.medistockbackend.dto;

import br.com.fiap.medistockbackend.model.Hospital;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class HospitalDtos {
    public record HospitalRequest(
            @NotBlank @Size(max = 150) String nome,
            @Size(max = 200) String endereco,
            @Size(max = 80) String cidade,
            @Size(max = 2) String estado,
            @NotNull Double latitude,
            @NotNull Double longitude
    ) {}

    public record HospitalResponse(
            Long id,
            String nome,
            String endereco,
            String cidade,
            String estado,
            Double latitude,
            Double longitude
    ) {
        public static HospitalResponse fromEntity(Hospital h) {
            return new HospitalResponse(h.getId(), h.getNome(), h.getEndereco(),
                    h.getCidade(), h.getEstado(), h.getLatitude(), h.getLongitude());
        }
    }
}
