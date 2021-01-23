/*
 * Copyright 2012-2020 the original author or authors.
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

	private Origin origin = mock(Origin.class);

	private final ConfigDataLocation location = ConfigDataLocation.of("optional:test").withOrigin(this.origin);

	private final ConfigDataLocationNotFoundException exception = new ConfigDataLocationNotFoundException(
			this.location);

	@Test
	void createWhenLocationIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ConfigDataLocationNotFoundException(null))
				.withMessage("Location must not be null");
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
