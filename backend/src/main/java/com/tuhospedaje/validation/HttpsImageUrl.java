package com.tuhospedaje.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Constrains a representative-media field to an absolute {@code https} URL, or to
 * {@code null} (used by callers that also need {@code @NotBlank} for a create-only
 * requirement, e.g. category media). See {@link HttpsImageUrlValidator} for the exact
 * accept/reject rules; no remote fetch or content inspection is performed.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = HttpsImageUrlValidator.class)
public @interface HttpsImageUrl {

    String message() default "La URL de la imagen debe ser una dirección https absoluta y válida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
