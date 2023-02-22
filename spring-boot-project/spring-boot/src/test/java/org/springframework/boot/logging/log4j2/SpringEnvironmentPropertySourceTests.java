/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.logging.log4j2;

import java.util.Properties;

import org.apache.logging.log4j.util.PropertiesPropertySource;
import org.apache.logging.log4j.util.SystemPropertiesPropertySource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link SpringEnvironmentPropertySource}.
 *
 * @author Phillip Webb
 */
class SpringEnvironmentPropertySourceTests {

	private MockEnvironment environment;

	private SpringEnvironmentPropertySource propertySource;

	@BeforeEach
	void setup() {
		this.environment = new MockEnvironment();
		this.environment.setProperty("spring", "boot");
		this.propertySource = new SpringEnvironmentPropertySource(this.environment);
	}

	@Test
	void createWhenEnvironmentIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new SpringEnvironmentPropertySource(null))
			.withMessage("Environment must not be null");
	}

	@Test
	void getPriorityIsOrderedCorrectly() {
		int priority = this.propertySource.getPriority();
		assertThat(priority).isEqualTo(-100);
		assertThat(priority).isLessThan(new SystemPropertiesPropertySource().getPriority());
		assertThat(priority).isLessThan(new PropertiesPropertySource(new Properties()).getPriority());
	}

	@Test
	void getPropertyWhenInEnvironmentReturnsValue() {
		assertThat(this.propertySource.getProperty("spring")).isEqualTo("boot");
	}

	@Test
	void getPropertyWhenNotInEnvironmentReturnsNull() {
		assertThat(this.propertySource.getProperty("nope")).isNull();
	}

	@Test
	void containsPropertyWhenInEnvironmentReturnsTrue() {
		assertThat(this.propertySource.containsProperty("spring")).isTrue();
	}

	@Test
	void containsPropertyWhenNotInEnvironmentReturnsFalse() {
		assertThat(this.propertySource.containsProperty("nope")).isFalse();
	}

}
