/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.web;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration tests for reactive OAuth2 resource server with
 * {@link WebMvcTest @WebMvcTest}.
 *
 * @author Andy Wilkinson
 */
@WebMvcTest
class OAuth2ResourceServerWebSecurityAutoConfigurationMvcIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void oauth2ResourceServerAutoConfigurationWasImported() {
		assertThat(this.context).has(importedAutoConfiguration(OAuth2ResourceServerAutoConfiguration.class));
		assertThat(this.context).has(importedAutoConfiguration(OAuth2ResourceServerWebSecurityAutoConfiguration.class));
	}

	@SpringBootConfiguration
	static class TestConfiguration {

	}

}
