package com.tuhospedaje.lodging;

import com.tuhospedaje.entity.Category;
import com.tuhospedaje.entity.Lodging;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class LodgingEntityMappingTest {

    @Test
    void shouldMapCategoryAsLazyManyToOneWithNullableJoinColumn() throws NoSuchFieldException {
        Field categoryField = Lodging.class.getDeclaredField("category");

        ManyToOne manyToOne = categoryField.getAnnotation(ManyToOne.class);
        JoinColumn joinColumn = categoryField.getAnnotation(JoinColumn.class);

        assertThat(categoryField.getType()).isEqualTo(Category.class);
        assertThat(manyToOne).isNotNull();
        assertThat(manyToOne.fetch()).isEqualTo(FetchType.LAZY);
        assertThat(joinColumn).isNotNull();
        assertThat(joinColumn.name()).isEqualTo("category_id");
        assertThat(joinColumn.nullable()).isTrue();
    }
}
