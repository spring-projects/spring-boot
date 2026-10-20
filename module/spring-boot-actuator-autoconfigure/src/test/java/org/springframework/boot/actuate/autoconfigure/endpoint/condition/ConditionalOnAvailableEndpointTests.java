/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint.condition;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.endpoint.expose.EndpointExposure;
import org.springframework.boot.actuate.endpoint.Access;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.EndpointExtension;
import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ConditionalOnAvailableEndpoint @ConditionalOnAvailableEndpoint}.
 *
 * @author Brian Clozel
 */
class ConditionalOnAvailableEndpointTests {

	private static final String CACHE_BEAN_NAME = OnAvailableEndpointCondition.class.getName() + ".cache";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(AllEndpointsConfiguration.class)
		.withInitializer(
				(context) -> context.getEnvironment().setConversionService(new ApplicationConversionService()));

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
				Throwable startupFailure = context.getStartupFailure();
				assertThat(startupFailure).isNotNull();
				Throwable cause = startupFailure.getCause();
				assertThat(cause).isNotNull();
				assertThat(cause.getMessage())
					.contains("No endpoint is specified and the return type of the @Bean method "
							+ "is neither an @Endpoint, nor an @EndpointExtension");
			});
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

	@Test
	void whenBothAccessAndEnabledAreConfiguredThenThrows() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=*",
					"management.endpoint.shutdown.enabled=true", "management.endpoint.shutdown.access=none")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.isInstanceOf(MutuallyExclusiveConfigurationPropertiesException.class));
	}

	@Test
	void whenBothDefaultAccessAndDefaultEnabledAreConfiguredThenThrows() {
		this.contextRunner
			.withPropertyValues("management.endpoints.web.exposure.include=*",
					"management.endpoints.enabled-by-default=true", "management.endpoints.access.default=none")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.isInstanceOf(MutuallyExclusiveConfigurationPropertiesException.class));
	}

	@Test
	@WithTestEndpointOutcomeExposureContributor
	void exposureOutcomeContributorCanMakeEndpointAvailable() {
		this.contextRunner.withPropertyValues("management.endpoints.test.exposure.include=test")
			.run((context) -> assertThat(context).hasSingleBean(TestEndpoint.class));
	}

	@Test
	void conditionDoesNotUseStaticMapCaches() {
		for (Field field : OnAvailableEndpointCondition.class.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				assertThat(Map.class.isAssignableFrom(field.getType()))
					.as("Field %s must not be a static Map", field.getName())
					.isFalse();
			}
		}
	}

	@Test
	void endpointAvailabilityCacheIsCleanedUpWhenContextCloses() {
		ConfigurableListableBeanFactory beanFactory;
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.register(AllEndpointsConfiguration.class);
			context.refresh();
			beanFactory = context.getBeanFactory();
			assertThat(beanFactory.containsSingleton(CACHE_BEAN_NAME)).isTrue();
			assertThat(beanFactory.getSingleton(CACHE_BEAN_NAME)).isNotNull();
			assertThat(context.containsBean("health")).isTrue();
		}
		assertThat(beanFactory.containsSingleton(CACHE_BEAN_NAME)).isFalse();
		assertThat(beanFactory.getSingleton(CACHE_BEAN_NAME)).isNull();
	}

	@Test
	void endpointAvailabilityCacheIsCleanedUpWhenContextRefreshFails() {
		FailingConfiguration.cacheInitializedBeforeFailure.set(false);
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.register(FailingConfiguration.class);
			assertThatExceptionOfType(BeanCreationException.class).isThrownBy(context::refresh)
				.withMessageContaining("Simulated refresh failure");
			assertThat(FailingConfiguration.cacheInitializedBeforeFailure).isTrue();
			ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
			assertThat(beanFactory.containsSingleton(CACHE_BEAN_NAME)).isFalse();
			assertThat(beanFactory.getSingleton(CACHE_BEAN_NAME)).isNull();
		}
	}

	@Test
	void exposureContributorsAreNotLoadedWhenAccessIsNone() {
		this.contextRunner.withPropertyValues("management.endpoints.access.default=none").run((context) -> {
			assertThat(context).doesNotHaveBean("health");
			Object availability = context.getBeanFactory().getSingleton(CACHE_BEAN_NAME);
			assertThat(availability).isNotNull();
			Object exposureContributors = ReflectionTestUtils.getField(availability, "exposureOutcomeContributors");
			assertThat(exposureContributors).isNull();
		});
	}

	@Test
	void parentAndChildContextsSharingEnvironmentDoNotInterfere() {
		MockEnvironment sharedEnv = new MockEnvironment();
		sharedEnv.setProperty("management.endpoints.web.exposure.include", "*");
		try (AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
				AnnotationConfigApplicationContext child = new AnnotationConfigApplicationContext()) {
			parent.setEnvironment(sharedEnv);
			parent.register(AllEndpointsConfiguration.class);
			parent.refresh();

			child.setParent(parent);
			child.setEnvironment(sharedEnv);
			child.register(AllEndpointsConfiguration.class);
			child.refresh();

			ConfigurableListableBeanFactory parentBeanFactory = parent.getBeanFactory();
			ConfigurableListableBeanFactory childBeanFactory = child.getBeanFactory();
			Object parentCache = parentBeanFactory.getSingleton(CACHE_BEAN_NAME);
			Object childCache = childBeanFactory.getSingleton(CACHE_BEAN_NAME);
			assertThat(parentCache).isNotNull();
			assertThat(childCache).isNotNull();
			assertThat(childCache).isNotSameAs(parentCache);

			child.close();
			assertThat(childBeanFactory.containsSingleton(CACHE_BEAN_NAME)).isFalse();
			assertThat(childBeanFactory.getSingleton(CACHE_BEAN_NAME)).isNull();
			assertThat(parent.isActive()).isTrue();
			assertThat(parent.containsBean("health")).isTrue();
			assertThat(parentBeanFactory.getSingleton(CACHE_BEAN_NAME)).isSameAs(parentCache);
		}
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

	@Endpoint(id = "shutdown", defaultAccess = Access.NONE)
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
		@ConditionalOnAvailableEndpoint(SpringEndpoint.class)
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
		@ConditionalOnAvailableEndpoint(endpoint = TestEndpoint.class, exposure = EndpointExposure.WEB)
		String unexposed() {
			return "unexposed";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FailingConfiguration {

		static final AtomicBoolean cacheInitializedBeforeFailure = new AtomicBoolean();

		@Bean
		@ConditionalOnAvailableEndpoint
		HealthEndpoint health() {
			return new HealthEndpoint();
		}

		@Bean
		String failingBean(BeanFactory beanFactory) {
			if (beanFactory instanceof ConfigurableListableBeanFactory listable) {
				cacheInitializedBeforeFailure.set(listable.containsSingleton(CACHE_BEAN_NAME));
			}
			throw new RuntimeException("Simulated refresh failure");
		}

	}

}
