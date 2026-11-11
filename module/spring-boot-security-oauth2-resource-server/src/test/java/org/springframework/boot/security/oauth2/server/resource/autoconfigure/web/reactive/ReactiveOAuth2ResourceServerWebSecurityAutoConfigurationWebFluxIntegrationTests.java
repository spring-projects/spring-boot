/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.reactive;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.autoconfigure.AutoConfigurationImportedCondition.importedAutoConfiguration;

/**
 * Integration tests for reactive OAuth2 resource server with
 * {@link WebFluxTest @WebFluxTest}.
 *
 * @author Andy Wilkinson
 */
@WebFluxTest
class ReactiveOAuth2ResourceServerWebSecurityAutoConfigurationWebFluxIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void oauth2ResourceServerAutoConfigurationWasImported() {
		assertThat(this.applicationContext)
			.has(importedAutoConfiguration(ReactiveOAuth2ResourceServerAutoConfiguration.class));
	}

	@SpringBootConfiguration
	static class TestConfiguration {

	}

}
