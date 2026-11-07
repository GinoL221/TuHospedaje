package com.tuhospedaje.lodging;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.dto.lodging.LodgingDTO;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.repository.LodgingRepository;
import com.tuhospedaje.service.LodgingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LodgingDtoVersionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LodgingRepository lodgingRepository;

    @Autowired
    private LodgingService lodgingService;

    @Test
    void mapsVersionFromEntityToDto() {
        Lodging lodging = new Lodging();
        lodging.setName("Hotel Version Test");
        lodging.setDescription("Desc");
        lodging.setAddress("Calle 123");
        lodging.setCity("Bariloche");
        lodging.setCountry("Argentina");
        lodging.setPhoneNumber("+54000");
        lodging.setEmail("version-test@hotel.com");
        lodging.setPricePerNight(new BigDecimal("100.00"));
        lodging.setMaxGuests(2);
        lodging.setVersion(3L);

        LodgingDTO dto = LodgingDTO.fromEntity(lodging);
        assertThat(dto.getVersion()).isEqualTo(3L);
    }

    @Test
    void getLodgingExposesVersionInJsonResponse() throws Exception {
        Lodging lodging = new Lodging();
        lodging.setName("Hotel Json Version");
        lodging.setDescription("Desc");
        lodging.setAddress("Calle 123");
        lodging.setCity("Bariloche");
        lodging.setCountry("Argentina");
        lodging.setPhoneNumber("+54001");
        lodging.setEmail("json-version@hotel.com");
        lodging.setPricePerNight(new BigDecimal("100.00"));
        lodging.setMaxGuests(2);
        lodging = lodgingRepository.save(lodging);

        mockMvc.perform(get("/api/lodgings/{id}", lodging.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").isNumber());
    }

    @Test
    void rejectsUpdateWhenClientProvidesStaleVersion() {
        Lodging lodging = new Lodging();
        lodging.setName("Hotel Concurrency Test");
        lodging.setDescription("Desc");
        lodging.setAddress("Calle 123");
        lodging.setCity("Bariloche");
        lodging.setCountry("Argentina");
        lodging.setPhoneNumber("+54002");
        lodging.setEmail("concurrency-test@hotel.com");
        lodging.setPricePerNight(new BigDecimal("100.00"));
        lodging.setMaxGuests(2);
        lodging = lodgingRepository.save(lodging);

        LodgingDTO updateDto = LodgingDTO.fromEntity(lodging);
        updateDto.setName("Hotel Concurrency Renamed");
        // Simulate concurrent modification where DB version is incremented or different
        updateDto.setVersion(999L);

        assertThatThrownBy(() -> lodgingService.update(updateDto))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
