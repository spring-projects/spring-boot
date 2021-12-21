/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.graphql;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link InvalidSchemaLocationsException}.
 *
 * @author Brian Clozel
 */
class InvalidSchemaLocationsExceptionTests {

	private final String schemaFolder = "graphql/";

	private final String[] locations = new String[] { "classpath:" + this.schemaFolder };

	@Test
	void shouldRejectEmptyLocations() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new InvalidSchemaLocationsException(new String[] {}, new PathMatchingResourcePatternResolver()))
				.isInstanceOf(IllegalArgumentException.class).withMessage("locations should not be empty");
	}

	@Test
	void shouldRejectNullResolver() {
		assertThatIllegalArgumentException().isThrownBy(() -> new InvalidSchemaLocationsException(this.locations, null))
				.isInstanceOf(IllegalArgumentException.class).withMessage("resolver should not be null");
	}

	@Test
	void shouldExposeConfiguredLocations() {
		InvalidSchemaLocationsException exception = new InvalidSchemaLocationsException(this.locations,
				new PathMatchingResourcePatternResolver());
		assertThat(exception.getSchemaLocations()).hasSize(1);
		InvalidSchemaLocationsException.SchemaLocation schemaLocation = exception.getSchemaLocations().get(0);
		assertThat(schemaLocation.getLocation()).isEqualTo(this.locations[0]);
		assertThat(schemaLocation.getUri()).endsWith(this.schemaFolder);
	}

	@Test
	void shouldNotFailWithUnresolvableLocations() {
		String unresolved = "classpath:unresolved/";
		InvalidSchemaLocationsException exception = new InvalidSchemaLocationsException(new String[] { unresolved },
				new PathMatchingResourcePatternResolver());
		assertThat(exception.getSchemaLocations()).hasSize(1);
		InvalidSchemaLocationsException.SchemaLocation schemaLocation = exception.getSchemaLocations().get(0);
		assertThat(schemaLocation.getLocation()).isEqualTo(unresolved);
		assertThat(schemaLocation.getUri()).isEmpty();
	}

}
