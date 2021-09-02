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

package org.springframework.boot.autoconfigure.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JacksonAutoConfiguration} using Jackson 2.11.x
 *
 * @author Stephane Nicoll
 */
@ClassPathExclusions({ "jackson-databind*.jar", "jackson-dataformat-xml*.jar" })
@ClassPathOverrides({ "com.fasterxml.jackson.core:jackson-databind:2.11.3",
		"com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.11.3" })
class Jackson211AutoConfigurationTests extends JacksonAutoConfigurationTests {

	public static final String STRATEGY_CLASS_NAME = "com.fasterxml.jackson.databind.PropertyNamingStrategy$SnakeCaseStrategy";

	@Test
	void customPropertyNamingStrategyField() {
		this.contextRunner.withPropertyValues("spring.jackson.property-naming-strategy:SNAKE_CASE").run((context) -> {
			ObjectMapper mapper = context.getBean(ObjectMapper.class);
			assertThat(mapper.getPropertyNamingStrategy().getClass().getName()).isEqualTo(STRATEGY_CLASS_NAME);
		});
	}

	@Test
	void customPropertyNamingStrategyClass() {
		this.contextRunner.withPropertyValues(
				"spring.jackson.property-naming-strategy:com.fasterxml.jackson.databind.PropertyNamingStrategy.SnakeCaseStrategy")
				.run((context) -> {
					ObjectMapper mapper = context.getBean(ObjectMapper.class);
					assertThat(mapper.getPropertyNamingStrategy().getClass().getName()).isEqualTo(STRATEGY_CLASS_NAME);
				});
	}

	// ConstructorDetector only available as of Jackson 2.12
	@Override
	void constructorDetectorWithNoStrategyUseDefault() {
	}

	@Override
	void constructorDetectorWithDefaultStrategy() {
	}

	@Override
	void constructorDetectorWithUsePropertiesBasedStrategy() {
	}

	@Override
	void constructorDetectorWithUseDelegatingStrategy() {
	}

	@Override
	void constructorDetectorWithExplicitOnlyStrategy() {
	}

}
