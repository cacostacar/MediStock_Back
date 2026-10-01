package br.com.fiap.medistockbackend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EmailInstitucionalValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)

public @interface EmailInstitucional {
     String message() default "E-mail deve pertencer a um dominio institucional permitido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
