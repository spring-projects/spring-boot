/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.origin.Origin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataLocationNotFoundException}.
 *
 * @author Phillip Webb
 */
class ConfigDataLocationNotFoundExceptionTests {

	private final Origin origin = mock(Origin.class);

	private final ConfigDataLocation location = ConfigDataLocation.of("optional:test").withOrigin(this.origin);

	private final ConfigDataLocationNotFoundException exception = new ConfigDataLocationNotFoundException(
			this.location);

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenLocationIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConfigDataLocationNotFoundException(null))
			.withMessage("'location' must not be null");
	}

	@Test
	void getLocationReturnsLocation() {
		assertThat(this.exception.getLocation()).isSameAs(this.location);
	}

	@Test
	void getOriginReturnsLocationOrigin() {
		assertThat(this.exception.getOrigin()).isSameAs(this.origin);
	}

	@Test
	void getReferenceDescriptionReturnsLocationString() {
		assertThat(this.exception.getReferenceDescription()).isEqualTo("location 'optional:test'");
	}

	@Test
	void getMessageReturnsMessage() {
		assertThat(this.exception).hasMessage("Config data location 'optional:test' cannot be found");
	}

}
