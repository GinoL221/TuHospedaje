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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/lodgings")
                        .header(HttpHeaders.AUTHORIZATION, adminToken)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"address\":\"x\",\"city\":\"y\",\"country\":\"z\"," +
                                "\"phoneNumber\":\"1\",\"email\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields").isMap());
    }
}
