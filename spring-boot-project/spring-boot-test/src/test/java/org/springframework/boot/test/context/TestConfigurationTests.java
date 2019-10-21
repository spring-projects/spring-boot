/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.context;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Tests for {@link TestConfiguration}.
 *
 * @author Dmytro Nosan
 */
class TestConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void shouldProxyBeanMethods() {
		this.contextRunner.withUserConfiguration(ProxyBeanMethodsConfiguration.class)
				.run((context) -> Assertions.assertThat(context).hasFailed());
	}

	@Test
	void shouldNotProxyBeanMethods() {
		this.contextRunner.withUserConfiguration(ProxyBeanMethodsDisableConfiguration.class)
				.run((context) -> Assertions.assertThat(context).hasNotFailed());
	}

	@TestConfiguration
	final static class ProxyBeanMethodsConfiguration {

	}

	@TestConfiguration(proxyBeanMethods = false)
	final static class ProxyBeanMethodsDisableConfiguration {

	}

}
