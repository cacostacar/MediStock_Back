package br.com.fiap.medistockbackend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class EmailInstitucionalValidator implements ConstraintValidator<EmailInstitucional, String> {

    @Value("${medistock.auth.dominios-permitidos}")
    private String dominiosPermitidosRaw;

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || !email.contains("@")) {
            return false;
        }

        String dominioDoEmail = email.substring(email.indexOf('@') + 1).toLowerCase().trim();

        List<String> dominiosPermitidos = Arrays.stream(dominiosPermitidosRaw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .toList();

        boolean valido = dominiosPermitidos.contains(dominioDoEmail);

        if (!valido) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Dominio '" + dominioDoEmail + "' nao autorizado. Dominios aceitos: "
                            + String.join(", ", dominiosPermitidos)
            ).addConstraintViolation();
        }

        return valido;
    }
}
