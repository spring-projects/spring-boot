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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OptionalConfigDataLocation}.
 *
 * @author Phillip Webb
 */
class OptionalConfigDataLocationTests {

	private ConfigDataLocation location;

	@BeforeEach
	void setup() {
		this.location = new ResourceConfigDataLocation("classpath:application.properties",
				new ClassPathResource("application.properties"), null, mock(PropertySourceLoader.class));
	}

	@Test
	void createWrapsLocation() {
		OptionalConfigDataLocation optionalLocation = new OptionalConfigDataLocation(this.location);
		assertThat(optionalLocation.getLocation()).isSameAs(this.location);
	}

	@Test
	void equalsAndHashCode() {
		OptionalConfigDataLocation optionalLocation1 = new OptionalConfigDataLocation(this.location);
		OptionalConfigDataLocation optionalLocation2 = new OptionalConfigDataLocation(this.location);
		assertThat(optionalLocation1.hashCode()).isEqualTo(optionalLocation2.hashCode());
		assertThat(optionalLocation1).isEqualTo(optionalLocation1).isEqualTo(optionalLocation2)
				.isNotEqualTo(this.location);
	}

	@Test
	void toStringReturnsLocationString() {
		OptionalConfigDataLocation optionalLocation = new OptionalConfigDataLocation(this.location);
		assertThat(optionalLocation).hasToString(this.location.toString());
	}

	@Test
	void wrapAllWrapsList() {
		List<ConfigDataLocation> locations = Collections.singletonList(this.location);
		List<ConfigDataLocation> optionalLocations = OptionalConfigDataLocation.wrapAll(locations);
		assertThat(optionalLocations).hasSize(1);
		assertThat(optionalLocations.get(0)).isInstanceOf(OptionalConfigDataLocation.class).extracting("location")
				.isSameAs(this.location);
	}

	@Test
	void unwrapUnwrapps() {
		ConfigDataLocation optionalLocation = new OptionalConfigDataLocation(this.location);
		assertThatObject(OptionalConfigDataLocation.unwrap(optionalLocation)).isSameAs(this.location);
	}

}
