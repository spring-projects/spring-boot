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

package org.springframework.boot.io;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for resolving configuration properties using
 * {@code ProtocolResolver ProtocolResolvers}.
 *
 * @author Scott Frederick
 */
class ProtocolResolverApplicationContextInitializerIntegrationTests {

	@Test
	void base64ResourceResolves() throws IOException {
		SpringApplication application = new SpringApplication(TestConfiguration.class);
		application.setDefaultProperties(Map.of("test.resource", "base64:dGVzdC12YWx1ZQ=="));
		application.setWebApplicationType(WebApplicationType.NONE);
		ConfigurableApplicationContext context = application.run();
		TestProperties propertiesBean = context.getBean(TestProperties.class);
		Resource resource = propertiesBean.getResource();
		assertThat(resource).isNotNull();
		assertThat(resource.exists()).isTrue();
		assertThat(resource.getContentAsString(Charset.defaultCharset())).isEqualTo("test-value");
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(TestProperties.class)
	static class TestConfiguration {

	}

	@ConfigurationProperties("test")
	static class TestProperties {

		Resource resource;

		Resource getResource() {
			return this.resource;
		}

		void setResource(Resource resource) {
			this.resource = resource;
		}

	}

}
