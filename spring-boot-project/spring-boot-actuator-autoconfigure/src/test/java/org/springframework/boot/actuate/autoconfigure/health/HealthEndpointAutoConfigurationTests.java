/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.WithTestEndpointOutcomeExposureContributor;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointConfiguration.HealthEndpointGroupMembershipValidator.NoSuchHealthContributorException;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointReactiveWebExtensionConfiguration.WebFluxAdditionalHealthEndpointPathsConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointWebExtensionConfiguration.JerseyAdditionalHealthEndpointPathsConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointWebExtensionConfiguration.MvcAdditionalHealthEndpointPathsConfiguration;
import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.WebServerNamespace;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.DefaultReactiveHealthContributorRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HealthEndpointGroupsPostProcessor;
import org.springframework.boot.actuate.health.HealthEndpointWebExtension;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.boot.actuate.health.ReactiveHealthContributorRegistry;
import org.springframework.boot.actuate.health.ReactiveHealthEndpointWebExtension;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.actuate.health.SystemHealth;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HealthEndpointAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
class HealthEndpointAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withUserConfiguration(HealthIndicatorsConfiguration.class)
		.withConfiguration(
				AutoConfigurations.of(HealthContributorAutoConfiguration.class, HealthEndpointAutoConfiguration.class));

	private final ReactiveWebApplicationContextRunner reactiveContextRunner = new ReactiveWebApplicationContextRunner()
		.withUserConfiguration(HealthIndicatorsConfiguration.class)
		.withConfiguration(
				AutoConfigurations.of(HealthContributorAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
						WebEndpointAutoConfiguration.class, EndpointAutoConfiguration.class));

	@Test
	void runWhenHealthEndpointIsDisabledDoesNotCreateBeans() {
		this.contextRunner.withPropertyValues("management.endpoint.health.enabled=false").run((context) -> {
			assertThat(context).doesNotHaveBean(StatusAggregator.class);
			assertThat(context).doesNotHaveBean(HttpCodeStatusMapper.class);
			assertThat(context).doesNotHaveBean(HealthEndpointGroups.class);
			assertThat(context).doesNotHaveBean(HealthContributorRegistry.class);
			assertThat(context).doesNotHaveBean(HealthEndpoint.class);
			assertThat(context).doesNotHaveBean(ReactiveHealthContributorRegistry.class);
			assertThat(context).doesNotHaveBean(HealthEndpointWebExtension.class);
			assertThat(context).doesNotHaveBean(ReactiveHealthEndpointWebExtension.class);
		});
	}

	@Test
	void runCreatesStatusAggregatorFromProperties() {
		this.contextRunner.withPropertyValues("management.endpoint.health.status.order=up,down").run((context) -> {
			StatusAggregator aggregator = context.getBean(StatusAggregator.class);
			assertThat(aggregator.getAggregateStatus(Status.UP, Status.DOWN)).isEqualTo(Status.UP);
		});
	}

	@Test
	void runWhenHasStatusAggregatorBeanIgnoresProperties() {
		this.contextRunner.withUserConfiguration(StatusAggregatorConfiguration.class)
			.withPropertyValues("management.endpoint.health.status.order=up,down")
			.run((context) -> {
				StatusAggregator aggregator = context.getBean(StatusAggregator.class);
				assertThat(aggregator.getAggregateStatus(Status.UP, Status.DOWN)).isEqualTo(Status.UNKNOWN);
			});
	}

	@Test
	void runCreatesHttpCodeStatusMapperFromProperties() {
		this.contextRunner.withPropertyValues("management.endpoint.health.status.http-mapping.up=123")
			.run((context) -> {
				HttpCodeStatusMapper mapper = context.getBean(HttpCodeStatusMapper.class);
				assertThat(mapper.getStatusCode(Status.UP)).isEqualTo(123);
			});
	}

	@Test
	void runWhenHasHttpCodeStatusMapperBeanIgnoresProperties() {
		this.contextRunner.withUserConfiguration(HttpCodeStatusMapperConfiguration.class)
			.withPropertyValues("management.endpoint.health.status.http-mapping.up=123")
			.run((context) -> {
				HttpCodeStatusMapper mapper = context.getBean(HttpCodeStatusMapper.class);
				assertThat(mapper.getStatusCode(Status.UP)).isEqualTo(456);
			});
	}

	@Test
	void runCreatesHealthEndpointGroups() {
		this.contextRunner.withPropertyValues("management.endpoint.health.group.ready.include=*").run((context) -> {
			HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
			assertThat(groups).isInstanceOf(AutoConfiguredHealthEndpointGroups.class);
			assertThat(groups.getNames()).containsOnly("ready");
		});
	}

	@Test
	void runFailsWhenHealthEndpointGroupIncludesContributorThatDoesNotExist() {
		this.contextRunner.withUserConfiguration(CompositeHealthIndicatorConfiguration.class)
			.withPropertyValues("management.endpoint.health.group.ready.include=composite/b/c,nope")
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).isInstanceOf(NoSuchHealthContributorException.class)
					.hasMessage("Included health contributor 'nope' in group 'ready' does not exist");
			});
	}

	@Test
	void runFailsWhenHealthEndpointGroupExcludesContributorThatDoesNotExist() {
		this.contextRunner
			.withPropertyValues("management.endpoint.health.group.ready.exclude=composite/b/d",
					"management.endpoint.health.group.ready.include=*")
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).isInstanceOf(NoSuchHealthContributorException.class)
					.hasMessage("Excluded health contributor 'composite/b/d' in group 'ready' does not exist");
			});
	}

	@Test
	void runCreatesHealthEndpointGroupThatIncludesContributorThatDoesNotExistWhenValidationIsDisabled() {
		this.contextRunner
			.withPropertyValues("management.endpoint.health.validate-group-membership=false",
					"management.endpoint.health.group.ready.include=nope")
			.run((context) -> {
				HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
				assertThat(groups).isInstanceOf(AutoConfiguredHealthEndpointGroups.class);
				assertThat(groups.getNames()).containsOnly("ready");
			});
	}

	@Test
	void runWhenHasHealthEndpointGroupsBeanDoesNotCreateAdditionalHealthEndpointGroups() {
		this.contextRunner.withUserConfiguration(HealthEndpointGroupsConfiguration.class)
			.withPropertyValues("management.endpoint.health.group.ready.include=*")
			.run((context) -> {
				HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
				assertThat(groups.getNames()).containsOnly("mock");
			});
	}

	@Test
	void runCreatesHealthContributorRegistryContainingHealthBeans() {
		this.contextRunner.run((context) -> {
			HealthContributorRegistry registry = context.getBean(HealthContributorRegistry.class);
			Object[] names = registry.stream().map(NamedContributor::getName).toArray();
			assertThat(names).containsExactlyInAnyOrder("simple", "additional", "ping", "reactive");
		});
	}

	@Test
	void runWhenNoReactorCreatesHealthContributorRegistryContainingHealthBeans() {
		ClassLoader classLoader = new FilteredClassLoader(Mono.class, Flux.class);
		this.contextRunner.withClassLoader(classLoader).run((context) -> {
			HealthContributorRegistry registry = context.getBean(HealthContributorRegistry.class);
			Object[] names = registry.stream().map(NamedContributor::getName).toArray();
			assertThat(names).containsExactlyInAnyOrder("simple", "additional", "ping");
		});
	}

	@Test
	void runWhenHasHealthContributorRegistryBeanDoesNotCreateAdditionalRegistry() {
		this.contextRunner.withUserConfiguration(HealthContributorRegistryConfiguration.class).run((context) -> {
			HealthContributorRegistry registry = context.getBean(HealthContributorRegistry.class);
			Object[] names = registry.stream().map(NamedContributor::getName).toArray();
			assertThat(names).isEmpty();
		});
	}

	@Test
	void runCreatesHealthEndpoint() {
		this.contextRunner.withPropertyValues("management.endpoint.health.show-details=always").run((context) -> {
			HealthEndpoint endpoint = context.getBean(HealthEndpoint.class);
			Health health = (Health) endpoint.healthForPath("simple");
			assertThat(health.getDetails()).containsEntry("counter", 42);
		});
	}

	@Test
	void runWhenHasHealthEndpointBeanDoesNotCreateAdditionalHealthEndpoint() {
		this.contextRunner.withUserConfiguration(HealthEndpointConfiguration.class).run((context) -> {
			HealthEndpoint endpoint = context.getBean(HealthEndpoint.class);
			assertThat(endpoint.health()).isNull();
		});
	}

	@Test
	void runCreatesReactiveHealthContributorRegistryContainingAdaptedBeans() {
		this.reactiveContextRunner.run((context) -> {
			ReactiveHealthContributorRegistry registry = context.getBean(ReactiveHealthContributorRegistry.class);
			Object[] names = registry.stream().map(NamedContributor::getName).toArray();
			assertThat(names).containsExactlyInAnyOrder("simple", "additional", "reactive", "ping");
		});
	}

	@Test
	void runWhenHasReactiveHealthContributorRegistryBeanDoesNotCreateAdditionalReactiveHealthContributorRegistry() {
		this.reactiveContextRunner.withUserConfiguration(ReactiveHealthContributorRegistryConfiguration.class)
			.run((context) -> {
				ReactiveHealthContributorRegistry registry = context.getBean(ReactiveHealthContributorRegistry.class);
				Object[] names = registry.stream().map(NamedContributor::getName).toArray();
				assertThat(names).isEmpty();
			});
	}

	@Test
	void runCreatesHealthEndpointWebExtension() {
		this.contextRunner.run((context) -> {
			HealthEndpointWebExtension webExtension = context.getBean(HealthEndpointWebExtension.class);
			WebEndpointResponse<HealthComponent> response = webExtension.health(ApiVersion.V3,
					WebServerNamespace.SERVER, SecurityContext.NONE, true, "simple");
			Health health = (Health) response.getBody();
			assertThat(response.getStatus()).isEqualTo(200);
			assertThat(health.getDetails()).containsEntry("counter", 42);
		});
	}

	@Test
	void runWhenHasHealthEndpointWebExtensionBeanDoesNotCreateExtraHealthEndpointWebExtension() {
		this.contextRunner.withUserConfiguration(HealthEndpointWebExtensionConfiguration.class).run((context) -> {
			HealthEndpointWebExtension webExtension = context.getBean(HealthEndpointWebExtension.class);
			WebEndpointResponse<HealthComponent> response = webExtension.health(ApiVersion.V3,
					WebServerNamespace.SERVER, SecurityContext.NONE, true, "simple");
			assertThat(response).isNull();
		});
	}

	@Test
	void runCreatesReactiveHealthEndpointWebExtension() {
		this.reactiveContextRunner.run((context) -> {
			ReactiveHealthEndpointWebExtension webExtension = context.getBean(ReactiveHealthEndpointWebExtension.class);
			Mono<WebEndpointResponse<? extends HealthComponent>> response = webExtension.health(ApiVersion.V3,
					WebServerNamespace.SERVER, SecurityContext.NONE, true, "simple");
			Health health = (Health) (response.block().getBody());
			assertThat(health.getDetails()).containsEntry("counter", 42);
		});
	}

	@Test
	void runWhenHasReactiveHealthEndpointWebExtensionBeanDoesNotCreateExtraReactiveHealthEndpointWebExtension() {
		this.reactiveContextRunner.withUserConfiguration(ReactiveHealthEndpointWebExtensionConfiguration.class)
			.run((context) -> {
				ReactiveHealthEndpointWebExtension webExtension = context
					.getBean(ReactiveHealthEndpointWebExtension.class);
				Mono<WebEndpointResponse<? extends HealthComponent>> response = webExtension.health(ApiVersion.V3,
						WebServerNamespace.SERVER, SecurityContext.NONE, true, "simple");
				assertThat(response).isNull();
			});
	}

	@Test
	void runWhenHasHealthEndpointGroupsPostProcessorPerformsProcessing() {
		this.contextRunner.withPropertyValues("management.endpoint.health.group.ready.include=*")
			.withUserConfiguration(HealthEndpointGroupsConfiguration.class, TestHealthEndpointGroupsPostProcessor.class)
			.run((context) -> {
				HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
				assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> groups.get("test"))
					.withMessage("postprocessed");
			});
	}

	@Test
	void runWithIndicatorsInParentContextFindsIndicators() {
		new ApplicationContextRunner().withUserConfiguration(HealthIndicatorsConfiguration.class)
			.run((parent) -> new WebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(HealthContributorAutoConfiguration.class,
						HealthEndpointAutoConfiguration.class))
				.withParent(parent)
				.run((context) -> {
					HealthComponent health = context.getBean(HealthEndpoint.class).health();
					Map<String, HealthComponent> components = ((SystemHealth) health).getComponents();
					assertThat(components).containsKeys("additional", "ping", "simple");
				}));
	}

	@Test
	void runWithReactiveContextAndIndicatorsInParentContextFindsIndicators() {
		new ApplicationContextRunner().withUserConfiguration(HealthIndicatorsConfiguration.class)
			.run((parent) -> new ReactiveWebApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(HealthContributorAutoConfiguration.class,
						HealthEndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
						EndpointAutoConfiguration.class))
				.withParent(parent)
				.run((context) -> {
					HealthComponent health = context.getBean(HealthEndpoint.class).health();
					Map<String, HealthComponent> components = ((SystemHealth) health).getComponents();
					assertThat(components).containsKeys("additional", "ping", "simple");
				}));
	}

	@Test
	@WithTestEndpointOutcomeExposureContributor
	void additionalHealthEndpointsPathsTolerateHealthEndpointThatIsNotWebExposed() {
		this.contextRunner
			.withConfiguration(AutoConfigurations.of(DispatcherServletAutoConfiguration.class,
					EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.exclude=*",
					"management.endpoints.test.exposure.include=*")
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasSingleBean(HealthEndpoint.class);
				assertThat(context).hasSingleBean(HealthEndpointWebExtension.class);
				assertThat(context.getBean(WebEndpointsSupplier.class).getEndpoints()).isEmpty();
				assertThat(context).hasSingleBean(MvcAdditionalHealthEndpointPathsConfiguration.class);
			});
	}

	@Test
	@WithTestEndpointOutcomeExposureContributor
	void additionalJerseyHealthEndpointsPathsTolerateHealthEndpointThatIsNotWebExposed() {
		this.contextRunner
			.withConfiguration(
					AutoConfigurations.of(EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class))
			.withClassLoader(
					new FilteredClassLoader(Thread.currentThread().getContextClassLoader(), DispatcherServlet.class))
			.withPropertyValues("management.endpoints.web.exposure.exclude=*",
					"management.endpoints.test.exposure.include=*")
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasSingleBean(HealthEndpoint.class);
				assertThat(context).hasSingleBean(HealthEndpointWebExtension.class);
				assertThat(context.getBean(WebEndpointsSupplier.class).getEndpoints()).isEmpty();
				assertThat(context).hasSingleBean(JerseyAdditionalHealthEndpointPathsConfiguration.class);
			});
	}

	@Test
	@WithTestEndpointOutcomeExposureContributor
	void additionalReactiveHealthEndpointsPathsTolerateHealthEndpointThatIsNotWebExposed() {
		this.reactiveContextRunner
			.withConfiguration(
					AutoConfigurations.of(EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.exclude=*",
					"management.endpoints.test.exposure.include=*")
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).hasSingleBean(HealthEndpoint.class);
				assertThat(context).hasSingleBean(ReactiveHealthEndpointWebExtension.class);
				assertThat(context.getBean(WebEndpointsSupplier.class).getEndpoints()).isEmpty();
				assertThat(context).hasSingleBean(WebFluxAdditionalHealthEndpointPathsConfiguration.class);
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class HealthIndicatorsConfiguration {

		@Bean
		HealthIndicator simpleHealthIndicator() {
			return () -> Health.up().withDetail("counter", 42).build();
		}

		@Bean
		HealthIndicator additionalHealthIndicator() {
			return () -> Health.up().build();
		}

		@Bean
		ReactiveHealthIndicator reactiveHealthIndicator() {
			return () -> Mono.just(Health.up().build());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CompositeHealthIndicatorConfiguration {

		@Bean
		CompositeHealthContributor compositeHealthIndicator() {
			return CompositeHealthContributor.fromMap(Map.of("a", (HealthIndicator) () -> Health.up().build(), "b",
					CompositeHealthContributor.fromMap(Map.of("c", (HealthIndicator) () -> Health.up().build()))));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class StatusAggregatorConfiguration {

		@Bean
		StatusAggregator statusAggregator() {
			return (statuses) -> Status.UNKNOWN;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HttpCodeStatusMapperConfiguration {

		@Bean
		HttpCodeStatusMapper httpCodeStatusMapper() {
			return (status) -> 456;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HealthEndpointGroupsConfiguration {

		@Bean
		HealthEndpointGroups healthEndpointGroups() {
			HealthEndpointGroups groups = mock(HealthEndpointGroups.class);
			given(groups.getNames()).willReturn(Collections.singleton("mock"));
			return groups;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HealthContributorRegistryConfiguration {

		@Bean
		HealthContributorRegistry healthContributorRegistry() {
			return new DefaultHealthContributorRegistry();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HealthEndpointConfiguration {

		@Bean
		HealthEndpoint healthEndpoint() {
			return mock(HealthEndpoint.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ReactiveHealthContributorRegistryConfiguration {

		@Bean
		ReactiveHealthContributorRegistry reactiveHealthContributorRegistry() {
			return new DefaultReactiveHealthContributorRegistry();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HealthEndpointWebExtensionConfiguration {

		@Bean
		HealthEndpointWebExtension healthEndpointWebExtension() {
			return mock(HealthEndpointWebExtension.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ReactiveHealthEndpointWebExtensionConfiguration {

		@Bean
		ReactiveHealthEndpointWebExtension reactiveHealthEndpointWebExtension() {
			return mock(ReactiveHealthEndpointWebExtension.class);
		}

	}

	static class TestHealthEndpointGroupsPostProcessor implements HealthEndpointGroupsPostProcessor {

		@Override
		public HealthEndpointGroups postProcessHealthEndpointGroups(HealthEndpointGroups groups) {
			given(groups.get("test")).willThrow(new RuntimeException("postprocessed"));
			return groups;
		}

	}

}
