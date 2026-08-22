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
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// US-21.1-S1..S3 and QA-2-S1/S3: deterministic HTTPS fixture URLs only, no remote fetch.
@SpringBootTest
@AutoConfigureMockMvc
class CategoryImageIntegrationTest extends AbstractIntegrationTest {

    private static final String VALID_IMAGE_URL = "https://cdn.tuhospedaje.test/categories/hotel.jpg";
    private static final String REPLACEMENT_IMAGE_URL = "https://cdn.tuhospedaje.test/categories/hotel-v2.jpg";

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String authHeader;

    @BeforeEach
    void setUp() {
        User admin = User.builder()
                .firstName("Admin")
                .lastName("Image")
                .email("admin-category-image-test@tuhospedaje.com")
                .password("123456")
                .role(RoleEnum.ADMIN)
                .build();

        authHeader = jwtService.generateToken(userRepository.save(admin));
    }

    @Test
    void categoriesTableHasANullableImageUrlColumn() {
        Integer columnCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'categories'
                  AND column_name = 'image_url'
                  AND is_nullable = 'YES'
                  AND character_maximum_length = 2048
                """, Integer.class);

        assertThat(columnCount).isEqualTo(1);
    }

    @Test
    void shouldRejectCreateWhenImageUrlIsMissing() throws Exception {
        CategoryDTO request = new CategoryDTO();
        request.setName("Sin imagen");
        request.setDescription("Falta la imagen representativa");

        performAuthenticatedPost(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.imageUrl").exists());

        assertThat(categoryRepository.findByNameIgnoreCase("Sin imagen")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not-a-url", "http://cdn.tuhospedaje.test/hotel.jpg", "javascript:alert(1)"})
    void shouldRejectCreateWithMalformedOrUnsupportedImageUrl(String invalidUrl) throws Exception {
        CategoryDTO request = new CategoryDTO();
        request.setName("Imagen inválida " + invalidUrl.hashCode());
        request.setImageUrl(invalidUrl);

        performAuthenticatedPost(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.imageUrl").exists());
    }

    @Test
    void shouldCreateCategoryWithAValidHttpsImageUrlAndExposeItOnRead() throws Exception {
        CategoryDTO request = new CategoryDTO();
        request.setName("Hotel boutique");
        request.setDescription("Con imagen representativa");
        request.setImageUrl(VALID_IMAGE_URL);

        performAuthenticatedPost(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").value(VALID_IMAGE_URL));

        Category saved = categoryRepository.findByNameIgnoreCase("Hotel boutique").orElseThrow();
        assertThat(saved.getImageUrl()).isEqualTo(VALID_IMAGE_URL);

        mockMvc.perform(get("/api/categories/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(VALID_IMAGE_URL));
    }

    @Test
    void shouldReplaceTheImageOnAValidEdit() throws Exception {
        Category existing = persistCategory("Cabaña editable", VALID_IMAGE_URL);

        CategoryDTO update = new CategoryDTO();
        update.setName(existing.getName());
        update.setImageUrl(REPLACEMENT_IMAGE_URL);

        performAuthenticatedPut(existing.getId(), update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(REPLACEMENT_IMAGE_URL));

        assertThat(categoryRepository.findById(existing.getId()).orElseThrow().getImageUrl())
                .isEqualTo(REPLACEMENT_IMAGE_URL);
    }

    @Test
    void shouldPreserveTheStoredImageWhenAnUpdateOmitsIt() throws Exception {
        Category existing = persistCategory("Hostal legado", VALID_IMAGE_URL);

        CategoryDTO update = new CategoryDTO();
        update.setName(existing.getName());
        update.setDescription("Solo cambia la descripción");

        performAuthenticatedPut(existing.getId(), update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrl").value(VALID_IMAGE_URL));

        assertThat(categoryRepository.findById(existing.getId()).orElseThrow().getImageUrl())
                .isEqualTo(VALID_IMAGE_URL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "not-a-url", "data:image/png;base64,AAAA"})
    void shouldRejectABlankOrMalformedReplacementWithoutErasingTheExistingImage(String invalidUrl) throws Exception {
        Category existing = persistCategory("Posada protegida " + invalidUrl.hashCode(), VALID_IMAGE_URL);

        CategoryDTO update = new CategoryDTO();
        update.setName(existing.getName());
        update.setImageUrl(invalidUrl);

        performAuthenticatedPut(existing.getId(), update)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.imageUrl").exists());

        assertThat(categoryRepository.findById(existing.getId()).orElseThrow().getImageUrl())
                .isEqualTo(VALID_IMAGE_URL);
    }

    private Category persistCategory(String name, String imageUrl) {
        Category category = new Category();
        category.setName(name);
        category.setImageUrl(imageUrl);
        return categoryRepository.save(category);
    }

    private org.springframework.test.web.servlet.ResultActions performAuthenticatedPost(CategoryDTO request) throws Exception {
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        return mockMvc.perform(post("/api/categories")
                .cookie(accessCookie(authHeader))
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private org.springframework.test.web.servlet.ResultActions performAuthenticatedPut(Long id, CategoryDTO request) throws Exception {
        Cookie csrfCookie = obtainCsrfCookie(mockMvc);
        return mockMvc.perform(put("/api/categories/{id}", id)
                .cookie(accessCookie(authHeader))
                .cookie(csrfCookie)
                .header("X-XSRF-TOKEN", csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }
}
