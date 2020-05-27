/*
 * Copyright 2012-2020 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.SecurityContext;
import org.springframework.boot.actuate.endpoint.http.ApiVersion;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.health.AbstractHealthAggregator;
import org.springframework.boot.actuate.health.DefaultHealthContributorRegistry;
import org.springframework.boot.actuate.health.DefaultReactiveHealthContributorRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
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
import org.springframework.boot.actuate.health.ReactiveHealthIndicatorRegistry;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
@SuppressWarnings("deprecation")
class HealthEndpointAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withUserConfiguration(HealthIndicatorsConfiguration.class).withConfiguration(AutoConfigurations
					.of(HealthContributorAutoConfiguration.class, HealthEndpointAutoConfiguration.class));

	private ReactiveWebApplicationContextRunner reactiveContextRunner = new ReactiveWebApplicationContextRunner()
			.withUserConfiguration(HealthIndicatorsConfiguration.class).withConfiguration(AutoConfigurations
					.of(HealthContributorAutoConfiguration.class, HealthEndpointAutoConfiguration.class));

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
	void runWhenHasHealthAggregatorAdaptsToStatusAggregator() {
		this.contextRunner.withUserConfiguration(HealthAggregatorConfiguration.class).run((context) -> {
			StatusAggregator aggregator = context.getBean(StatusAggregator.class);
			assertThat(aggregator.getAggregateStatus(Status.UP, Status.DOWN)).isEqualTo(Status.UNKNOWN);
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
	void runWhenUsingDeprecatedPropertyCreatesStatusAggregatorFromProperties() {
		this.contextRunner.withPropertyValues("management.health.status.order=up,down").run((context) -> {
			StatusAggregator aggregator = context.getBean(StatusAggregator.class);
			assertThat(aggregator.getAggregateStatus(Status.UP, Status.DOWN)).isEqualTo(Status.UP);
		});
	}

	@Test
	void runWhenHasStatusAggregatorBeanIgnoresProperties() {
		this.contextRunner.withUserConfiguration(StatusAggregatorConfiguration.class)
				.withPropertyValues("management.health.status.order=up,down").run((context) -> {
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
	void runUsingDeprecatedPropertyCreatesHttpCodeStatusMapperFromProperties() {
		this.contextRunner.withPropertyValues("management.health.status.http-mapping.up=123").run((context) -> {
			HttpCodeStatusMapper mapper = context.getBean(HttpCodeStatusMapper.class);
			assertThat(mapper.getStatusCode(Status.UP)).isEqualTo(123);
		});
	}

	@Test
	void runWhenHasHttpCodeStatusMapperBeanIgnoresProperties() {
		this.contextRunner.withUserConfiguration(HttpCodeStatusMapperConfiguration.class)
				.withPropertyValues("management.health.status.http-mapping.up=123").run((context) -> {
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
	void runWhenHasHealthEndpointGroupsBeanDoesNotCreateAdditionalHealthEndpointGroups() {
		this.contextRunner.withUserConfiguration(HealthEndpointGroupsConfiguration.class)
				.withPropertyValues("management.endpoint.health.group.ready.include=*").run((context) -> {
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
					ReactiveHealthContributorRegistry registry = context
							.getBean(ReactiveHealthContributorRegistry.class);
					Object[] names = registry.stream().map(NamedContributor::getName).toArray();
					assertThat(names).isEmpty();
				});
	}

	@Test
	void runCreatesHealthEndpointWebExtension() {
		this.contextRunner.run((context) -> {
			HealthEndpointWebExtension webExtension = context.getBean(HealthEndpointWebExtension.class);
			WebEndpointResponse<HealthComponent> response = webExtension.health(ApiVersion.V3, SecurityContext.NONE,
					true, "simple");
			Health health = (Health) response.getBody();
			assertThat(response.getStatus()).isEqualTo(200);
			assertThat(health.getDetails()).containsEntry("counter", 42);
		});
	}

	@Test
	void runWhenHasHealthEndpointWebExtensionBeanDoesNotCreateExtraHealthEndpointWebExtension() {
		this.contextRunner.withUserConfiguration(HealthEndpointWebExtensionConfiguration.class).run((context) -> {
			HealthEndpointWebExtension webExtension = context.getBean(HealthEndpointWebExtension.class);
			WebEndpointResponse<HealthComponent> response = webExtension.health(ApiVersion.V3, SecurityContext.NONE,
					true, "simple");
			assertThat(response).isNull();
		});
	}

	@Test
	void runCreatesReactiveHealthEndpointWebExtension() {
		this.reactiveContextRunner.run((context) -> {
			ReactiveHealthEndpointWebExtension webExtension = context.getBean(ReactiveHealthEndpointWebExtension.class);
			Mono<WebEndpointResponse<? extends HealthComponent>> response = webExtension.health(ApiVersion.V3,
					SecurityContext.NONE, true, "simple");
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
							SecurityContext.NONE, true, "simple");
					assertThat(response).isNull();
				});
	}

	@Test // gh-18354
	void runCreatesLegacyHealthAggregator() {
		this.contextRunner.run((context) -> {
			HealthAggregator aggregator = context.getBean(HealthAggregator.class);
			Map<String, Health> healths = new LinkedHashMap<>();
			healths.put("one", Health.up().build());
			healths.put("two", Health.down().build());
			Health result = aggregator.aggregate(healths);
			assertThat(result.getStatus()).isEqualTo(Status.DOWN);
		});
	}

	@Test
	void runWhenReactorAvailableCreatesReactiveHealthIndicatorRegistryBean() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(ReactiveHealthIndicatorRegistry.class));
	}

	@Test // gh-18570
	void runWhenReactorUnavailableDoesNotCreateReactiveHealthIndicatorRegistryBean() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(Mono.class.getPackage().getName()))
				.run((context) -> assertThat(context).doesNotHaveBean(ReactiveHealthIndicatorRegistry.class));
	}

	@Test
	void runWhenHasHealthEndpointGroupsPostProcessorPerformsProcessing() {
		this.contextRunner.withPropertyValues("management.endpoint.health.group.ready.include=*").withUserConfiguration(
				HealthEndpointGroupsConfiguration.class, TestHealthEndpointGroupsPostProcessor.class).run((context) -> {
					HealthEndpointGroups groups = context.getBean(HealthEndpointGroups.class);
					assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> groups.get("test"))
							.withMessage("postprocessed");
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
	static class HealthAggregatorConfiguration {

		@Bean
		HealthAggregator healthAggregator() {
			return new AbstractHealthAggregator() {

				@Override
				protected Status aggregateStatus(List<Status> candidates) {
					return Status.UNKNOWN;
				}

			};
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
