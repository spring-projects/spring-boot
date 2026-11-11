/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.json;

import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ObjectContent}.
 *
 * @author Phillip Webb
 */
class ObjectContentTests {

	private static final ExampleObject OBJECT = new ExampleObject();

	private static final ResolvableType TYPE = ResolvableType.forClass(ExampleObject.class);

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenObjectIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ObjectContent<ExampleObject>(TYPE, null))
			.withMessageContaining("'object' must not be null");
	}

	@Test
	void createWhenTypeIsNullShouldCreateContent() {
		ObjectContent<ExampleObject> content = new ObjectContent<>(null, OBJECT);
		assertThat(content).isNotNull();
	}

	@Test
	void assertThatShouldReturnObjectContentAssert() {
		ObjectContent<ExampleObject> content = new ObjectContent<>(TYPE, OBJECT);
		assertThat(content.assertThat()).isInstanceOf(ObjectContentAssert.class);
	}

	@Test
	void getObjectShouldReturnObject() {
		ObjectContent<ExampleObject> content = new ObjectContent<>(TYPE, OBJECT);
		assertThat(content.getObject()).isEqualTo(OBJECT);
	}

	@Test
	void toStringWhenHasTypeShouldReturnString() {
		ObjectContent<ExampleObject> content = new ObjectContent<>(TYPE, OBJECT);
		assertThat(content.toString()).isEqualTo("ObjectContent " + OBJECT + " created from " + TYPE);
	}

	@Test
	void toStringWhenHasNoTypeShouldReturnString() {
		ObjectContent<ExampleObject> content = new ObjectContent<>(null, OBJECT);
		assertThat(content.toString()).isEqualTo("ObjectContent " + OBJECT);
	}

}
