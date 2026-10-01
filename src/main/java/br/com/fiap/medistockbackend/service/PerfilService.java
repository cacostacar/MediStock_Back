package br.com.fiap.medistockbackend.service;

import br.com.fiap.medistockbackend.dto.MatriculaResponse;
import br.com.fiap.medistockbackend.exception.CredenciaisInvalidasException;
import br.com.fiap.medistockbackend.model.Usuario;
import br.com.fiap.medistockbackend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PerfilService {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public MatriculaResponse matricula(String emailAutenticado) {
        Usuario usuario = usuarioRepository.findByEmailInstitucionalIgnoreCase(emailAutenticado)
                .orElseThrow(CredenciaisInvalidasException::new);
        return MatriculaResponse.mockPara(usuario.getNomeCompleto());
    }
}
