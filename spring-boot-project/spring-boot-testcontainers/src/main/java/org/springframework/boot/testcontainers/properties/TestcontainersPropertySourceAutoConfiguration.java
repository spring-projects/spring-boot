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

package org.springframework.boot.testcontainers.properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} to add {@link TestcontainersPropertySource} support.
 *
 * @author Phillip Webb
 * @since 3.1.0
 */
@ConditionalOnClass(DynamicPropertyRegistry.class)
public class TestcontainersPropertySourceAutoConfiguration {

	TestcontainersPropertySourceAutoConfiguration() {
	}

	@Bean
	DynamicPropertyRegistry dynamicPropertyRegistry(ConfigurableEnvironment environment) {
		return TestcontainersPropertySource.attach(environment);
	}

}
