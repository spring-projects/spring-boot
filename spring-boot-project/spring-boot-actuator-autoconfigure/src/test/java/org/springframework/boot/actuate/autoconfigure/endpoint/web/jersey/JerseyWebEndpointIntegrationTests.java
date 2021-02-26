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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.jersey;

import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.jersey.JerseySameManagementContextConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for web endpoints running on Jersey.
 *
 * @author Andy Wilkinson
 */
class JerseyWebEndpointIntegrationTests {

	@Test
	void whenJerseyIsConfiguredToUseAFilterThenResourceRegistrationSucceeds() {
		new WebApplicationContextRunner(AnnotationConfigServletWebServerApplicationContext::new)
				.withConfiguration(AutoConfigurations.of(JerseySameManagementContextConfiguration.class,
						JerseyAutoConfiguration.class, ServletWebServerFactoryAutoConfiguration.class,
						EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
						JerseyWebEndpointManagementContextConfiguration.class))
				.withUserConfiguration(ResourceConfigConfiguration.class)
				.withClassLoader(new FilteredClassLoader(DispatcherServlet.class))
				.withInitializer(new ConditionEvaluationReportLoggingListener(LogLevel.INFO))
				.withPropertyValues("spring.jersey.type=filter", "server.port=0").run((context) -> {
					assertThat(context).hasNotFailed();
					Set<Resource> resources = context.getBean(ResourceConfig.class).getResources();
					assertThat(resources).hasSize(1);
					Resource resource = resources.iterator().next();
					assertThat(resource.getPath()).isEqualTo("/actuator");
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class ResourceConfigConfiguration {

		@Bean
		ResourceConfig resourceConfig() {
			return new ResourceConfig();
		}

	}

}
