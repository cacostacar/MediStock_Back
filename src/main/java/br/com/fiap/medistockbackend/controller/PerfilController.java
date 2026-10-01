package br.com.fiap.medistockbackend.controller;

import br.com.fiap.medistockbackend.dto.MatriculaResponse;
import br.com.fiap.medistockbackend.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfil")
@RequiredArgsConstructor
@Tag(name = "Perfil", description = "Tela de matricula exibida logo apos cadastro/login")
public class PerfilController {

    private final PerfilService perfilService;

    @GetMapping("/matricula")
    @Operation(summary = "Retorna o nome real do usuario logado + dados mockados de matricula/departamento/cargo")
    public MatriculaResponse matricula(Authentication authentication) {
        return perfilService.matricula(authentication.getName());
    }
}
