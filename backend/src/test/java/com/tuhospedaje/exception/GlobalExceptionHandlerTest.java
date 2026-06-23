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
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
        adminToken = "Bearer " + jwtService.generateToken(admin);
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
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
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

        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);

        @SuppressWarnings("unchecked")
        Map<String, String> fields = (Map<String, String>) response.getBody().get("fields");

        assertThat(new ArrayList<>(fields.keySet()))
                .containsExactly("z", "y", "x", "w", "v");
    }
}
