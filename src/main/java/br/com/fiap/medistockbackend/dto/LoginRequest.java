package br.com.fiap.medistockbackend.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "E-mail institucional e obrigatorio")
        String emailInstitucional,

        @NotBlank(message = "Senha e obrigatoria")
        String senha
) {}
