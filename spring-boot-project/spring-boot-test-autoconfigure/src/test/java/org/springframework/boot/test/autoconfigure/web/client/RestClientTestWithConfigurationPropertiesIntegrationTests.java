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

package org.springframework.boot.test.autoconfigure.web.client;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RestClientTest @RestClientTest} with a
 * {@link ConfigurationProperties @ConfigurationProperties} annotated type.
 *
 * @author Stephane Nicoll
 */
@RestClientTest(components = ExampleProperties.class, properties = "example.name=Hello")
class RestClientTestWithConfigurationPropertiesIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void configurationPropertiesCanBeAddedAsComponent() {
		assertThat(this.applicationContext.getBeansOfType(ExampleProperties.class).keySet())
			.containsOnly("example-org.springframework.boot.test.autoconfigure.web.client.ExampleProperties");
		assertThat(this.applicationContext.getBean(ExampleProperties.class).getName()).isEqualTo("Hello");
	}

}
