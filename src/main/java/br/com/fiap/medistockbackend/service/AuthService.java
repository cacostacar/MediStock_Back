package br.com.fiap.medistockbackend.service;

import br.com.fiap.medistockbackend.dto.AuthResponse;
import br.com.fiap.medistockbackend.dto.LoginRequest;
import br.com.fiap.medistockbackend.dto.RegistroRequest;
import br.com.fiap.medistockbackend.dto.UsuarioResponse;
import br.com.fiap.medistockbackend.exception.CredenciaisInvalidasException;
import br.com.fiap.medistockbackend.exception.EmailCadastradoException;
import br.com.fiap.medistockbackend.model.Usuario;
import br.com.fiap.medistockbackend.repository.UsuarioRepository;
import br.com.fiap.medistockbackend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmailInstitucionalIgnoreCase(request.emailInstitucional())) {
            throw new EmailCadastradoException(request.emailInstitucional());
        }

        Usuario usuario = Usuario.builder()
                .primeiroNome(request.primeiroNome().trim())
                .ultimoNome(request.ultimoNome().trim())
                .emailInstitucional(request.emailInstitucional().trim().toLowerCase())
                .senhaHash(passwordEncoder.encode(request.senha()))
                .build();

        usuarioRepository.save(usuario);

        return gerarAuthResponse(usuario);
    }

    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmailInstitucionalIgnoreCase(request.emailInstitucional())
                .orElseThrow(CredenciaisInvalidasException::new);

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }

        return gerarAuthResponse(usuario);
    }

    private AuthResponse gerarAuthResponse(Usuario usuario) {
        String token = jwtUtil.gerarToken(usuario.getEmailInstitucional());
        return new AuthResponse(
                token,
                "Bearer",
                jwtUtil.getExpiracaoMinutos(),
                UsuarioResponse.fromEntity(usuario)
        );
    }
}
