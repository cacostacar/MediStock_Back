package br.com.fiap.medistockbackend.dto;

public record AuthResponse(

    String token,
    String tipo,
    long expiraEmMinutos,
    UsuarioResponse usuario

) {}
