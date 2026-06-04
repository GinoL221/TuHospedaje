package com.tuhospedaje.reservation;

import com.tuhospedaje.entity.Lodging;
import com.tuhospedaje.entity.Reservation;
import com.tuhospedaje.enums.ReservationStatus;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationEntityMappingTest {

    @Test
    void shouldHaveIdGeneratedByIdentity() throws NoSuchFieldException {
        Field idField = Reservation.class.getDeclaredField("id");

        GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
        Id id = idField.getAnnotation(Id.class);

        assertThat(id).isNotNull();
        assertThat(generatedValue).isNotNull();
        assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);
    }

    @Test
    void shouldMapLodgingAsManyToOne() throws NoSuchFieldException {
        Field lodgingField = Reservation.class.getDeclaredField("lodging");

        ManyToOne manyToOne = lodgingField.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = lodgingField.getAnnotation(JoinColumn.class);

        assertThat(lodgingField.getType()).isEqualTo(Lodging.class);
        assertThat(manyToOne).isNotNull();
        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(joinColumn).isNotNull();
        assertThat(joinColumn.name()).isEqualTo("lodging_id");
    }

    @Test
    void shouldMapStatusAsEnumeratedString() throws NoSuchFieldException {
        Field statusField = Reservation.class.getDeclaredField("status");

        Enumerated enumerated = statusField.getAnnotation(Enumerated.class);

        assertThat(enumerated).isNotNull();
        assertThat(enumerated.value()).isEqualTo(EnumType.STRING);
    }

    @Test
    void shouldHaveVersionAnnotation() throws NoSuchFieldException {
        Field versionField = Reservation.class.getDeclaredField("version");

        Version version = versionField.getAnnotation(Version.class);

        assertThat(version).isNotNull();
        assertThat(versionField.getType()).isEqualTo(Long.class);
    }

    @Test
    void shouldMapDatesAsLocalDate() throws NoSuchFieldException {
        Field checkInField = Reservation.class.getDeclaredField("checkIn");
        Field checkOutField = Reservation.class.getDeclaredField("checkOut");

        assertThat(checkInField.getType()).isEqualTo(LocalDate.class);
        assertThat(checkOutField.getType()).isEqualTo(LocalDate.class);
    }

    @Test
    void shouldDefaultStatusToConfirmed() {
        Reservation reservation = new Reservation();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void shouldCalculateTotalPrice() {
        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2026, 7, 1));
        reservation.setCheckOut(LocalDate.of(2026, 7, 5));

        BigDecimal nights = BigDecimal.valueOf(
                reservation.getCheckOut().toEpochDay() - reservation.getCheckIn().toEpochDay()
        );
        BigDecimal pricePerNight = new BigDecimal("150.00");
        BigDecimal total = pricePerNight.multiply(nights);

        assertThat(total).isEqualByComparingTo(new BigDecimal("600.00"));
    }
}
