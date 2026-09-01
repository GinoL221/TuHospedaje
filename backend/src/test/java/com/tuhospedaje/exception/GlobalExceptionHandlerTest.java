package com.tuhospedaje.exception;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.RatingRepository;
import com.tuhospedaje.repository.ReservationRepository;
import com.tuhospedaje.repository.UserRepository;
import com.tuhospedaje.service.LodgingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SC-6.1, SC-6.3: catch-all 500 and PessimisticLockingFailureException → 409.
 * SC-6.2 (UploadException → 502) is covered in UploadExceptionHandlerTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private LodgingRepository lodgingRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MessageSource messageSource;

    @MockitoBean
    private LodgingService lodgingService;

    private String adminToken;

    @BeforeEach
    void setUp() {
        ratingRepository.deleteAll();
        reservationRepository.deleteAll();
        lodgingRepository.deleteAll();
        userRepository.deleteAll();
        User admin = userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("Handler")
                .email("admin-handler@test.com")
                .password("hash")
                .role(RoleEnum.ADMIN)
                .build());
        adminToken = jwtService.generateToken(admin);
    }

    /** SC-6.1: unhandled RuntimeException → 500 standard JSON shape, no stack trace in body */
    @Test
    void unhandledRuntimeException_returns500StandardShape() throws Exception {
        when(lodgingService.findAll()).thenThrow(new RuntimeException("unexpected internal error"));

        mockMvc.perform(get("/api/lodgings"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").isString())
                // must NOT leak the internal exception message to the client
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("unexpected internal error"))));
    }

    /** SC-6.3: PessimisticLockingFailureException → 409 */
    @Test
    void pessimisticLockingFailure_returns409() throws Exception {
        when(lodgingService.findAll())
                .thenThrow(new PessimisticLockingFailureException("lock wait timeout"));

        mockMvc.perform(get("/api/lodgings"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    /** SC-6.3: MethodArgumentNotValidException is NOT intercepted by the catch-all → still returns 400 with fields */
    @Test
    void existingValidationHandler_stillReturns400WithFields() throws Exception {
        // Validation fires at the controller layer before the service is called —
        // the mock doesn't need to be configured here.
        jakarta.servlet.http.Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/lodgings")
                        .cookie(accessCookie(adminToken))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"address\":\"x\",\"city\":\"y\",\"country\":\"z\"," +
                                "\"phoneNumber\":\"1\",\"email\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields").isMap());
    }

    /**
     * Regression guard for the tech-debt fix that replaced HashMap with LinkedHashMap
     * in {@code GlobalExceptionHandler.handleValidation}: the "fields" map in the
     * response must preserve the exact insertion order of
     * {@code BindingResult.getFieldErrors()} — a HashMap does not guarantee this
     * (iteration order is unspecified and bucket-layout dependent), so the handler
     * must keep using a LinkedHashMap. Mocking BindingResult directly (rather than
     * going through real bean validation) makes the asserted order deterministic and
     * independent of Hibernate Validator's own internal traversal order.
     */
    @Test
    void handleValidation_preservesFieldErrorInsertionOrder() {
        // Keys chosen so HashMap's bucket-iteration order ("v","w","x","y","z" —
        // ascending by hashCode/bucket index) is the exact REVERSE of insertion
        // order. "a","b","c" looked safe but coincidentally landed in ascending
        // buckets under HashMap too, so that version of this test could not have
        // caught a regression back to HashMap — verified empirically before
        // settling on this set (see commit message / engram for details).
        BindingResult bindingResult = mock(BindingResult.class);
        List<FieldError> fieldErrors = List.of(
                new FieldError("lodgingDTO", "z", "error Z"),
                new FieldError("lodgingDTO", "y", "error Y"),
                new FieldError("lodgingDTO", "x", "error X"),
                new FieldError("lodgingDTO", "w", "error W"),
                new FieldError("lodgingDTO", "v", "error V"));
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        MessageSource messageSource = mock(MessageSource.class);
        when(messageSource.getMessage("error.validation", null, Locale.ENGLISH)).thenReturn("Validation error.");
        GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource);
        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex, Locale.ENGLISH);

        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) response.getBody().get("fields");

        assertThat(new ArrayList<>(fields.keySet()))
                .containsExactly("z", "y", "x", "w", "v");
    }

    @Test
    void standardHandlers_resolveMessagesViaAcceptLanguageLocale() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource);

        assertThat(errorOf(handler.handleAuthError(new BadCredentialsException("bad"), Locale.ENGLISH)))
                .isEqualTo("Invalid credentials.");
        assertThat(errorOf(handler.handleAuthError(new BadCredentialsException("bad"), new Locale("es"))))
                .isEqualTo("Credenciales inválidas.");

        assertThat(errorOf(handler.handleOptimisticLock(mock(ObjectOptimisticLockingFailureException.class), Locale.ENGLISH)))
                .isEqualTo("The reservation was modified by another user. Try again.");
        assertThat(errorOf(handler.handlePessimisticLock(new PessimisticLockingFailureException("lock"), Locale.ENGLISH)))
                .isEqualTo("The resource is being modified. Try again in a few seconds.");
        assertThat(errorOf(handler.handleUploadError(new UploadException("upload failed"), Locale.ENGLISH)))
                .isEqualTo("Error processing the image.");
        assertThat(errorOf(handler.handleDataIntegrity(new DataIntegrityViolationException("bad data"), Locale.ENGLISH)))
                .isEqualTo("Missing required fields or invalid data.");
        assertThat(errorOf(handler.handleGeneric(new RuntimeException("boom"), Locale.ENGLISH)))
                .isEqualTo("Internal server error.");
    }

    private String errorOf(ResponseEntity<Map<String, Object>> response) {
        return (String) response.getBody().get("error");
    }

    /**
     * Design "Family 429 handled in GlobalExceptionHandler, not in AuthController": a
     * dedicated handler, distinct from {@code AuthController.handleRefreshRejected}
     * (which clears the REFRESH_TOKEN cookie — wrong for a throttled-but-valid
     * credential). Direct unit style, mirroring {@link
     * #standardHandlers_resolveMessagesViaAcceptLanguageLocale()} above.
     */
    @Test
    void rateLimitExceeded_returns429WithRetryAfterHeaderAndErrorBody() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource);

        ResponseEntity<Map<String, Object>> response =
                handler.handleRateLimitExceeded(new RateLimitExceededException(42), Locale.ENGLISH);

        assertThat(response.getStatusCode().value()).isEqualTo(429);
        assertThat(response.getHeaders().getFirst(org.springframework.http.HttpHeaders.RETRY_AFTER)).isEqualTo("42");
        assertThat(response.getBody()).containsExactlyInAnyOrderEntriesOf(
                Map.of("error", "Too many attempts. Try again later.", "status", 429));
    }

    /**
     * i18n scope note: the 13 real {@link IllegalArgumentException} throw sites in this
     * codebase all use hardcoded literal Spanish text (e.g. {@code AuthServiceImpl},
     * {@code LodgingServiceImpl}), not message keys. {@code handleIllegalArgument} tries
     * {@code messageSource.getMessage(ex.getMessage(), null, locale)} first — since the
     * literal text is never a registered key, this MUST fall back to the literal message
     * unchanged, regardless of {@code Accept-Language}. This is accepted, documented
     * behavior (see design doc), not a bug.
     */
    @Test
    void illegalArgument_withUnregisteredLiteralMessage_fallsBackVerbatim_regardlessOfLocale() throws Exception {
        String literalSpanishMessage = "El email ya está registrado";
        when(lodgingService.findAll()).thenThrow(new IllegalArgumentException(literalSpanishMessage));

        mockMvc.perform(get("/api/lodgings"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(literalSpanishMessage));

        mockMvc.perform(get("/api/lodgings").header("Accept-Language", "es"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(literalSpanishMessage));

        mockMvc.perform(get("/api/lodgings").header("Accept-Language", "en"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(literalSpanishMessage));
    }

    /**
     * {@code HandlerMethodValidationException} is defensively handled (per design) even
     * though empirical testing in PR2 found that {@code @Validated} + request-param
     * {@code @Min} on this codebase's controllers actually throws
     * {@link jakarta.validation.ConstraintViolationException}, not this exception type —
     * there is no real request path in this app that triggers it today. Tested directly
     * against the handler method (not via MockMvc) since no reachable endpoint exists.
     */
    @Test
    void handleMethodValidation_resolvesMessageViaMessageSource() {
        MessageSource messageSource = mock(MessageSource.class);
        Locale locale = new Locale("es");

        MessageSourceResolvable resolvable = mock(MessageSourceResolvable.class);
        when(messageSource.getMessage(resolvable, locale)).thenReturn("El tamaño debe ser mayor a cero.");

        ParameterValidationResult result = mock(ParameterValidationResult.class);
        when(result.getResolvableErrors()).thenReturn(List.of(resolvable));

        HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);
        when(ex.getParameterValidationResults()).thenReturn(List.of(result));

        GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource);
        ResponseEntity<Map<String, Object>> response = handler.handleMethodValidation(ex, locale);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().get("error")).isEqualTo("El tamaño debe ser mayor a cero.");
    }

    /**
     * Spring aborts multipart parsing with MaxUploadSizeExceededException once a part
     * crosses spring.servlet.multipart.max-file-size. Without a dedicated handler it
     * lands on the {@code Exception} catch-all and the client sees a 500 — an oversized
     * upload is the caller's input, not a server fault, so it must be 413.
     * <p>
     * Direct unit style: MockMvc's MockMultipartFile bypasses the real multipart parser,
     * so the size limit can never fire through the servlet stack in a MockMvc test.
     */
    @Test
    void maxUploadSizeExceeded_returns413() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource);

        ResponseEntity<Map<String, Object>> response =
                handler.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(5_242_880L), Locale.ENGLISH);

        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(response.getBody()).containsExactlyInAnyOrderEntriesOf(
                Map.of("error", "The image exceeds the maximum allowed size.", "status", 413));
    }

    /** The same handler localizes, so the Spanish bundle is what an es client receives. */
    @Test
    void maxUploadSizeExceeded_resolvesMessageForSpanishLocale() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler(messageSource);

        ResponseEntity<Map<String, Object>> response =
                handler.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(5_242_880L), new Locale("es"));

        assertThat(response.getBody().get("error")).isEqualTo("La imagen supera el tamaño máximo permitido.");
    }
}
