package br.com.fiap.medistockbackend.dto;

import br.com.fiap.medistockbackend.model.Usuario;

public record UsuarioResponse(
    Long id,
    String primeiroNome,
    String ultimoNome,
    String emailInstitucional
) {
    public static UsuarioResponse fromEntity(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getPrimeiroNome(),
                usuario.getUltimoNome(),
                usuario.getEmailInstitucional()
        );
    }
}
