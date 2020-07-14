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

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ResourceConfigDataLocation}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
public class ResourceConfigDataLocationTests {

	private final String location = "location";

	private final Resource resource = mock(Resource.class);

	private final PropertySourceLoader propertySource = mock(PropertySourceLoader.class);

	@Test
	void constructorWhenNameIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ResourceConfigDataLocation(null, this.resource, this.propertySource))
				.withMessage("Name must not be null");
	}

	@Test
	void constructorWhenResourceIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ResourceConfigDataLocation(this.location, null, this.propertySource))
				.withMessage("Resource must not be null");
	}

	@Test
	void constructorWhenLoaderIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ResourceConfigDataLocation(this.location, this.resource, null))
				.withMessage("PropertySourceLoader must not be null");
	}

	@Test
	void equalsWhenResourceIsTheSameReturnsTrue() {
		Resource resource = new ClassPathResource("config/");
		ResourceConfigDataLocation location = new ResourceConfigDataLocation("my-location", resource,
				this.propertySource);
		ResourceConfigDataLocation other = new ResourceConfigDataLocation("other-location", resource,
				this.propertySource);
		assertThat(location).isEqualTo(other);
	}

	@Test
	void equalsWhenResourceIsDifferentReturnsFalse() {
		Resource resource1 = new ClassPathResource("config/");
		Resource resource2 = new ClassPathResource("configdata/");
		ResourceConfigDataLocation location = new ResourceConfigDataLocation("my-location", resource1,
				this.propertySource);
		ResourceConfigDataLocation other = new ResourceConfigDataLocation("other-location", resource2,
				this.propertySource);
		assertThat(location).isNotEqualTo(other);
	}

}
