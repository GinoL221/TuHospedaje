package com.tuhospedaje.lodging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LodgingCategoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private LodgingRepository lodgingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String authHeader;

    @BeforeEach
    void setUp() {
        User admin = User.builder()
                .firstName("Admin")
                .lastName("Lodging")
                .email("admin-lodging-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.ADMIN)
                .build();

        User savedAdmin = userRepository.save(admin);
        authHeader = jwtService.generateToken(savedAdmin);
    }

    @Test
    void shouldCreateLodgingWithCategoryAndExposeCategoryFields() throws Exception {
        Category category = new Category();
        category.setName("Hotel");
        category.setDescription("Lujo");
        Category savedCategory = categoryRepository.save(category);

        Map<String, Object> request = buildLodgingRequest("Gran Hotel", "gran@hotel.com", savedCategory.getId());

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/lodgings")
                        .cookie(accessCookie(authHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Gran Hotel"))
                .andExpect(jsonPath("$.categoryId").value(savedCategory.getId()))
                .andExpect(jsonPath("$.categoryName").value("Hotel"));
    }

    @Test
    void shouldCreateLodgingWithoutCategoryAndExposeNullCategoryFields() throws Exception {
        Map<String, Object> request = buildLodgingRequest("Sin Categoría", "sin@categoria.com", null);

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/lodgings")
                        .cookie(accessCookie(authHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sin Categoría"))
                .andExpect(jsonPath("$.categoryId").isEmpty())
                .andExpect(jsonPath("$.categoryName").isEmpty());
    }

    @Test
    void shouldReturnNotFoundWhenCreatingLodgingWithMissingCategory() throws Exception {
        Map<String, Object> request = buildLodgingRequest("Categoría Missing", "missing@cat.com", 9999L);

        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/lodgings")
                        .cookie(accessCookie(authHeader))
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Categoría no encontrada"));
    }

    @Test
    void shouldUpdateLodgingCategoryAndExposeCategoryInGetById() throws Exception {
        Category oldCategory = new Category();
        oldCategory.setName("Hotel");
        oldCategory.setDescription("Old");
        Category savedOldCategory = categoryRepository.save(oldCategory);

        Category newCategory = new Category();
        newCategory.setName("Cabaña");
        newCategory.setDescription("New");
        Category savedNewCategory = categoryRepository.save(newCategory);

        Map<String, Object> createRequest = buildLodgingRequest("Cambio Categoría", "cambio@cat.com", savedOldCategory.getId());
        Cookie createCsrfCookie = obtainCsrfCookie(mockMvc);
        String createResponse = mockMvc.perform(post("/api/lodgings")
                        .cookie(accessCookie(authHeader))
                        .cookie(createCsrfCookie)
                        .header("X-XSRF-TOKEN", createCsrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long lodgingId = objectMapper.readTree(createResponse).get("id").asLong();

        Map<String, Object> updateRequest = buildLodgingRequest("Cambio Categoría", "cambio@cat.com", savedNewCategory.getId());

        Cookie updateCsrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(put("/api/lodgings/{id}", lodgingId)
                        .cookie(accessCookie(authHeader))
                        .cookie(updateCsrfCookie)
                        .header("X-XSRF-TOKEN", updateCsrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(savedNewCategory.getId()))
                .andExpect(jsonPath("$.categoryName").value("Cabaña"));

        mockMvc.perform(get("/api/lodgings/{id}", lodgingId)
                        .cookie(accessCookie(authHeader)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(savedNewCategory.getId()))
                .andExpect(jsonPath("$.categoryName").value("Cabaña"));
    }

    @Test
    void shouldPermitPublicGetCategoriesWithoutAuthentication() throws Exception {
        Category category = new Category();
        category.setName("Hostel");
        categoryRepository.save(category);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/categories/{id}", category.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hostel"));
    }

    @Test
    void shouldRejectCategoryWriteWithoutAuthentication() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", "NoAuth");
        request.put("description", "Write denied");

        // Keep CSRF valid even without auth, so the 403 is attributable to the missing
        // token, not to a missing CSRF header (design's explicit ordering-trap warning).
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        mockMvc.perform(post("/api/categories")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private Map<String, Object> buildLodgingRequest(String name, String email, Long categoryId) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        request.put("description", "Descripción test");
        request.put("address", "Calle 123");
        request.put("city", "Ciudad");
        request.put("country", "País");
        request.put("phoneNumber", "123456789");
        request.put("email", email);
        request.put("categoryId", categoryId);
        request.put("pricePerNight", new BigDecimal("30000.00"));
        request.put("maxGuests", 4);
        return request;
    }
}
