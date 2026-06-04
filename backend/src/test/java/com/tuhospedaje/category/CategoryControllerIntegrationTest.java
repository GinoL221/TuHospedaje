package com.tuhospedaje.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.configuration.JwtService;
import com.tuhospedaje.dto.category.CategoryDTO;
import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.User;
import com.tuhospedaje.enums.RoleEnum;
import com.tuhospedaje.repository.CategoryRepository;
import com.tuhospedaje.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    private String authHeader;

    @BeforeEach
    void setUp() {
        User admin = User.builder()
                .firstName("Admin")
                .lastName("Test")
                .email("admin-category-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.ADMIN)
                .build();

        User savedAdmin = userRepository.save(admin);
        String token = jwtService.generateToken(savedAdmin);
        authHeader = "Bearer " + token;
    }

    @Test
    void shouldCreateCategorySuccessfully() throws Exception {
        CategoryDTO request = new CategoryDTO();
        request.setName("Hotel 5 estrellas");
        request.setDescription("Alojamientos de lujo");

        mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Hotel 5 estrellas"))
                .andExpect(jsonPath("$.description").value("Alojamientos de lujo"));
    }

    @Test
    void shouldReturnBadRequestWhenCreatingDuplicateCategory() throws Exception {
        Category existing = new Category();
        existing.setName("Cabaña");
        existing.setDescription("Inicial");
        categoryRepository.save(existing);

        CategoryDTO request = new CategoryDTO();
        request.setName("Cabaña");
        request.setDescription("Otra");

        mockMvc.perform(post("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldListCategories() throws Exception {
        Category one = new Category();
        one.setName("Hotel");
        categoryRepository.save(one);

        Category two = new Category();
        two.setName("Hostel");
        categoryRepository.save(two);

        mockMvc.perform(get("/api/categories")
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void shouldGetCategoryById() throws Exception {
        Category category = new Category();
        category.setName("Posada");
        Category saved = categoryRepository.save(category);

        mockMvc.perform(get("/api/categories/{id}", saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("Posada"));
    }

    @Test
    void shouldReturnNotFoundWhenGetCategoryByIdDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/categories/{id}", 999L)
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateCategorySuccessfully() throws Exception {
        Category category = new Category();
        category.setName("Hotel");
        category.setDescription("Original");
        Category saved = categoryRepository.save(category);

        CategoryDTO request = new CategoryDTO();
        request.setName("Hotel Boutique");
        request.setDescription("Actualizado");

        mockMvc.perform(put("/api/categories/{id}", saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("Hotel Boutique"))
                .andExpect(jsonPath("$.description").value("Actualizado"));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingCategoryDoesNotExist() throws Exception {
        CategoryDTO request = new CategoryDTO();
        request.setName("No existe");

        mockMvc.perform(put("/api/categories/{id}", 888L)
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldDeleteCategorySuccessfully() throws Exception {
        Category category = new Category();
        category.setName("Temporal");
        Category saved = categoryRepository.save(category);

        mockMvc.perform(delete("/api/categories/{id}", saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingCategoryDoesNotExist() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", 777L)
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isNotFound());
    }
}
