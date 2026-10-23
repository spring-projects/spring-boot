/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jersey.autoconfigure;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests run with the Jackson 2 Jersey provider excluded from the runtime classpath.
 *
 * @author Kristoffer Larsen Hopland
 */
class JerseyJackson3OnlyIntegrationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
			AnnotationConfigServletWebServerApplicationContext::new)
		.withConfiguration(AutoConfigurations.of(TomcatServletWebServerAutoConfiguration.class,
				JerseyAutoConfiguration.class, JerseyJacksonAutoConfiguration.class, JacksonAutoConfiguration.class))
		.withBean(ResourceConfig.class,
				() -> new ResourceConfig(JerseyJacksonAutoConfigurationIntegrationTests.Endpoint.class))
		.withPropertyValues("server.port=0");

	@Test
	void excludingJackson2ProviderSelectsJackson3WithoutProperty() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasNotFailed();
			AnnotationConfigServletWebServerApplicationContext applicationContext = context
				.getSourceApplicationContext(AnnotationConfigServletWebServerApplicationContext.class);
			assertThat(applicationContext.getWebServer()).isNotNull();
			assertThat(RestClient.create("http://localhost:" + applicationContext.getWebServer().getPort())
				.get()
				.uri("/engine")
				.retrieve()
				.body(String.class)).isEqualTo("{\"first_name\":\"value\"}");
		});
	}

	@Test
	void explicitJackson2PreferenceFailsWhenProviderIsExcluded() {
		this.contextRunner.withPropertyValues("spring.jersey.preferred-json-mapper=jackson2")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.hasRootCauseMessage(
						"The Jersey JSON provider selected by spring.jersey.preferred-json-mapper=jackson2 is not available"));
	}

}
