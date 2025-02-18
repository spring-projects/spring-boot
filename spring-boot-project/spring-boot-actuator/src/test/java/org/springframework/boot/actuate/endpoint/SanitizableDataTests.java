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

package org.springframework.boot.actuate.endpoint;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SanitizableData}.
 *
 * @author Phillip Webb
 */
class SanitizableDataTests {

	private final PropertySource<?> propertySource = new MapPropertySource("test", Map.of("key", "value"));

	@Test
	void getPropertySourceReturnsPropertySource() {
		SanitizableData data = new SanitizableData(this.propertySource, "key", "value");
		assertThat(data.getPropertySource()).isSameAs(this.propertySource);
	}

	@Test
	void getKeyReturnsKey() {
		SanitizableData data = new SanitizableData(this.propertySource, "key", "value");
		assertThat(data.getKey()).isEqualTo("key");
	}

	@Test
	void getValueReturnsValue() {
		SanitizableData data = new SanitizableData(this.propertySource, "key", "value");
		assertThat(data.getValue()).isEqualTo("value");
	}

	@Test
	void withSanitizedValueReturnsNewInstanceWithSanitizedValue() {
		SanitizableData data = new SanitizableData(this.propertySource, "key", "value");
		SanitizableData sanitized = data.withSanitizedValue();
		assertThat(data.getValue()).isEqualTo("value");
		assertThat(sanitized.getValue()).isEqualTo("******");
	}

	@Test
	void withValueReturnsNewInstanceWithNewValue() {
		SanitizableData data = new SanitizableData(this.propertySource, "key", "value");
		SanitizableData sanitized = data.withValue("eulav");
		assertThat(data.getValue()).isEqualTo("value");
		assertThat(sanitized.getValue()).isEqualTo("eulav");
	}

}
