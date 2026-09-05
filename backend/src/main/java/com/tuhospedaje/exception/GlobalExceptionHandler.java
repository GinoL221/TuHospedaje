package com.tuhospedaje.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    /**
     * Resolves via {@code MessageSource}. Handles the only real throw site
     * ({@code ReservationServiceImpl.getReservationById}), which uses the plain
     * {@code (String message)} constructor — no {@code errorCode} — so the fallback
     * branch ({@code error.resource.not_found={0}}, a passthrough) is the one actually
     * exercised in production today. The {@code errorCode}/{@code args} branch exists for
     * future callers that want real per-key localization.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex, Locale locale) {
        String resolvedMsg;
        if (ex.getErrorCode() != null) {
            resolvedMsg = messageSource.getMessage(ex.getErrorCode(), ex.getArgs(), locale);
        } else {
            resolvedMsg = messageSource.getMessage("error.resource.not_found", new Object[]{ex.getMessage()}, locale);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", resolvedMsg, "status", 404));
    }

    /**
     * Resolves via {@code MessageSource}, keyed by the exception message itself. The 13
     * real throw sites in this codebase (e.g. {@code AuthServiceImpl},
     * {@code LodgingServiceImpl}) all use hardcoded literal Spanish text, not message
     * keys, so none of them match a registered key — the {@link NoSuchMessageException}
     * fallback to {@code ex.getMessage()} verbatim is what actually runs for all of them
     * today, regardless of {@code Accept-Language}. This is documented, accepted
     * behavior, not a regression: converting those 13 sites to keys is out of scope.
     */
    /**
     * Kept distinct from {@link #handleIllegalArgument} so this response body can carry
     * a stable {@code code} the frontend discriminates on ({@code err.code}), instead of
     * parsing the localized {@code error} message text as {@code RegisterPage.jsx} used
     * to. Resolved via a fixed message key ({@code error.auth.duplicate_email}), not the
     * exception's own message — same pattern as {@link #handleAuthError}.
     */
    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateEmail(DuplicateEmailException ex, Locale locale) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", resolveMessage("error.auth.duplicate_email", locale),
                        "status", 400,
                        "code", DuplicateEmailException.ERROR_CODE));
    }

    /**
     * Design "Family 429 handled in GlobalExceptionHandler, not in AuthController":
     * {@code AuthController}'s local {@code handleRefreshRejected} clears the
     * REFRESH_TOKEN cookie, which is correct for a dead credential and WRONG for a
     * throttled one — the credential is still valid and must survive the cooldown. This
     * global handler also keeps the 429 body byte-identical to {@code
     * RefreshRateLimitFilter}'s self-serialized 429 (both resolve {@code
     * error.rate_limit} via the same fixed key and emit the same {@code {error,status}}
     * shape).
     */
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(RateLimitExceededException ex, Locale locale) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(Map.of("error", resolveMessage("error.rate_limit", locale), "status", 429));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex, Locale locale) {
        String resolvedMsg;
        try {
            resolvedMsg = messageSource.getMessage(ex.getMessage(), null, locale);
        } catch (NoSuchMessageException e) {
            resolvedMsg = ex.getMessage();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", resolvedMsg, "status", 400));
    }

    /**
     * Enhances the interim handler added in the PR2 follow-up (raw {@code ex.getMessage()},
     * no {@code MessageSource}) with locale-aware resolution for `@Min`-style request-param
     * validation failures (e.g. negative `page` / non-positive `size` on
     * `/api/lodgings/search`). {@code @Validated} at class level throws this exception
     * (legacy AOP method validation) — verified empirically in the PR2 follow-up.
     * <p>
     * Each violation's {@code messageTemplate} is expected in {@code {key}} form (see
     * {@code @Min(message = "{error.page.negative}")} on {@code LodgingController}); the
     * key is looked up via {@code MessageSource} with the already-interpolated Bean
     * Validation message as the default fallback.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolations(ConstraintViolationException ex, Locale locale) {
        String resolved = ex.getConstraintViolations().stream()
                .map(violation -> resolveTemplateMessage(violation.getMessageTemplate(), violation.getMessage(), locale))
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", resolved, "status", 400));
    }

    /**
     * Enhances the interim handler added in the PR2 follow-up with locale-aware
     * resolution. Defensive: no request path in this codebase is currently known to throw
     * this exception type (the native Spring 6.1+ method-validation mechanism) rather than
     * {@link ConstraintViolationException} — see class-level note above — but it is kept
     * mapped so it never silently falls through to the generic 500 catch-all if that ever
     * changes.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<Map<String, Object>> handleMethodValidation(HandlerMethodValidationException ex, Locale locale) {
        String resolved = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(error -> resolveResolvableMessage(error, locale))
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", resolved, "status", 400));
    }

    private String resolveTemplateMessage(String messageTemplate, String fallback, Locale locale) {
        if (messageTemplate != null && messageTemplate.startsWith("{") && messageTemplate.endsWith("}")) {
            String key = messageTemplate.substring(1, messageTemplate.length() - 1);
            return messageSource.getMessage(key, null, fallback, locale);
        }
        return fallback;
    }

    private String resolveResolvableMessage(MessageSourceResolvable resolvable, Locale locale) {
        try {
            return messageSource.getMessage(resolvable, locale);
        } catch (NoSuchMessageException e) {
            String[] codes = resolvable.getCodes();
            return (codes != null && codes.length > 0) ? codes[codes.length - 1] : "Validation error";
        }
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthError(AuthenticationException ex, Locale locale) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", resolveMessage("error.auth.invalid", locale), "status", 401));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, Locale locale) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), resolveResolvableMessage(fieldError, locale));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", resolveMessage("error.validation", locale), "status", 400, "fields", errors));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, Locale locale) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", resolveMessage("error.optimistic_lock", locale), "status", 409));
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> handlePessimisticLock(PessimisticLockingFailureException ex, Locale locale) {
        log.warn("Pessimistic lock contention: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", resolveMessage("error.pessimistic_lock", locale), "status", 409));
    }

    /**
     * The multipart parser aborts with this exception once a part crosses
     * {@code spring.servlet.multipart.max-file-size}, before the request ever reaches
     * {@code UploadController}. Without this handler it falls through to the
     * {@code Exception} catch-all and the client gets a 500 for what is plainly its own
     * oversized input — 413 is the honest answer, and the only one a UI can act on.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, Locale locale) {
        log.warn("Upload rejected: exceeds max size ({} bytes)", ex.getMaxUploadSize());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", resolveMessage("error.upload.too_large", locale), "status", 413));
    }

    @ExceptionHandler(UploadException.class)
    public ResponseEntity<Map<String, Object>> handleUploadError(UploadException ex, Locale locale) {
        log.error("Upload failed", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", resolveMessage("error.upload", locale), "status", 502));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex, Locale locale) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", resolveMessage("error.data_integrity", locale), "status", 400));
    }

    /**
     * Catch-all for unhandled exceptions. AccessDeniedException is explicitly re-thrown
     * so Spring Security's access-denied handler can produce the 403 response instead
     * of this catch-all swallowing it and returning 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, Locale locale) throws Exception {
        if (ex instanceof AccessDeniedException) {
            throw ex;
        }
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", resolveMessage("error.internal", locale), "status", 500));
    }

    private String resolveMessage(String key, Locale locale) {
        return messageSource.getMessage(key, null, locale);
    }
}
