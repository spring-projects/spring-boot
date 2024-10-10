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

package org.springframework.boot.logging.structured;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StructuredLoggingJsonProperties}.
 *
 * @author Phillip Webb
 */
class StructuredLoggingJsonPropertiesTests {

	@Test
	void getBindsFromEnvironment() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("logging.structured.json.include", "a,b");
		environment.setProperty("logging.structured.json.exclude", "c,d");
		environment.setProperty("logging.structured.json.rename.e", "f");
		environment.setProperty("logging.structured.json.add.g", "h");
		environment.setProperty("logging.structured.json.customizer", "i");
		StructuredLoggingJsonProperties properties = StructuredLoggingJsonProperties.get(environment);
		assertThat(properties).isEqualTo(new StructuredLoggingJsonProperties(Set.of("a", "b"), Set.of("c", "d"),
				Map.of("e", "f"), Map.of("g", "h"), "i"));
	}

	@Test
	void getWhenNoBoundPropertiesReturnsNull() {
		MockEnvironment environment = new MockEnvironment();
		StructuredLoggingJsonProperties.get(environment);
	}

}
