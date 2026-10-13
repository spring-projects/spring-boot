/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.context.properties.bind.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link BindValidationException}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class BindValidationExceptionTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void createWhenValidationErrorsIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new BindValidationException(null))
			.withMessageContaining("'validationErrors' must not be null");
	}

	@Test
	void getValidationErrorsShouldReturnValidationErrors() {
		ValidationErrors errors = mock(ValidationErrors.class);
		BindValidationException exception = new BindValidationException(errors);
		assertThat(exception.getValidationErrors()).isEqualTo(errors);
	}

}
