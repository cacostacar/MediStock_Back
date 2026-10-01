package br.com.fiap.medistockbackend.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "itens_estoque")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "quantidade_atual", nullable = false)
    private Integer quantidadeAtual;

    @Column(name = "quantidade_minima", nullable = false)
    private Integer quantidadeMinima;

    @Column(length = 20)
    private String unidadeMedida;

    @Column(name = "local_armazenamento", length = 150)
    private String localArmazenamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    private LocalDate validade;

    @Column(name = "custo_unitario", precision = 12, scale = 2)
    private BigDecimal custoUnitario;

    @Column(name = "alto_custo_baixa_demanda", nullable = false)
    @Builder.Default
    private boolean altoCustoBaixaDemanda = false;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @PrePersist
    @PreUpdate
    public void aoSalvar() {
        this.atualizadoEm = LocalDateTime.now();
    }

    public NivelEstoque calcularNivel() {
        if (quantidadeAtual <= quantidadeMinima) {
            return NivelEstoque.CRITICO;
        }
        if (quantidadeAtual <= quantidadeMinima * 1.5) {
            return NivelEstoque.ATENCAO;
        }
        return NivelEstoque.NORMAL;
    }

    public boolean isVencido() {
        return validade != null && validade.isBefore(LocalDate.now());
    }

    public boolean isValidadeProxima() {
        if (validade == null || isVencido()) {
            return false;
        }
        return !validade.isAfter(LocalDate.now().plusDays(60));
    }

    public long diasParaVencer() {
        if (validade == null) {
            return Long.MAX_VALUE;
        }
        return ChronoUnit.DAYS.between(LocalDate.now(), validade);
    }
}
