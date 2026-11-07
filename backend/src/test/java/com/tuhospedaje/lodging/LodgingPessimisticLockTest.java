package com.tuhospedaje.lodging;

import com.tuhospedaje.configuration.TestcontainersConfiguration;
import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.repository.LodgingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LodgingPessimisticLockTest {

    @Autowired
    private LodgingRepository lodgingRepository;

    @Test
    @Transactional
    void findByIdForUpdate_acquiresLockAndReturnsLodging() {
        Lodging lodging = new Lodging();
        lodging.setName("Lock Test Hotel");
        lodging.setDescription("Test");
        lodging.setAddress("Calle Falsa 123");
        lodging.setCity("Test City");
        lodging.setCountry("Argentina");
        lodging.setPhoneNumber("12345678");
        lodging.setEmail("lock-test@test.com");
        lodging.setPricePerNight(new BigDecimal("150.00"));
        lodging.setMaxGuests(2);
        Lodging saved = lodgingRepository.save(lodging);

        Optional<Lodging> locked = lodgingRepository.findByIdForUpdate(saved.getId());

        assertThat(locked).isPresent();
        assertThat(locked.get().getId()).isEqualTo(saved.getId());
    }
}
