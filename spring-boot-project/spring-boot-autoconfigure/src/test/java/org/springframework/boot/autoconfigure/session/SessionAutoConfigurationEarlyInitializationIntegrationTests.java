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

package org.springframework.boot.autoconfigure.session;

import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests to ensure {@link SessionAutoConfiguration} and
 * {@link SessionRepositoryFilterConfiguration} does not cause early initialization.
 *
 * @author Phillip Webb
 */
class SessionAutoConfigurationEarlyInitializationIntegrationTests {

	@Test
	void configurationIsFrozenWhenSessionRepositoryAccessed() {
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
			.withSystemProperties("spring.jndi.ignore=true")
			.withPropertyValues("server.port=0")
			.withUserConfiguration(TestConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(MapSessionRepository.class));
	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	@ImportAutoConfiguration({ ServletWebServerFactoryAutoConfiguration.class, SessionAutoConfiguration.class })
	static class TestConfiguration {

		@Bean
		MapSessionRepository mapSessionRepository(ConfigurableApplicationContext context) {
			Assert.isTrue(context.getBeanFactory().isConfigurationFrozen(), "Context should be frozen");
			return new MapSessionRepository(new LinkedHashMap<>());
		}

	}

}
