/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.jersey.autoconfigure;

import jakarta.ws.rs.ext.ContextResolver;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the dependency-based Jackson 2 compatibility configuration.
 *
 * @author Kristoffer Larsen Hopland
 */
@SuppressWarnings("removal")
class JerseyJackson2AutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JerseyAutoConfiguration.class, JerseyJacksonAutoConfiguration.class))
		.withUserConfiguration(ResourceConfigConfiguration.class);

	@Test
	@SuppressWarnings("removal")
	void jackson2IsUsedByDefaultWithBothProvidersAvailable() {
		this.contextRunner
			.withConfiguration(AutoConfigurations
				.of(org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(ResourceConfigCustomizer.class);
				assertThat(context.getBean(ResourceConfig.class)
					.isRegistered(org.glassfish.jersey.jackson.JacksonFeature.class)).isTrue();
			});
	}

	@Test
	void jackson3IsUsedByDefaultWithoutJackson2Provider() {
		this.contextRunner
			.withClassLoader(new FilteredClassLoader((name) -> name.startsWith("org.glassfish.jersey.jackson.")))
			.run((context) -> {
				assertThat(context).hasNotFailed().hasSingleBean(ResourceConfigCustomizer.class);
				assertThat(context.getBean(ResourceConfig.class)
					.isRegistered(org.glassfish.jersey.jackson3.JacksonFeature.class)).isTrue();
			});
	}

	@Test
	void invalidPreferredJsonMapperFails() {
		this.contextRunner.withPropertyValues("spring.jersey.preferred-json-mapper=unknown")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.hasRootCauseMessage("spring.jersey.preferred-json-mapper must be 'jackson' or 'jackson2'"));
	}

	@Test
	void jackson2ProviderIsSelectedWithoutObjectMapper() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(ResourceConfigCustomizer.class);
			assertThat(context.getBean(ResourceConfig.class).getInstances())
				.noneMatch(ContextResolver.class::isInstance);
		});
	}

	@Test
	@SuppressWarnings("removal")
	void explicitJackson2PreferenceFailsWithoutJacksonFeature() {
		this.contextRunner.withPropertyValues("spring.jersey.preferred-json-mapper=jackson2")
			.withConfiguration(AutoConfigurations
				.of(org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader((name) -> name.startsWith("org.glassfish.jersey.jackson.")))
			.run((context) -> assertThat(context).hasFailed());
	}

	@Test
	void jackson2ProviderIsSelectedWithMultipleObjectMappers() {
		this.contextRunner
			.withBean("first", com.fasterxml.jackson.databind.ObjectMapper.class,
					com.fasterxml.jackson.databind.ObjectMapper::new)
			.withBean("second", com.fasterxml.jackson.databind.ObjectMapper.class,
					com.fasterxml.jackson.databind.ObjectMapper::new)
			.run((context) -> {
				assertThat(context).hasSingleBean(ResourceConfigCustomizer.class);
				assertThat(context.getBean(ResourceConfig.class).getInstances())
					.noneMatch(ContextResolver.class::isInstance);
			});
	}

	@Test
	void primaryJackson2ObjectMapperIsRegisteredWithJersey() {
		this.contextRunner
			.withBean("first", com.fasterxml.jackson.databind.ObjectMapper.class,
					com.fasterxml.jackson.databind.ObjectMapper::new)
			.withBean("primary", com.fasterxml.jackson.databind.ObjectMapper.class,
					com.fasterxml.jackson.databind.ObjectMapper::new, (definition) -> definition.setPrimary(true))
			.run((context) -> {
				ResourceConfig config = context.getBean(ResourceConfig.class);
				assertThat(config.getInstances()).filteredOn(ContextResolver.class::isInstance)
					.singleElement()
					.satisfies((resolver) -> assertThat(((ContextResolver<?>) resolver).getContext(Object.class))
						.isSameAs(context.getBean("primary")));
			});
	}

	@Test
	void existingObjectMapperRetainsJaxbSupport() {
		this.contextRunner
			.withConfiguration(AutoConfigurations
				.of(org.springframework.boot.jackson2.autoconfigure.Jackson2AutoConfiguration.class))
			.run((context) -> {
				com.fasterxml.jackson.databind.ObjectMapper mapper = context
					.getBean(com.fasterxml.jackson.databind.ObjectMapper.class);
				assertThat(mapper.getSerializationConfig().getAnnotationIntrospector().allIntrospectors()).filteredOn(
						com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector.class::isInstance)
					.hasSize(1);
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
