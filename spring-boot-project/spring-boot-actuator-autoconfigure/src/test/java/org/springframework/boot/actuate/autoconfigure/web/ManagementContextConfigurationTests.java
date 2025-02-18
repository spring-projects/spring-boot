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

package org.springframework.boot.actuate.autoconfigure.web;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManagementContextConfiguration @ManagementContextConfiguration}.
 *
 * @author Andy Wilkinson
 */
class ManagementContextConfigurationTests {

	@Test
	void proxyBeanMethodsIsEnabledByDefault() {
		AnnotationAttributes attributes = AnnotatedElementUtils
			.getMergedAnnotationAttributes(DefaultManagementContextConfiguration.class, Configuration.class);
		assertThat(attributes).containsEntry("proxyBeanMethods", true);
	}

	@Test
	void proxyBeanMethodsCanBeDisabled() {
		AnnotationAttributes attributes = AnnotatedElementUtils.getMergedAnnotationAttributes(
				NoBeanMethodProxyingManagementContextConfiguration.class, Configuration.class);
		assertThat(attributes).containsEntry("proxyBeanMethods", false);
	}

	@ManagementContextConfiguration
	static class DefaultManagementContextConfiguration {

	}

	@ManagementContextConfiguration(proxyBeanMethods = false)
	static class NoBeanMethodProxyingManagementContextConfiguration {

	}

}
