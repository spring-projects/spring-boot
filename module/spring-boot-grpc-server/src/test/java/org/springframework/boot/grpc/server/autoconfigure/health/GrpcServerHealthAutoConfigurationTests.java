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

package org.springframework.boot.grpc.server.autoconfigure.health;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.grpc.BindableService;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.protobuf.services.HealthStatusManager;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.diagnostics.FailureAnalyzedException;
import org.springframework.boot.grpc.server.health.GrpcServerHealth;
import org.springframework.boot.grpc.server.health.HealthCheckedGrpcComponents;
import org.springframework.boot.grpc.server.health.StatusAggregator;
import org.springframework.boot.grpc.server.health.StatusMapper;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorMembershipValidator;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.CompositeReactiveHealthContributor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.ApplicationContextAssertProvider;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.AbstractApplicationContextRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GrpcServerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GrpcServerHealthAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Chris Bono
 * @author Andrey Litvitski
 */
class GrpcServerHealthAutoConfigurationTests {

	private static final AutoConfigurations autoConfigurations = AutoConfigurations.of(
			GrpcServerHealthAutoConfiguration.class, GrpcServerHealthSchedulerAutoConfiguration.class,
			HealthContributorRegistryAutoConfiguration.class, TaskSchedulingAutoConfiguration.class);

	private final BindableService service = mock();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(HealthIndicatorsConfiguration.class)
		.withConfiguration(autoConfigurations)
		.with(this::serviceBean);

	@Test
	void autoConfiguresBeans() {
		this.contextRunner.run(this::assertConfigured);
	}

	@Test
	void whenNoBindableServiceDefinedDoesNotAutoConfigureBeans() {
		new ApplicationContextRunner().withConfiguration(autoConfigurations).run(this::assertNotConfigured);
	}

	@Test
	void whenNoBindableServiceDefinedButHealthEnabledPropertyIsTrueAutoConfiguresBeans() {
		new ApplicationContextRunner().withConfiguration(autoConfigurations)
			.withPropertyValues("spring.grpc.server.health.enabled=true")
			.run(this::assertConfigured);
	}

	@Test
	void whenGrpcNotOnClasspathDoesNotAutoConfigureBeans() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(BindableService.class))
			.run(this::assertNotConfigured);
	}

	@Test
	void whenSpringGrpcNotOnClasspathDoesNotAutoConfigureBeans() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(GrpcServerFactory.class))
			.run(this::assertNotConfigured);
	}

	@Test
	void whenHealthStatusManagerNotOnClasspathDoesNotAutoConfigureBeans() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(HealthStatusManager.class))
			.run(this::assertNotConfigured);
	}

	@Test
	void whenNoTaskSchedulerDoesNotAutoConfigureHealthScheduler() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcServerHealthAutoConfiguration.class,
					HealthContributorRegistryAutoConfiguration.class))
			.with(this::serviceBean)
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerHealthScheduler.class));
	}

	@Test
	void whenHealthScheduleEnabledPropertyFalseDoesNotAutoConfigureHealthScheduler() {
		this.contextRunner.withPropertyValues("spring.grpc.server.health.schedule.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerHealthScheduler.class));
	}

	@Test
	void whenHealthScheduleEnabledPropertyTrueDoesAutoConfigureHealthScheduler() {
		this.contextRunner.withPropertyValues("spring.grpc.server.health.schedule.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerHealthScheduler.class));
	}

	@Test
	void whenHealthEnabledPropertyIsTrueAutoConfiguresBeans() {
		this.contextRunner.withPropertyValues("spring.grpc.server.health.enabled=true").run(this::assertConfigured);
	}

	@Test
	void whenHealthEnabledPropertyIsFalseDoesNotAutoConfigureBeans() {
		this.contextRunner.withPropertyValues("spring.grpc.server.health.enabled=false").run(this::assertNotConfigured);
	}

	@Test
	void whenServerEnabledPropertyIsFalseDoesNotAutoConfigureBeans() {
		this.contextRunner.withPropertyValues("spring.grpc.server.enabled=false").run(this::assertNotConfigured);
	}

	@Test
	void whenServerEnabledPropertyIsTrueAutoConfiguresBeans() {
		this.contextRunner.withPropertyValues("spring.grpc.server.enabled=true").run(this::assertConfigured);
	}

	@Test
	void enterTerminalStateIsCalledOnShutdown() {
		AtomicReference<HealthStatusManager> manager = new AtomicReference<>();
		this.contextRunner.run((context) -> {
			manager.set(context.getBean(HealthStatusManager.class));
			assertTerminalState(manager.get(), false);
		});
		assertTerminalState(manager.get(), true);
	}

	@Test
	void whenHasUserDefinedHealthStatusManagerDoesNotAutoConfigureBean() {
		HealthStatusManager customHealthStatusManager = mock();
		this.contextRunner
			.withBean("customHealthStatusManager", HealthStatusManager.class, () -> customHealthStatusManager)
			.run((context) -> assertThat(context).getBean(HealthStatusManager.class)
				.isSameAs(customHealthStatusManager));
	}

	@Test
	void whenHasNoHealthContributorRegistryOnlyAutoConfiguresBasicService() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcServerHealthAutoConfiguration.class,
					TaskSchedulingAutoConfiguration.class))
			.with(this::serviceBean)
			.run((context) -> {
				assertThat(context).hasSingleBean(HealthStatusManager.class);
				assertThat(context).hasBean("grpcServerHealthService");
				assertThat(context).doesNotHaveBean(GrpcServerHealth.class);
			});
	}

	@Test
	void createsStatusAggregatorFromProperties() {
		this.contextRunner.withPropertyValues("spring.grpc.server.health.status.order=up,down").run((context) -> {
			StatusAggregator aggregator = context.getBean(StatusAggregator.class);
			assertThat(aggregator.getAggregateStatus(Status.UP, Status.DOWN)).isEqualTo(Status.UP);
		});
	}

	@Test
	void whenHasStatusAggregatorBeanIgnoresProperties() {
		this.contextRunner.withUserConfiguration(StatusAggregatorConfiguration.class)
			.withPropertyValues("spring.grpc.server.health.status.order=up,down")
			.run((context) -> {
				StatusAggregator aggregator = context.getBean(StatusAggregator.class);
				assertThat(aggregator.getAggregateStatus(Status.UP, Status.DOWN)).isEqualTo(Status.UNKNOWN);
			});
	}

	@Test
	void createsStatusMapperFromProperties() {
		this.contextRunner.withPropertyValues("spring.grpc.server.health.status.mapping.up=not-serving")
			.run((context) -> {
				StatusMapper mapper = context.getBean(StatusMapper.class);
				assertThat(mapper.getServingStatus(Status.UP)).isEqualTo(ServingStatus.NOT_SERVING);
			});
	}

	@Test
	void whenHasStatusMapperBeanIgnoresProperties() {
		this.contextRunner.withUserConfiguration(StatusMapperConfiguration.class)
			.withPropertyValues("spring.grpc.server.health.status.mapping.up=not-serving")
			.run((context) -> {
				StatusMapper mapper = context.getBean(StatusMapper.class);
				assertThat(mapper.getServingStatus(Status.UP)).isEqualTo(ServingStatus.UNRECOGNIZED);
			});
	}

	@Test
	void createsHealthCheckedGrpcComponents() {
		this.contextRunner.withPropertyValues("spring.grpc.server.health.service.test.include=*").run((context) -> {
			HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
			assertThat(components).isInstanceOf(AutoConfiguredHealthCheckedGrpcComponents.class);
			assertThat(components.getServiceNames()).containsOnly("test");
		});
	}

	@Test
	void whenComponentsIncludesContributorThatExistsDoesNotFail() {
		this.contextRunner.withUserConfiguration(CompositeHealthIndicatorConfiguration.class)
			.withPropertyValues("spring.grpc.server.health.service.test.include=composite/b/c")
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void whenHealthCheckedGrpcComponentsIncludesReactiveContributorThatExists() {
		this.contextRunner.withUserConfiguration(CompositeReactiveHealthIndicatorConfiguration.class)
			.withPropertyValues("spring.grpc.server.health.service.test.include=composite/b/c")
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void whenHealthCheckedGrpcComponentsIncludesContributorThatDoesNotExistThrowsException() {
		this.contextRunner.withUserConfiguration(CompositeHealthIndicatorConfiguration.class)
			.withPropertyValues("spring.grpc.server.health.service.test.include=composite/b/c,nope")
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).isInstanceOf(FailureAnalyzedException.class)
					.hasMessage("Health contributor 'nope' defined in "
							+ "'spring.grpc.server.health.service.test.include' does not exist");
			});
	}

	@Test
	void whenHealthCheckedGrpcComponentsExcludesContributorThatDoesNotExistThrowsException() {
		this.contextRunner
			.withPropertyValues("spring.grpc.server.health.service.test.exclude=composite/b/d",
					"spring.grpc.server.health.service.test.include=*")
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).isInstanceOf(FailureAnalyzedException.class)
					.hasMessage("Health contributor 'composite/b/d' defined in "
							+ "'spring.grpc.server.health.service.test.exclude' does not exist");
			});
	}

	@Test
	void whenHealthCheckedGrpcComponentsIncludesContributorThatDoesNotExistAndValidationDisabledCreatesComponents() {
		this.contextRunner
			.withPropertyValues("spring.grpc.server.health.services.validate-membership=false",
					"spring.grpc.server.health.service.test.include=nope")
			.run((context) -> {
				HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
				assertThat(components).isInstanceOf(AutoConfiguredHealthCheckedGrpcComponents.class);
				assertThat(components.getServiceNames()).containsOnly("test");
			});
	}

	@Test
	void whenHasHealthCheckedGrpcComponentsBeanDoesNotCreateAdditional() {
		this.contextRunner.withUserConfiguration(HealthCheckedGrpcComponentsConfiguration.class)
			.withPropertyValues("spring.grpc.server.health.service.test.include=*")
			.run((context) -> {
				HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
				assertThat(components.getServiceNames()).containsOnly("mock");
			});
	}

	@Test
	void createsGrpcServerHealth() {
		this.contextRunner.run((context) -> {
			GrpcServerHealth serverHealth = context.getBean(GrpcServerHealth.class);
			Map<String, ServingStatus> result = new LinkedHashMap<>();
			serverHealth.update(result::put);
			assertThat(result).containsExactly(entry("", ServingStatus.SERVING));
		});
	}

	@Test
	void whenHasGrpcServerHealthBeanDoesNotCreateAdditional() {
		this.contextRunner.withUserConfiguration(GrpcServerHealthConfiguration.class).run((context) -> {
			GrpcServerHealth serverHealth = context.getBean(GrpcServerHealth.class);
			Map<String, ServingStatus> result = new LinkedHashMap<>();
			serverHealth.update(result::put);
			assertThat(result).isEmpty();
		});
	}

	@Test
	void runWithIndicatorsInParentContextFindsIndicators() {
		new ApplicationContextRunner().withUserConfiguration(DownHealthIndicatorConfiguration.class)
			.run((parent) -> new ApplicationContextRunner().withConfiguration(autoConfigurations)
				.withUserConfiguration(HealthIndicatorsConfiguration.class)
				.with(this::serviceBean)
				.withParent(parent)
				.run((context) -> {
					GrpcServerHealth serverHealth = context.getBean(GrpcServerHealth.class);
					Map<String, ServingStatus> result = new LinkedHashMap<>();
					serverHealth.update(result::put);
					assertThat(result).containsExactly(entry("", ServingStatus.NOT_SERVING));
				}));
	}

	private void assertTerminalState(HealthStatusManager healthStatusManager, boolean expected) {
		assertThat(healthStatusManager).extracting("healthService.terminal").isEqualTo(expected);
	}

	private void assertConfigured(AssertableApplicationContext context) {
		assertThat(context).hasSingleBean(HealthStatusManager.class);
		assertThat(context).hasBean("grpcServerHealthService");
		assertThat(context).hasSingleBean(StatusAggregator.class);
		assertThat(context).hasSingleBean(StatusMapper.class);
		assertThat(context).hasSingleBean(HealthCheckedGrpcComponents.class);
		assertThat(context).hasSingleBean(GrpcServerHealth.class);
		assertThat(context).hasSingleBean(HealthContributorMembershipValidator.class);
		assertThat(context).hasSingleBean(GrpcServerHealthScheduler.class);
	}

	private void assertNotConfigured(AssertableApplicationContext context) {
		assertThat(context).doesNotHaveBean(HealthStatusManager.class);
		assertThat(context).doesNotHaveBean("grpcServerHealthService");
		assertThat(context).doesNotHaveBean(GrpcServerHealthAutoConfiguration.class);
	}

	private <R extends AbstractApplicationContextRunner<R, C, A>, C extends ConfigurableApplicationContext, A extends ApplicationContextAssertProvider<C>> R serviceBean(
			R contextRunner) {
		return contextRunner.withBean(BindableService.class, () -> this.service);
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
	static class DownHealthIndicatorConfiguration {

		@Bean
		HealthIndicator downHealthIndicator() {
			return () -> Health.down().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CompositeHealthIndicatorConfiguration {

		@Bean
		CompositeHealthContributor compositeHealthIndicator() {
			return CompositeHealthContributor.fromMap(Map.of("a", createHealthIndicator(), "b",
					CompositeHealthContributor.fromMap(Map.of("c", createHealthIndicator()))));
		}

		private HealthIndicator createHealthIndicator() {
			return () -> Health.up().build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CompositeReactiveHealthIndicatorConfiguration {

		@Bean
		CompositeReactiveHealthContributor compositeHealthIndicator() {
			return CompositeReactiveHealthContributor.fromMap(Map.of("a", createHealthIndicator(), "b",
					CompositeReactiveHealthContributor.fromMap(Map.of("c", createHealthIndicator()))));
		}

		private ReactiveHealthIndicator createHealthIndicator() {
			return () -> Mono.just(Health.up().build());
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
	static class StatusMapperConfiguration {

		@Bean
		StatusMapper statusMapper() {
			return (status) -> ServingStatus.UNRECOGNIZED;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HealthCheckedGrpcComponentsConfiguration {

		@Bean
		HealthCheckedGrpcComponents healthCheckedGrpcComponents() {
			HealthCheckedGrpcComponents components = mock();
			given(components.getServiceNames()).willReturn(Collections.singleton("mock"));
			return components;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GrpcServerHealthConfiguration {

		@Bean
		GrpcServerHealth grpcServerHealth() {
			return mock();
		}

	}

}
