/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.data.redis.autoconfigure;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataRedisReactiveAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class DataRedisReactiveAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(DataRedisAutoConfiguration.class, DataRedisReactiveAutoConfiguration.class));

	@Test
	void testDefaultRedisConfiguration() {
		this.contextRunner.run((context) -> {
			Map<String, ?> beans = context.getBeansOfType(ReactiveRedisTemplate.class);
			assertThat(beans).containsOnlyKeys("reactiveRedisTemplate", "reactiveStringRedisTemplate");
		});
	}

}
