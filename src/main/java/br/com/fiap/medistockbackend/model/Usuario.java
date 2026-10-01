package br.com.fiap.medistockbackend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(columnNames = "email_institucional"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "primeiro_nome", nullable = false, length = 80)
    private String primeiroNome;

    @Column(name = "ultimo_nome", nullable = false, length = 80)
    private String ultimoNome;

    @Column(name = "email_institucional", nullable = false, unique = true, length = 150)
    private String emailInstitucional;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    public void aoPersistir() {
        this.criadoEm = LocalDateTime.now();
    }

    public String getNomeCompleto() {
        return primeiroNome + " " + ultimoNome;
    }
}
