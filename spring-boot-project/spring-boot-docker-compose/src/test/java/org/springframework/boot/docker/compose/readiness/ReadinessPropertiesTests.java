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

package org.springframework.boot.docker.compose.readiness;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReadinessProperties}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ReadinessPropertiesTests {

	@Test
	void getWhenNoPropertiesReturnsNewInstance() {
		Binder binder = new Binder(new MapConfigurationPropertySource());
		ReadinessProperties properties = ReadinessProperties.get(binder);
		assertThat(properties.getTimeout()).isEqualTo(Duration.ofMinutes(2));
		assertThat(properties.getTcp().getConnectTimeout()).isEqualTo(Duration.ofMillis(200));
		assertThat(properties.getTcp().getReadTimeout()).isEqualTo(Duration.ofMillis(200));
	}

	@Test
	void getWhenPropertiesReturnsBoundInstance() {
		Map<String, String> source = new LinkedHashMap<>();
		source.put("spring.docker.compose.readiness.timeout", "10s");
		source.put("spring.docker.compose.readiness.tcp.connect-timeout", "400ms");
		source.put("spring.docker.compose.readiness.tcp.read-timeout", "500ms");
		Binder binder = new Binder(new MapConfigurationPropertySource(source));
		ReadinessProperties properties = ReadinessProperties.get(binder);
		assertThat(properties.getTimeout()).isEqualTo(Duration.ofSeconds(10));
		assertThat(properties.getTcp().getConnectTimeout()).isEqualTo(Duration.ofMillis(400));
		assertThat(properties.getTcp().getReadTimeout()).isEqualTo(Duration.ofMillis(500));

	}

}
