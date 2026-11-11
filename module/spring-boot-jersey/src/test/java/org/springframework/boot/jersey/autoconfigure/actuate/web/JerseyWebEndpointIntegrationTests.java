/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jersey.autoconfigure.actuate.web;

import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jersey.autoconfigure.JerseyAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
			.withConfiguration(
					AutoConfigurations.of(JerseySameManagementContextConfiguration.class, JerseyAutoConfiguration.class,
							TomcatServletWebServerAutoConfiguration.class, EndpointAutoConfiguration.class,
							WebEndpointAutoConfiguration.class, JerseyWebEndpointManagementContextConfiguration.class))
			.withUserConfiguration(ResourceConfigConfiguration.class)
			.withPropertyValues("spring.jersey.type=filter", "server.port=0")
			.run((context) -> {
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
