/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

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
			.containsOnly("example-" + ExampleProperties.class.getName());
		assertThat(this.applicationContext.getBean(ExampleProperties.class).getName()).isEqualTo("Hello");
	}

}
