/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.service.connection;

import java.lang.reflect.Field;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.origin.Origin;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link FieldOrigin}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class FieldOriginTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenFieldIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new FieldOrigin(null))
			.withMessage("'field' must not be null");
	}

	@Test
	void equalsAndHashCode() {
		Origin o1 = new FieldOrigin(findField("one"));
		Origin o2 = new FieldOrigin(findField("one"));
		Origin o3 = new FieldOrigin(findField("two"));
		assertThat(o1).isEqualTo(o1).isEqualTo(o2).isNotEqualTo(o3);
		assertThat(o1).hasSameHashCodeAs(o2);
	}

	@Test
	void toStringReturnsSensibleString() {
		Origin origin = new FieldOrigin(findField("one"));
		assertThat(origin).hasToString("FieldOriginTests.Fields.one");
	}

	private Field findField(String name) {
		Field field = ReflectionUtils.findField(Fields.class, name);
		assertThat(field).isNotNull();
		return field;
	}

	static class Fields {

		@Nullable String one;

		@Nullable String two;

	}

}
