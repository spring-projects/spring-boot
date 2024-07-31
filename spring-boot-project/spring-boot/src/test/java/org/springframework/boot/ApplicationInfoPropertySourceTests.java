/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.system.ApplicationPid;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApplicationInfoPropertySource}.
 *
 * @author Moritz Halbritter
 */
class ApplicationInfoPropertySourceTests {

	@Test
	void shouldAddVersion() {
		MockEnvironment environment = new MockEnvironment();
		environment.getPropertySources().addLast(new ApplicationInfoPropertySource("1.2.3"));
		assertThat(environment.getProperty("spring.application.version")).isEqualTo("1.2.3");
	}

	@Test
	void shouldNotAddVersionIfVersionIsNotAvailable() {
		MockEnvironment environment = new MockEnvironment();
		environment.getPropertySources().addLast(new ApplicationInfoPropertySource((String) null));
		assertThat(environment.containsProperty("spring.application.version")).isFalse();
	}

	@Test
	void shouldAddPid() {
		MockEnvironment environment = new MockEnvironment();
		environment.getPropertySources().addLast(new ApplicationInfoPropertySource("1.2.3"));
		assertThat(environment.getProperty("spring.application.pid", Long.class))
			.isEqualTo(new ApplicationPid().toLong());
	}

	@Test
	void shouldMoveToEnd() {
		MockEnvironment environment = new MockEnvironment();
		environment.getPropertySources().addFirst(new MapPropertySource("first", Collections.emptyMap()));
		environment.getPropertySources().addAfter("first", new MapPropertySource("second", Collections.emptyMap()));
		environment.getPropertySources().addFirst(new ApplicationInfoPropertySource("1.2.3"));
		List<String> propertySources = environment.getPropertySources().stream().map(PropertySource::getName).toList();
		assertThat(propertySources).containsExactly("applicationInfo", "first", "second", "mockProperties");
		ApplicationInfoPropertySource.moveToEnd(environment);
		List<String> propertySourcesAfterMove = environment.getPropertySources()
			.stream()
			.map(PropertySource::getName)
			.toList();
		assertThat(propertySourcesAfterMove).containsExactly("first", "second", "mockProperties", "applicationInfo");
	}

}
