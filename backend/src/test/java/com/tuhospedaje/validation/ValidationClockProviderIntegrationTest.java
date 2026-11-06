package com.tuhospedaje.validation;

import com.tuhospedaje.AbstractIntegrationTest;
import com.tuhospedaje.dto.reservation.CreateReservationRequest;
import jakarta.validation.ClockProvider;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ValidationClockProviderIntegrationTest extends AbstractIntegrationTest {

    @Autowired(required = false)
    private ClockProvider clockProvider;

    @Autowired
    private ValidatorFactory validatorFactory;

    @Autowired
    private Clock businessClock;

    @Test
    void applicationProvidesClockProviderConfiguredWithBusinessTimeZone() {
        assertThat(clockProvider)
                .as("A ClockProvider bean must be registered so Bean Validation respects business time zone")
                .isNotNull();

        assertThat(clockProvider.getClock().getZone())
                .isEqualTo(ZoneId.of("America/Argentina/Buenos_Aires"));

        assertThat(validatorFactory.getClockProvider().getClock().getZone())
                .isEqualTo(ZoneId.of("America/Argentina/Buenos_Aires"));
    }

    @Test
    void acceptsSameDayCheckInUsingBusinessClock() {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setLodgingId(1L);
        LocalDate todayInBusinessZone = LocalDate.now(businessClock);
        request.setCheckIn(todayInBusinessZone);
        request.setCheckOut(todayInBusinessZone.plusDays(2));
        request.setGuestPhone("+5491112345678");

        Set<ConstraintViolation<CreateReservationRequest>> violations =
                validatorFactory.getValidator().validate(request);
        assertThat(violations).noneMatch(v -> v.getPropertyPath().toString().equals("checkIn"));
    }
}
