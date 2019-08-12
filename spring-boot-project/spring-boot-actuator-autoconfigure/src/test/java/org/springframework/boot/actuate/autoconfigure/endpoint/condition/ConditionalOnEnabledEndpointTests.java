/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.condition;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnEnabledEndpoint @ConditionalOnEnabledEndpoint}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
@Deprecated
@SuppressWarnings("deprecation")
class ConditionalOnEnabledEndpointTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void outcomeWhenEndpointEnabledPropertyIsTrueShouldMatch() {
		this.contextRunner.withPropertyValues("management.endpoint.foo.enabled=true")
				.withUserConfiguration(FooEndpointEnabledByDefaultFalseConfiguration.class)
				.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	void outcomeWhenEndpointEnabledPropertyIsFalseShouldNotMatch() {
		this.contextRunner.withPropertyValues("management.endpoint.foo.enabled=false")
				.withUserConfiguration(FooEndpointEnabledByDefaultTrueConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("foo"));
	}

	@Test
	void outcomeWhenNoEndpointPropertyAndUserDefinedDefaultIsTrueShouldMatch() {
		this.contextRunner.withPropertyValues("management.endpoints.enabled-by-default=true")
				.withUserConfiguration(FooEndpointEnabledByDefaultFalseConfiguration.class)
				.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	void outcomeWhenNoEndpointPropertyAndUserDefinedDefaultIsFalseShouldNotMatch() {
		this.contextRunner.withPropertyValues("management.endpoints.enabled-by-default=false")
				.withUserConfiguration(FooEndpointEnabledByDefaultTrueConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("foo"));
	}

	@Test
	void outcomeWhenNoPropertiesAndAnnotationIsEnabledByDefaultShouldMatch() {
		this.contextRunner.withUserConfiguration(FooEndpointEnabledByDefaultTrueConfiguration.class)
				.run((context) -> assertThat(context).hasBean("foo"));
	}

	@Test
	void outcomeWhenNoPropertiesAndAnnotationIsNotEnabledByDefaultShouldNotMatch() {
		this.contextRunner.withUserConfiguration(FooEndpointEnabledByDefaultFalseConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("foo"));
	}

	@Test
	void outcomeWhenNoPropertiesAndExtensionAnnotationIsEnabledByDefaultShouldMatch() {
		this.contextRunner.withUserConfiguration(FooEndpointAndExtensionEnabledByDefaultTrueConfiguration.class)
				.run((context) -> assertThat(context).hasBean("foo").hasBean("fooExt"));
	}

	@Test
	void outcomeWhenNoPropertiesAndExtensionAnnotationIsNotEnabledByDefaultShouldNotMatch() {
		this.contextRunner.withUserConfiguration(FooEndpointAndExtensionEnabledByDefaultFalseConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("foo").doesNotHaveBean("fooExt"));
	}

	@Test
	void outcomeWithReferenceWhenNoPropertiesShouldMatch() {
		this.contextRunner
				.withUserConfiguration(FooEndpointEnabledByDefaultTrue.class,
						ComponentEnabledIfEndpointIsEnabledConfiguration.class)
				.run((context) -> assertThat(context).hasBean("fooComponent"));
	}

	@Test
	void outcomeWithReferenceWhenEndpointEnabledPropertyIsTrueShouldMatch() {
		this.contextRunner.withPropertyValues("management.endpoint.foo.enabled=true")
				.withUserConfiguration(FooEndpointEnabledByDefaultTrue.class,
						ComponentEnabledIfEndpointIsEnabledConfiguration.class)
				.run((context) -> assertThat(context).hasBean("fooComponent"));
	}

	@Test
	void outcomeWithReferenceWhenEndpointEnabledPropertyIsFalseShouldNotMatch() {
		this.contextRunner.withPropertyValues("management.endpoint.foo.enabled=false")
				.withUserConfiguration(FooEndpointEnabledByDefaultTrue.class,
						ComponentEnabledIfEndpointIsEnabledConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("fooComponent"));
	}

	@Test
	void outcomeWithNoReferenceShouldFail() {
		this.contextRunner.withUserConfiguration(ComponentWithNoEndpointReferenceConfiguration.class).run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure().getCause().getMessage())
					.contains("No endpoint is specified and the return type of the @Bean method "
							+ "is neither an @Endpoint, nor an @EndpointExtension");
		});
	}

	@Test
	void outcomeWhenEndpointEnabledPropertyIsTrueAndMixedCaseShouldMatch() {
		this.contextRunner.withPropertyValues("management.endpoint.foo-bar.enabled=true")
				.withUserConfiguration(FooBarEndpointEnabledByDefaultFalseConfiguration.class)
				.run((context) -> assertThat(context).hasBean("fooBar"));
	}

	@Test
	void outcomeWhenEndpointEnabledPropertyIsFalseOnClassShouldNotMatch() {
		this.contextRunner.withPropertyValues("management.endpoint.foo.enabled=false")
				.withUserConfiguration(FooEndpointEnabledByDefaultTrueOnConfigurationConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean("foo"));
	}

	@Endpoint(id = "foo", enableByDefault = true)
	static class FooEndpointEnabledByDefaultTrue {

	}

	@Endpoint(id = "foo", enableByDefault = false)
	static class FooEndpointEnabledByDefaultFalse {

	}

	@Endpoint(id = "fooBar", enableByDefault = false)
	static class FooBarEndpointEnabledByDefaultFalse {

	}

	@EndpointExtension(endpoint = FooEndpointEnabledByDefaultTrue.class, filter = TestFilter.class)
	static class FooEndpointExtensionEnabledByDefaultTrue {

	}

	@EndpointExtension(endpoint = FooEndpointEnabledByDefaultFalse.class, filter = TestFilter.class)
	static class FooEndpointExtensionEnabledByDefaultFalse {

	}

	static class TestFilter implements EndpointFilter<ExposableEndpoint<?>> {

		@Override
		public boolean match(ExposableEndpoint<?> endpoint) {
			return true;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FooEndpointEnabledByDefaultTrueConfiguration {

		@Bean
		@ConditionalOnEnabledEndpoint
		FooEndpointEnabledByDefaultTrue foo() {
			return new FooEndpointEnabledByDefaultTrue();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnEnabledEndpoint(endpoint = FooEndpointEnabledByDefaultTrue.class)
	static class FooEndpointEnabledByDefaultTrueOnConfigurationConfiguration {

		@Bean
		FooEndpointEnabledByDefaultTrue foo() {
			return new FooEndpointEnabledByDefaultTrue();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FooEndpointEnabledByDefaultFalseConfiguration {

		@Bean
		@ConditionalOnEnabledEndpoint
		FooEndpointEnabledByDefaultFalse foo() {
			return new FooEndpointEnabledByDefaultFalse();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FooBarEndpointEnabledByDefaultFalseConfiguration {

		@Bean
		@ConditionalOnEnabledEndpoint
		FooBarEndpointEnabledByDefaultFalse fooBar() {
			return new FooBarEndpointEnabledByDefaultFalse();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FooEndpointAndExtensionEnabledByDefaultTrueConfiguration {

		@Bean
		@ConditionalOnEnabledEndpoint
		FooEndpointEnabledByDefaultTrue foo() {
			return new FooEndpointEnabledByDefaultTrue();
		}

		@Bean
		@ConditionalOnEnabledEndpoint
		FooEndpointExtensionEnabledByDefaultTrue fooExt() {
			return new FooEndpointExtensionEnabledByDefaultTrue();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FooEndpointAndExtensionEnabledByDefaultFalseConfiguration {

		@Bean
		@ConditionalOnEnabledEndpoint
		FooEndpointEnabledByDefaultFalse foo() {
			return new FooEndpointEnabledByDefaultFalse();
		}

		@Bean
		@ConditionalOnEnabledEndpoint
		FooEndpointExtensionEnabledByDefaultFalse fooExt() {
			return new FooEndpointExtensionEnabledByDefaultFalse();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ComponentEnabledIfEndpointIsEnabledConfiguration {

		@Bean
		@ConditionalOnEnabledEndpoint(endpoint = FooEndpointEnabledByDefaultTrue.class)
		String fooComponent() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ComponentWithNoEndpointReferenceConfiguration {

		@Bean
		@ConditionalOnEnabledEndpoint
		String fooComponent() {
			return "foo";
		}

	}

}
