/*
 * Copyright 2012-present the original author or authors.
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
	@SuppressWarnings("removal")
	void mockServerRestTemplateCustomizerShouldNotBeRegistered() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(MockServerRestTemplateCustomizer.class));
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(MockServerRestClientCustomizer.class));
	}

}
