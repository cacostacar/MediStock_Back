package br.com.fiap.medistockbackend.repository;

import br.com.fiap.medistockbackend.model.ItemEstoque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemEstoqueRepository extends JpaRepository<ItemEstoque, Long> {

    List<ItemEstoque> findByHospitalId(Long hospitalId);

    List<ItemEstoque> findByAltoCustoBaixaDemandaTrue();
}
