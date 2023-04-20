/*
 * Copyright 2012-2023 the original author or authors.
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

import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConditionalOnAvailableEndpoint @ConditionalOnAvailableEndpoint}.
 *
 * @author Brian Clozel
 */
class ConditionalOnAvailableEndpointTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(AllEndpointsConfiguration.class);

	@Test
	void outcomeShouldMatchDefaults() {
		this.contextRunner.run((context) -> assertThat(context).hasBean("health")
			.doesNotHaveBean("spring")
			.doesNotHaveBean("test")
			.doesNotHaveBean("shutdown"));
	}

	@Test
	void outcomeWithEnabledByDefaultSetToFalseShouldNotMatchAnything() {
		this.contextRunner.withPropertyValues("management.endpoints.enabled-by-default=false")
			.run((context) -> assertThat(context).doesNotHaveBean("info")
				.doesNotHaveBean("health")
				.doesNotHaveBean("spring")
				.doesNotHaveBean("test")
				.doesNotHaveBean("shutdown"));
	}

	@Test
	void outcomeWhenIncludeAllWebShouldMatchEnabledEndpoints() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=*")
			.run((context) -> assertThat(context).hasBean("info")
				.hasBean("health")
				.hasBean("test")
				.hasBean("spring")
				.doesNotHaveBean("shutdown"));
	}

	@Test
	void outcomeWhenIncludeAllWebAndDisablingEndpointShouldMatchEnabledEndpoints() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=*", "management.endpoint.test.enabled=false",
					"management.endpoint.health.enabled=false")
			.run((context) -> assertThat(context).hasBean("info")
				.doesNotHaveBean("health")
				.doesNotHaveBean("test")
				.hasBean("spring")
				.doesNotHaveBean("shutdown"));
	}

	@Test
	void outcomeWhenIncludeAllWebAndEnablingEndpointDisabledByDefaultShouldMatchAll() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=*",
					"management.endpoint.shutdown.enabled=true")
			.run((context) -> assertThat(context).hasBean("info")
				.hasBean("health")
				.hasBean("test")
				.hasBean("spring")
				.hasBean("shutdown"));
	}

	@Test
	void outcomeWhenIncludeAllJmxButJmxDisabledShouldMatchDefaults() {
		this.contextRunner.withPropertyValues("management.endpoints.jmx.exposure.include=*")
			.run((context) -> assertThat(context).hasBean("health")
				.doesNotHaveBean("spring")
				.doesNotHaveBean("test")
				.doesNotHaveBean("shutdown"));
	}

	@Test
	void outcomeWhenIncludeAllJmxAndJmxEnabledShouldMatchEnabledEndpoints() {
		this.contextRunner.withPropertyValues("management.endpoints.jmx.exposure.include=*", "spring.jmx.enabled=true")
			.run((context) -> assertThat(context).hasBean("info")
				.hasBean("health")
				.hasBean("test")
				.hasBean("spring")
				.doesNotHaveBean("shutdown"));
	}

	@Test
	void outcomeWhenIncludeAllJmxAndJmxEnabledAndEnablingEndpointDisabledByDefaultShouldMatchAll() {
		this.contextRunner
			.withPropertyValues("management.endpoints.jmx.exposure.include=*", "spring.jmx.enabled=true",
					"management.endpoint.shutdown.enabled=true")
			.run((context) -> assertThat(context).hasBean("health")
				.hasBean("test")
				.hasBean("spring")
				.hasBean("shutdown"));
	}

	@Test
	void outcomeWhenIncludeAllWebAndExcludeMatchesShouldNotMatch() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=*",
					"management.endpoints.web.exposure.exclude=spring,info")
			.run((context) -> assertThat(context).hasBean("health")
				.hasBean("test")
				.doesNotHaveBean("info")
				.doesNotHaveBean("spring")
				.doesNotHaveBean("shutdown"));
	}

	@Test
	void outcomeWhenIncludeMatchesAndExcludeMatchesShouldNotMatch() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=info,health,spring,test",
					"management.endpoints.web.exposure.exclude=spring,info")
			.run((context) -> assertThat(context).hasBean("health")
				.hasBean("test")
				.doesNotHaveBean("info")
				.doesNotHaveBean("spring")
				.doesNotHaveBean("shutdown"));
	}

	@Test
	void outcomeWhenIncludeMatchesShouldMatchEnabledEndpoints() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=spring")
			.run((context) -> assertThat(context).hasBean("spring")
				.doesNotHaveBean("health")
				.doesNotHaveBean("info")
				.doesNotHaveBean("test")
				.doesNotHaveBean("shutdown"));
	}

	@Test
	void outcomeWhenIncludeMatchOnDisabledEndpointShouldNotMatch() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=shutdown")
			.run((context) -> assertThat(context).doesNotHaveBean("spring")
				.doesNotHaveBean("health")
				.doesNotHaveBean("info")
				.doesNotHaveBean("test")
				.doesNotHaveBean("shutdown"));
	}

	@Test
	void outcomeWhenIncludeMatchOnEnabledEndpointShouldNotMatch() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=shutdown",
					"management.endpoint.shutdown.enabled=true")
			.run((context) -> assertThat(context).doesNotHaveBean("spring")
				.doesNotHaveBean("health")
				.doesNotHaveBean("info")
				.doesNotHaveBean("test")
				.hasBean("shutdown"));
	}

	@Test
	void outcomeWhenIncludeMatchesWithCaseShouldMatch() {
		this.contextRunner.withPropertyValues("management.endpoints.web.exposure.include=sPRing")
			.run((context) -> assertThat(context).hasBean("spring")
				.doesNotHaveBean("health")
				.doesNotHaveBean("info")
				.doesNotHaveBean("test")
				.doesNotHaveBean("shutdown"));
	}

	@Test
	void outcomeWhenIncludeMatchesAndExcludeAllShouldNotMatch() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=info,health,spring,test",
					"management.endpoints.web.exposure.exclude=*")
			.run((context) -> assertThat(context).doesNotHaveBean("health")
				.doesNotHaveBean("info")
				.doesNotHaveBean("spring")
				.doesNotHaveBean("test")
				.doesNotHaveBean("shutdown"));
	}

	@Test
	void outcomeWhenIncludeMatchesShouldMatchWithExtensionsAndComponents() {
		this.contextRunner.withUserConfiguration(ComponentEnabledIfEndpointIsExposedConfiguration.class)
			.withPropertyValues("management.endpoints.web.exposure.include=spring")
			.run((context) -> assertThat(context).hasBean("spring")
				.hasBean("springComponent")
				.hasBean("springExtension")
				.doesNotHaveBean("info")
				.doesNotHaveBean("health")
				.doesNotHaveBean("test")
				.doesNotHaveBean("shutdown"));
	}

	@Test
	void outcomeWithNoEndpointReferenceShouldFail() {
		this.contextRunner.withUserConfiguration(ComponentWithNoEndpointReferenceConfiguration.class)
			.withPropertyValues("management.endpoints.web.exposure.include=*")
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure().getCause().getMessage())
					.contains("No endpoint is specified and the return type of the @Bean method "
							+ "is neither an @Endpoint, nor an @EndpointExtension");
			});
	}

	@Test
	void outcomeOnCloudFoundryShouldMatchAll() {
		this.contextRunner.withPropertyValues("VCAP_APPLICATION:---")
			.run((context) -> assertThat(context).hasBean("info").hasBean("health").hasBean("spring").hasBean("test"));
	}

	@Test // gh-21044
	void outcomeWhenIncludeAllShouldMatchDashedEndpoint() {
		this.contextRunner.withUserConfiguration(DashedEndpointConfiguration.class)
			.withPropertyValues("management.endpoints.web.exposure.include=*")
			.run((context) -> assertThat(context).hasSingleBean(DashedEndpoint.class));
	}

	@Test // gh-21044
	void outcomeWhenIncludeDashedShouldMatchDashedEndpoint() {
		this.contextRunner.withUserConfiguration(DashedEndpointConfiguration.class)
			.withPropertyValues("management.endpoints.web.exposure.include=test-dashed")
			.run((context) -> assertThat(context).hasSingleBean(DashedEndpoint.class));
	}

	@Test
	void outcomeWhenEndpointNotExposedOnSpecifiedTechnology() {
		this.contextRunner.withUserConfiguration(ExposureEndpointConfiguration.class)
			.withPropertyValues("spring.jmx.enabled=true", "management.endpoints.jmx.exposure.include=test",
					"management.endpoints.web.exposure.exclude=test")
			.run((context) -> assertThat(context).doesNotHaveBean("unexposed"));
	}

	@Endpoint(id = "health")
	static class HealthEndpoint {

	}

	@Endpoint(id = "info")
	static class InfoEndpoint {

	}

	@Endpoint(id = "spring")
	static class SpringEndpoint {

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

	}

	@Endpoint(id = "shutdown", enableByDefault = false)
	static class ShutdownEndpoint {

	}

	@Endpoint(id = "test-dashed")
	static class DashedEndpoint {

	}

	@EndpointExtension(endpoint = SpringEndpoint.class, filter = TestFilter.class)
	static class SpringEndpointExtension {

	}

	static class TestFilter implements EndpointFilter<ExposableEndpoint<?>> {

		@Override
		public boolean match(ExposableEndpoint<?> endpoint) {
			return true;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AllEndpointsConfiguration {

		@Bean
		@ConditionalOnAvailableEndpoint
		HealthEndpoint health() {
			return new HealthEndpoint();
		}

		@Bean
		@ConditionalOnAvailableEndpoint
		InfoEndpoint info() {
			return new InfoEndpoint();
		}

		@Bean
		@ConditionalOnAvailableEndpoint
		SpringEndpoint spring() {
			return new SpringEndpoint();
		}

		@Bean
		@ConditionalOnAvailableEndpoint
		TestEndpoint test() {
			return new TestEndpoint();
		}

		@Bean
		@ConditionalOnAvailableEndpoint
		ShutdownEndpoint shutdown() {
			return new ShutdownEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ComponentEnabledIfEndpointIsExposedConfiguration {

		@Bean
		@ConditionalOnAvailableEndpoint(endpoint = SpringEndpoint.class)
		String springComponent() {
			return "springComponent";
		}

		@Bean
		@ConditionalOnAvailableEndpoint
		SpringEndpointExtension springExtension() {
			return new SpringEndpointExtension();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ComponentWithNoEndpointReferenceConfiguration {

		@Bean
		@ConditionalOnAvailableEndpoint
		String springcomp() {
			return "springcomp";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DashedEndpointConfiguration {

		@Bean
		@ConditionalOnAvailableEndpoint
		DashedEndpoint dashedEndpoint() {
			return new DashedEndpoint();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ExposureEndpointConfiguration {

		@Bean
		@ConditionalOnAvailableEndpoint(endpoint = TestEndpoint.class,
				exposure = { EndpointExposure.WEB, EndpointExposure.CLOUD_FOUNDRY })
		String unexposed() {
			return "unexposed";
		}

	}

}
