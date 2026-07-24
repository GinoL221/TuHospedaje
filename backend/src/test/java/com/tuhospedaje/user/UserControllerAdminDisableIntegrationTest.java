package com.tuhospedaje.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.dto.auth.RegisterRequest;
import com.tuhospedaje.dto.auth.UserStatusRequest;
import com.tuhospedaje.entity.SessionSecurityEvent;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.SessionSecurityEventRepository;
import com.tuhospedaje.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers Delta Spec scenario "Admin disablement revokes sessions and blocks the very next
 * request" (session-revocation-lifecycle domain) with refresh sessions genuinely enabled —
 * a separate Spring context from {@link UserControllerIntegrationTest}, which deliberately
 * runs with the flag off.
 */
@SpringBootTest(properties = "app.session.refresh.enabled=true")
@AutoConfigureMockMvc
class UserControllerAdminDisableIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionSecurityEventRepository sessionSecurityEventRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void disablingAUserRevokesAllSessionsAndRejectsTheNextRequestAndRefresh() throws Exception {
        User admin = userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("Disabler")
                .email("admin-disabler@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.ADMIN)
                .build());
        String adminAuthHeader = jwtService.generateToken(admin);

        RegisterRequest registerRequest = new RegisterRequest("Target", "User", "target-disable@test.com", "123456");
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie targetAccessCookie = registerResult.getResponse().getCookie("ACCESS_TOKEN");
        Cookie targetRefreshCookie = registerResult.getResponse().getCookie("REFRESH_TOKEN");
        assertThat(targetAccessCookie).isNotNull();
        assertThat(targetRefreshCookie).isNotNull();
        Long targetUserId = userRepository.findByEmail("target-disable@test.com").orElseThrow().getId();

        // The still-valid JWT authenticates fine BEFORE the account is disabled.
        mockMvc.perform(get("/api/auth/me").cookie(targetAccessCookie))
                .andExpect(status().isOk());

        UserStatusRequest disableRequest = new UserStatusRequest(false);
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(patch("/api/users/{id}/enabled", targetUserId)
                        .cookie(accessCookie(adminAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disableRequest)))
                .andExpect(status().isOk());

        assertThat(userRepository.findById(targetUserId).orElseThrow().isEnabled()).isFalse();

        // The very next request with the SAME still-valid, unexpired JWT is rejected.
        mockMvc.perform(get("/api/auth/me").cookie(targetAccessCookie))
                .andExpect(status().isUnauthorized());

        // A subsequent refresh attempt with the (family-revoked) REFRESH_TOKEN also fails
        // with the same generic, non-disclosing 401 as any other invalid refresh.
        mockMvc.perform(post("/api/auth/refresh").cookie(targetRefreshCookie))
                .andExpect(status().isUnauthorized());

        assertThat(sessionSecurityEventRepository.findAll().stream()
                .filter(event -> event.getUser().getId().equals(targetUserId))
                .filter(event -> event.getEventType() == SessionSecurityEvent.Type.ADMIN_DISABLE))
                .singleElement();
    }

    @Test
    void onlyAdminCanDisableAUser() throws Exception {
        User regularUser = userRepository.save(User.builder()
                .firstName("Regular")
                .lastName("User")
                .email("regular-disabler-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.USER)
                .build());
        String regularAuthHeader = jwtService.generateToken(regularUser);

        UserStatusRequest disableRequest = new UserStatusRequest(false);
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(patch("/api/users/{id}/enabled", regularUser.getId())
                        .cookie(accessCookie(regularAuthHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disableRequest)))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(regularUser.getId()).orElseThrow().isEnabled()).isTrue();
    }
}
