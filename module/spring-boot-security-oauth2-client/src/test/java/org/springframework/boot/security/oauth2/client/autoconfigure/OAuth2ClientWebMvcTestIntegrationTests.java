/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.client.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration tests for Mustache with {@link WebMvcTest @WebMvcTest}.
 *
 * @author Andy Wilkinson
 */
@WebMvcTest
class OAuth2ClientWebMvcTestIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void oauth2ClientAutoConfigurationWasImported() {
		assertThat(this.applicationContext).has(importedAutoConfiguration(OAuth2ClientAutoConfiguration.class));
	}

	@SpringBootConfiguration
	static class TestConfiguration {

	}

}
