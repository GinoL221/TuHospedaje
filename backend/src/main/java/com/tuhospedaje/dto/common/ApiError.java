package com.tuhospedaje.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Documents the JSON body of an API error response in OpenAPI.
 *
 * <p>Responses are not built from this type: {@code GlobalExceptionHandler} and
 * {@code SecurityErrorHandler} write the same two fields directly. It exists so the
 * generated documentation shows the real contract instead of an empty body.
 */
@Schema(description = "Body of an error response")
public record ApiError(
        @Schema(description = "Message in the language of the request (Accept-Language)",
                example = "You do not have permission to perform this action.")
        String error,
        @Schema(description = "HTTP status code, repeated in the body", example = "403")
        int status) {
}
