/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.web.mappings;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.web.mappings.MappingsEndpoint.ApplicationMappingsDescriptor;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint.ContextMappingsDescriptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MappingsEndpoint}.
 *
 * @author Lee JiWon
 */
class MappingsEndpointTests {

	@Test
	void mappingsParentIdMatchesParentContextId() {
		ApplicationContextRunner parentRunner = new ApplicationContextRunner();
		parentRunner.run((parent) -> new ApplicationContextRunner().withUserConfiguration(EndpointConfiguration.class)
			.withParent(parent)
			.run((child) -> {
				ApplicationMappingsDescriptor result = child.getBean(MappingsEndpoint.class).mappings();
				ContextMappingsDescriptor parentDescriptor = result.getContexts().get(parent.getId());
				ContextMappingsDescriptor childDescriptor = result.getContexts().get(child.getId());
				assertThat(parentDescriptor).isNotNull();
				assertThat(parentDescriptor.getParentId()).isNull();
				assertThat(childDescriptor).isNotNull();
				assertThat(childDescriptor.getParentId()).isEqualTo(parent.getId());
			}));
	}

	@Configuration(proxyBeanMethods = false)
	static class EndpointConfiguration {

		@Bean
		MappingsEndpoint mappingsEndpoint(ConfigurableApplicationContext context) {
			return new MappingsEndpoint(Collections.emptyList(), context);
		}

	}

}
