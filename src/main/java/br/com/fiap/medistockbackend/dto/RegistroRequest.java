package br.com.fiap.medistockbackend.dto;

import br.com.fiap.medistockbackend.validation.EmailInstitucional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest (

        @NotBlank(message = "Primeiro nome e obrigatorio")
        @Size(max = 80)
        String primeiroNome,

        @NotBlank(message = "Ultimo nome e obrigatorio")
        @Size(max = 80)
        String ultimoNome,

        @NotBlank(message = "E-mail institucional e obrigatorio")
        @EmailInstitucional
        @Size(max = 150)
        String emailInstitucional,

        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 8, message = "Senha deve ter no minimo 8 caracteres")
        String senha

) {}
