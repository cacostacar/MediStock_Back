package br.com.fiap.medistockbackend.repository;

import br.com.fiap.medistockbackend.model.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {
}
