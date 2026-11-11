/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.http.client;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DisableReactorResourceFactoryGlobalResourcesContextCustomizerFactory}.
 *
 * @author Phillip Webb
 */
@SpringJUnitConfig
class DisableReactorResourceFactoryGlobalResourcesContextCustomizerFactoryTests {

	@Autowired
	private ReactorResourceFactory reactorResourceFactory;

	@Test
	void disablesUseGlobalResources() {
		assertThat(this.reactorResourceFactory.isUseGlobalResources()).isFalse();
	}

	@Configuration
	static class Config {

		@Bean
		ReactorResourceFactory reactorResourceFactory() {
			return new ReactorResourceFactory();
		}

	}

}
