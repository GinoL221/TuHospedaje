package com.tuhospedaje.lodging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LodgingCategoryIntegrationTest {

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
        String token = jwtService.generateToken(savedAdmin);
        authHeader = "Bearer " + token;
    }

    @Test
    void shouldCreateLodgingWithCategoryAndExposeCategoryFields() throws Exception {
        Category category = new Category();
        category.setName("Hotel");
        category.setDescription("Lujo");
        Category savedCategory = categoryRepository.save(category);

        Map<String, Object> request = buildLodgingRequest("Gran Hotel", "gran@hotel.com", savedCategory.getId());

        mockMvc.perform(post("/api/lodgings")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
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

        mockMvc.perform(post("/api/lodgings")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
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

        mockMvc.perform(post("/api/lodgings")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
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
        String createResponse = mockMvc.perform(post("/api/lodgings")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long lodgingId = objectMapper.readTree(createResponse).get("id").asLong();

        Map<String, Object> updateRequest = buildLodgingRequest("Cambio Categoría", "cambio@cat.com", savedNewCategory.getId());

        mockMvc.perform(put("/api/lodgings/{id}", lodgingId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(savedNewCategory.getId()))
                .andExpect(jsonPath("$.categoryName").value("Cabaña"));

        mockMvc.perform(get("/api/lodgings/{id}", lodgingId)
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
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

        mockMvc.perform(post("/api/categories")
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
        return request;
    }
}
