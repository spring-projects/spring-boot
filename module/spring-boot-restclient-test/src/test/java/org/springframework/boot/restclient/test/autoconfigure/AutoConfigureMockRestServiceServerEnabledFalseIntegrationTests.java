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

package org.springframework.boot.restclient.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.MockServerRestClientCustomizer;
import org.springframework.boot.restclient.test.MockServerRestTemplateCustomizer;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for
 * {@link AutoConfigureMockRestServiceServer @AutoConfigureMockRestServiceServer} with
 * {@code enabled=false}.
 *
 * @author Phillip Webb
 */
@RestClientTest
@AutoConfigureMockRestServiceServer(enabled = false)
class AutoConfigureMockRestServiceServerEnabledFalseIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void mockServerRestTemplateCustomizerShouldNotBeRegistered() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(MockServerRestTemplateCustomizer.class));
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(MockServerRestClientCustomizer.class));
	}

}
