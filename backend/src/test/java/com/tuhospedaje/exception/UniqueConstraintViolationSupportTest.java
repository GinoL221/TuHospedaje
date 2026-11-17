package com.tuhospedaje.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class UniqueConstraintViolationSupportTest {

    @Test
    void returnsFalseForNullThrowableOrBlankConstraint() {
        assertThat(UniqueConstraintViolationSupport.matchesConstraint(null, "UK_categories_name")).isFalse();
        assertThat(UniqueConstraintViolationSupport.matchesConstraint(
                new DataIntegrityViolationException("error"), "   ")).isFalse();
    }

    @Test
    void matchesExactSchemaPrefixedAndQuotedHibernateConstraintNames() {
        DataIntegrityViolationException exact = wrapped("UK_categories_name", "duplicate");
        DataIntegrityViolationException prefixed = wrapped("tuhospedaje.categories.UK_categories_name", "duplicate");
        DataIntegrityViolationException quoted = wrapped("`UK_categories_name`", "duplicate");

        assertThat(UniqueConstraintViolationSupport.matchesConstraint(exact, "uk_categories_name")).isTrue();
        assertThat(UniqueConstraintViolationSupport.matchesConstraint(prefixed, "UK_categories_name")).isTrue();
        assertThat(UniqueConstraintViolationSupport.matchesConstraint(quoted, "UK_categories_name")).isTrue();
    }

    @Test
    void doesNotFallBackToMessageWhenHibernateSuppliesDifferentConstraintName() {
        DataIntegrityViolationException otherConstraint = wrapped(
                "FK_lodgings_category",
                "Cannot add row with name UK_lodgings_name because FK_lodgings_category failed");

        assertThat(UniqueConstraintViolationSupport.matchesConstraint(otherConstraint, "UK_lodgings_name")).isFalse();
    }

    @Test
    void fallsBackToCauseMessageWhenConstraintNameIsNullOrNoHibernateConstraintExists() {
        ConstraintViolationException unnamed = new ConstraintViolationException(
                "could not execute",
                new SQLException("Duplicate entry 'x' for key 'UK_features_name'"),
                null);
        DataIntegrityViolationException wrappedUnnamed = new DataIntegrityViolationException("stmt", unnamed);
        DataIntegrityViolationException plain = new DataIntegrityViolationException(
                "Duplicate entry 'x' for key 'UK_features_name'");

        assertThat(UniqueConstraintViolationSupport.matchesConstraint(wrappedUnnamed, "UK_features_name")).isTrue();
        assertThat(UniqueConstraintViolationSupport.matchesConstraint(plain, "UK_features_name")).isTrue();
        assertThat(UniqueConstraintViolationSupport.matchesConstraint(plain, "UK_other")).isFalse();
    }

    @Test
    void handlesCyclicCauseWithoutLooping() {
        RuntimeException root = new RuntimeException("no match");
        RuntimeException wrapper = new RuntimeException("wrapper", root);
        root.initCause(wrapper);

        assertThat(UniqueConstraintViolationSupport.matchesConstraint(wrapper, "UK_categories_name")).isFalse();
    }

    private DataIntegrityViolationException wrapped(String constraintName, String sqlMessage) {
        ConstraintViolationException cve = new ConstraintViolationException(
                "could not execute statement",
                new SQLException(sqlMessage),
                constraintName);
        return new DataIntegrityViolationException("could not execute statement", cve);
    }
}
