/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.reactive;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.web.reactive.ReactiveManagementChildContextConfiguration.AccessLogCustomizer;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveManagementChildContextConfiguration}.
 *
 * @author Moritz Halbritter
 */
class ReactiveManagementChildContextConfigurationTests {

	@Test
	void accessLogCustomizer() {
		AccessLogCustomizer customizer = new AccessLogCustomizer("prefix") {
		};
		assertThat(customizer.customizePrefix(null)).isEqualTo("prefix");
		assertThat(customizer.customizePrefix("existing")).isEqualTo("prefixexisting");
		assertThat(customizer.customizePrefix("prefixexisting")).isEqualTo("prefixexisting");
	}

	@Test
	void accessLogCustomizerWithNullPrefix() {
		AccessLogCustomizer customizer = new AccessLogCustomizer(null) {
		};
		assertThat(customizer.customizePrefix(null)).isEqualTo(null);
		assertThat(customizer.customizePrefix("existing")).isEqualTo("existing");
	}

	@Test
	// gh-45857
	void failsWithoutManagementServerPropertiesBeanFromParent() {
		new ReactiveWebApplicationContextRunner()
			.run((parent) -> new ReactiveWebApplicationContextRunner().withParent(parent)
				.withUserConfiguration(ReactiveManagementChildContextConfiguration.class)
				.run((context) -> assertThat(context).hasFailed()));
	}

	@Test
	// gh-45857
	void succeedsWithManagementServerPropertiesBeanFromParent() {
		new ReactiveWebApplicationContextRunner().withBean(ManagementServerProperties.class)
			.run((parent) -> new ReactiveWebApplicationContextRunner().withParent(parent)
				.withUserConfiguration(ReactiveManagementChildContextConfiguration.class)
				.run((context) -> assertThat(context).hasNotFailed()));
	}

}
