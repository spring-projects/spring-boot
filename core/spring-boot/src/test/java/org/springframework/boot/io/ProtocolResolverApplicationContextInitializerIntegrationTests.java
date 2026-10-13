/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.io;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import org.jspecify.annotations.Nullable;
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

		@Nullable Resource resource;

		@Nullable Resource getResource() {
			return this.resource;
		}

		void setResource(@Nullable Resource resource) {
			this.resource = resource;
		}

	}

}
