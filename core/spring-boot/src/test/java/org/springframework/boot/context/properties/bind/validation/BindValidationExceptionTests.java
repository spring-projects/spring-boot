/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
