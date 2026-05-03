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

import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.grpc.server.health.HealthCheckedGrpcComponent;
import org.springframework.boot.grpc.server.health.HealthCheckedGrpcComponents;
import org.springframework.boot.grpc.server.health.StatusAggregator;
import org.springframework.boot.grpc.server.health.StatusMapper;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfiguredHealthCheckedGrpcComponent}.
 *
 * @author Phillip Webb
 */
class AutoConfiguredHealthCheckedGrpcComponentsTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(AutoConfiguredHealthCheckedGrpcComponentsTestConfiguration.class));

	@Test
	void getServerMatchesAllMembers() {
		this.contextRunner.run((context) -> {
			HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
			HealthCheckedGrpcComponent server = components.getServer();
			assertThat(server).isNotNull();
			assertThat(server.isMember("a")).isTrue();
			assertThat(server.isMember("b")).isTrue();
			assertThat(server.isMember("C")).isTrue();
		});
	}

	@Test
	void getServiceNamesReturnsServiceNames() {
		this.contextRunner
			.withPropertyValues("spring.grpc.server.health.service.a.include=*",
					"spring.grpc.server.health.service.b.include=*")
			.run((context) -> {
				HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
				assertThat(components.getServiceNames()).containsExactlyInAnyOrder("a", "b");
			});
	}

	@Test
	void getServiceWhenServiceExistsReturnsService() {
		this.contextRunner.withPropertyValues("spring.grpc.server.health.service.a.include=*").run((context) -> {
			HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
			HealthCheckedGrpcComponent component = components.getService("a");
			assertThat(component).isNotNull();
		});
	}

	@Test
	void getServiceWhenServiceDoesNotExistReturnsNull() {
		this.contextRunner.withPropertyValues("spring.grpc.server.health.service.a.include=*").run((context) -> {
			HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
			HealthCheckedGrpcComponent component = components.getService("b");
			assertThat(component).isNull();
		});
	}

	@Test
	void createWhenNoDefinedBeansAdaptsProperties() {
		this.contextRunner
			.withPropertyValues("spring.grpc.server.health.status.order=up,down",
					"spring.grpc.server.health.status.mapping.down=serving")
			.run((context) -> {
				HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
				HealthCheckedGrpcComponent server = components.getServer();
				assertThat(server).isNotNull();
				assertThat(server.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN))
					.isEqualTo(Status.UP);
				assertThat(server.getStatusMapper().getServingStatus(Status.DOWN)).isEqualTo(ServingStatus.SERVING);
			});
	}

	@Test
	void createWhenHasStatusAggregatorBeanReturnsInstanceWithAggregatorUsedForAllServices() {
		this.contextRunner.withUserConfiguration(CustomStatusAggregatorConfiguration.class)
			.withPropertyValues("spring.grpc.server.health.status.order=up,down",
					"spring.grpc.server.health.service.a.include=*")
			.run((context) -> {
				HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
				HealthCheckedGrpcComponent server = components.getServer();
				HealthCheckedGrpcComponent serviceA = components.getService("a");
				assertThat(server).isNotNull();
				assertThat(serviceA).isNotNull();
				assertThat(server.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
					.isEqualTo(Status.UNKNOWN);
				assertThat(serviceA).isNotNull();
				assertThat(serviceA.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
					.isEqualTo(Status.UNKNOWN);
			});
	}

	@Test
	void createWhenHasStatusAggregatorBeanAndServiceSpecificPropertyReturnsInstanceThatUsesBeanOnlyForUnconfiguredServices() {
		this.contextRunner.withUserConfiguration(CustomStatusAggregatorConfiguration.class)
			.withPropertyValues("spring.grpc.server.health.service.a.include=*",
					"spring.grpc.server.health.service.a.status.order=up,down",
					"spring.grpc.server.health.service.b.include=*")
			.run((context) -> {
				HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
				HealthCheckedGrpcComponent server = components.getServer();
				HealthCheckedGrpcComponent serviceA = components.getService("a");
				HealthCheckedGrpcComponent serviceB = components.getService("b");
				assertThat(server).isNotNull();
				assertThat(serviceA).isNotNull();
				assertThat(serviceB).isNotNull();
				assertThat(server.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
					.isEqualTo(Status.UNKNOWN);
				assertThat(serviceA).isNotNull();
				assertThat(serviceA.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
					.isEqualTo(Status.UP);
				assertThat(serviceB).isNotNull();
				assertThat(serviceB.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
					.isEqualTo(Status.UNKNOWN);
			});
	}

	@Test
	void createWhenHasStatusAggregatorPropertyReturnsInstanceWithPropertyUsedForAllServices() {
		this.contextRunner
			.withPropertyValues("spring.grpc.server.health.status.order=up,down",
					"spring.grpc.server.health.service.a.include=*")
			.run((context) -> {
				HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
				HealthCheckedGrpcComponent server = components.getServer();
				HealthCheckedGrpcComponent serviceA = components.getService("a");
				assertThat(server).isNotNull();
				assertThat(serviceA).isNotNull();
				assertThat(server.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN))
					.isEqualTo(Status.UP);
				assertThat(serviceA).isNotNull();
				assertThat(serviceA.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN))
					.isEqualTo(Status.UP);
			});
	}

	@Test
	void createWhenHasStatusAggregatorPropertyAndServiceSpecificPropertyReturnsInstanceWithPropertyUsedForExpectedServices() {
		this.contextRunner
			.withPropertyValues("spring.grpc.server.health.status.order=up,down",
					"spring.grpc.server.health.service.a.include=*",
					"spring.grpc.server.health.service.a.status.order=unknown,up,down",
					"spring.grpc.server.health.service.b.include=*")
			.run((context) -> {
				HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
				HealthCheckedGrpcComponent server = components.getServer();
				HealthCheckedGrpcComponent serviceA = components.getService("a");
				HealthCheckedGrpcComponent serviceB = components.getService("b");
				assertThat(server).isNotNull();
				assertThat(serviceA).isNotNull();
				assertThat(serviceB).isNotNull();
				assertThat(server.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
					.isEqualTo(Status.UP);
				assertThat(serviceA).isNotNull();
				assertThat(serviceA.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
					.isEqualTo(Status.UNKNOWN);
				assertThat(serviceB).isNotNull();
				assertThat(serviceB.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
					.isEqualTo(Status.UP);
			});
	}

	@Test
	void createWhenHasStatusAggregatorPropertyAndServiceQualifiedBeanReturnsInstanceWithBeanUsedForExpectedServices() {
		this.contextRunner.withUserConfiguration(CustomStatusAggregatorServiceAConfiguration.class)
			.withPropertyValues("spring.grpc.server.health.status.order=up,down",
					"spring.grpc.server.health.service.a.include=*",
					"spring.grpc.server.health.service.a.status.order=up,down",
					"spring.grpc.server.health.service.b.include=*")
			.run((context) -> {
				HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
				HealthCheckedGrpcComponent server = components.getServer();
				HealthCheckedGrpcComponent serviceA = components.getService("a");
				HealthCheckedGrpcComponent serviceB = components.getService("b");
				assertThat(server).isNotNull();
				assertThat(serviceA).isNotNull();
				assertThat(serviceB).isNotNull();
				assertThat(server.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
					.isEqualTo(Status.UP);
				assertThat(serviceA).isNotNull();
				assertThat(serviceA.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
					.isEqualTo(Status.UNKNOWN);
				assertThat(serviceB).isNotNull();
				assertThat(serviceB.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
					.isEqualTo(Status.UP);
			});
	}

	@Test
	void createWhenHasServiceSpecificStatusAggregatorPropertyAndServiceQualifiedBeanReturnsInstanceWithBeanUsedForExpectedServices() {
		this.contextRunner.withUserConfiguration(CustomStatusAggregatorServiceAConfiguration.class)
			.withPropertyValues("spring.grpc.server.health.service.a.include=*",
					"spring.grpc.server.health.service.a.status.order=up,down",
					"spring.grpc.server.health.service.b.include=*",
					"spring.grpc.server.health.service.b.status.order=up,down")
			.run((context) -> {
				HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
				HealthCheckedGrpcComponent server = components.getServer();
				HealthCheckedGrpcComponent serviceA = components.getService("a");
				HealthCheckedGrpcComponent serviceB = components.getService("b");
				assertThat(server).isNotNull();
				assertThat(serviceA).isNotNull();
				assertThat(serviceB).isNotNull();
				assertThat(server.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
					.isEqualTo(Status.DOWN);
				assertThat(serviceA).isNotNull();
				assertThat(serviceA.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
					.isEqualTo(Status.UNKNOWN);
				assertThat(serviceB).isNotNull();
				assertThat(serviceB.getStatusAggregator().getAggregateStatus(Status.UP, Status.DOWN, Status.UNKNOWN))
					.isEqualTo(Status.UP);
			});
	}

	@Test
	void createWhenHasStatusMapperBeanReturnsInstanceWithMapperUsedForAllServices() {
		this.contextRunner.withUserConfiguration(CustomStatusMapperConfiguration.class)
			.withPropertyValues("spring.grpc.server.health.status.mapping.down=service-unknown",
					"spring.grpc.server.health.service.a.include=*")
			.run((context) -> {
				HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
				HealthCheckedGrpcComponent server = components.getServer();
				HealthCheckedGrpcComponent serviceA = components.getService("a");
				assertThat(server).isNotNull();
				assertThat(serviceA).isNotNull();
				assertThat(server.getStatusMapper().getServingStatus(Status.DOWN)).isEqualTo(ServingStatus.SERVING);
				assertThat(serviceA).isNotNull();
				assertThat(serviceA.getStatusMapper().getServingStatus(Status.DOWN)).isEqualTo(ServingStatus.SERVING);
			});
	}

	@Test
	void createWhenHasStatusMapperBeanAndServiceSpecificPropertyReturnsInstanceThatUsesBeanOnlyForUnconfiguredServices() {
		this.contextRunner.withUserConfiguration(CustomStatusMapperConfiguration.class)
			.withPropertyValues("spring.grpc.server.health.service.a.include=*",
					"spring.grpc.server.health.service.a.status.mapping.down=service-unknown",
					"spring.grpc.server.health.service.b.include=*")
			.run((context) -> {
				HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
				HealthCheckedGrpcComponent server = components.getServer();
				HealthCheckedGrpcComponent serviceA = components.getService("a");
				HealthCheckedGrpcComponent serviceB = components.getService("b");
				assertThat(server).isNotNull();
				assertThat(serviceA).isNotNull();
				assertThat(serviceB).isNotNull();
				assertThat(server.getStatusMapper().getServingStatus(Status.DOWN)).isEqualTo(ServingStatus.SERVING);
				assertThat(serviceA).isNotNull();
				assertThat(serviceA.getStatusMapper().getServingStatus(Status.DOWN))
					.isEqualTo(ServingStatus.SERVICE_UNKNOWN);
				assertThat(serviceB).isNotNull();
				assertThat(serviceB.getStatusMapper().getServingStatus(Status.DOWN)).isEqualTo(ServingStatus.SERVING);
			});
	}

	@Test
	void createWhenHasStatusMapperPropertyReturnsInstanceWithPropertyUsedForAllServices() {
		this.contextRunner
			.withPropertyValues("spring.grpc.server.health.status.mapping.down=service-unknown",
					"spring.grpc.server.health.service.a.include=*")
			.run((context) -> {
				HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
				HealthCheckedGrpcComponent server = components.getServer();
				HealthCheckedGrpcComponent serviceA = components.getService("a");
				assertThat(server).isNotNull();
				assertThat(serviceA).isNotNull();
				assertThat(server.getStatusMapper().getServingStatus(Status.DOWN))
					.isEqualTo(ServingStatus.SERVICE_UNKNOWN);
				assertThat(serviceA).isNotNull();
				assertThat(serviceA.getStatusMapper().getServingStatus(Status.DOWN))
					.isEqualTo(ServingStatus.SERVICE_UNKNOWN);
			});
	}

	@Test
	void createWhenHasStatusMapperPropertyAndServiceSpecificPropertyReturnsInstanceWithPropertyUsedForExpectedServices() {
		this.contextRunner
			.withPropertyValues("spring.grpc.server.health.status.mapping.down=service-unknown",
					"spring.grpc.server.health.service.a.include=*",
					"spring.grpc.server.health.service.a.status.mapping.down=unrecognized",
					"spring.grpc.server.health.service.b.include=*")
			.run((context) -> {
				HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
				HealthCheckedGrpcComponent server = components.getServer();
				HealthCheckedGrpcComponent serviceA = components.getService("a");
				HealthCheckedGrpcComponent serviceB = components.getService("b");
				assertThat(server).isNotNull();
				assertThat(serviceA).isNotNull();
				assertThat(serviceB).isNotNull();
				assertThat(server.getStatusMapper().getServingStatus(Status.DOWN))
					.isEqualTo(ServingStatus.SERVICE_UNKNOWN);
				assertThat(serviceA).isNotNull();
				assertThat(serviceA.getStatusMapper().getServingStatus(Status.DOWN))
					.isEqualTo(ServingStatus.UNRECOGNIZED);
				assertThat(serviceB).isNotNull();
				assertThat(serviceB.getStatusMapper().getServingStatus(Status.DOWN))
					.isEqualTo(ServingStatus.SERVICE_UNKNOWN);
			});
	}

	@Test
	void createWhenHasStatusMapperPropertyAndServiceQualifiedBeanReturnsInstanceWithBeanUsedForExpectedServices() {
		this.contextRunner.withUserConfiguration(CustomStatusMapperServiceAConfiguration.class)
			.withPropertyValues("spring.grpc.server.health.status.mapping.down=service-unknown",
					"spring.grpc.server.health.service.a.include=*",
					"spring.grpc.server.health.service.a.status.mapping.down=service-unknown",
					"spring.grpc.server.health.service.b.include=*")
			.run((context) -> {
				HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
				HealthCheckedGrpcComponent server = components.getServer();
				HealthCheckedGrpcComponent serviceA = components.getService("a");
				HealthCheckedGrpcComponent serviceB = components.getService("b");
				assertThat(server).isNotNull();
				assertThat(serviceA).isNotNull();
				assertThat(serviceB).isNotNull();
				assertThat(server.getStatusMapper().getServingStatus(Status.DOWN))
					.isEqualTo(ServingStatus.SERVICE_UNKNOWN);
				assertThat(serviceA).isNotNull();
				assertThat(serviceA.getStatusMapper().getServingStatus(Status.DOWN)).isEqualTo(ServingStatus.SERVING);
				assertThat(serviceB).isNotNull();
				assertThat(serviceB.getStatusMapper().getServingStatus(Status.DOWN))
					.isEqualTo(ServingStatus.SERVICE_UNKNOWN);
			});
	}

	@Test
	void createWhenHasServiceSpecificStatusMapperPropertyAndServiceQualifiedBeanReturnsInstanceWithBeanUsedForExpectedServices() {
		this.contextRunner.withUserConfiguration(CustomStatusMapperServiceAConfiguration.class)
			.withPropertyValues("spring.grpc.server.health.service.a.include=*",
					"spring.grpc.server.health.service.a.status.mapping.down=service-unknown",
					"spring.grpc.server.health.service.b.include=*",
					"spring.grpc.server.health.service.b.status.mapping.down=service-unknown")
			.run((context) -> {
				HealthCheckedGrpcComponents components = context.getBean(HealthCheckedGrpcComponents.class);
				HealthCheckedGrpcComponent server = components.getServer();
				HealthCheckedGrpcComponent serviceA = components.getService("a");
				HealthCheckedGrpcComponent serviceB = components.getService("b");
				assertThat(server).isNotNull();
				assertThat(serviceA).isNotNull();
				assertThat(serviceB).isNotNull();
				assertThat(server.getStatusMapper().getServingStatus(Status.DOWN)).isEqualTo(ServingStatus.NOT_SERVING);
				assertThat(serviceA).isNotNull();
				assertThat(serviceA.getStatusMapper().getServingStatus(Status.DOWN)).isEqualTo(ServingStatus.SERVING);
				assertThat(serviceB).isNotNull();
				assertThat(serviceB.getStatusMapper().getServingStatus(Status.DOWN))
					.isEqualTo(ServingStatus.SERVICE_UNKNOWN);
			});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(GrpcServerHealthProperties.class)
	static class AutoConfiguredHealthCheckedGrpcComponentsTestConfiguration {

		@Bean
		AutoConfiguredHealthCheckedGrpcComponents healthCheckedGrpcComponents(
				ConfigurableApplicationContext applicationContext, GrpcServerHealthProperties properties) {
			return new AutoConfiguredHealthCheckedGrpcComponents(applicationContext, properties);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomStatusAggregatorConfiguration {

		@Bean
		@Primary
		StatusAggregator statusAggregator() {
			return StatusAggregator.of(Status.UNKNOWN, Status.UP, Status.DOWN);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomStatusAggregatorServiceAConfiguration {

		@Bean
		@Qualifier("a")
		StatusAggregator statusAggregator() {
			return StatusAggregator.of(Status.UNKNOWN, Status.UP, Status.DOWN);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomStatusMapperConfiguration {

		@Bean
		@Primary
		StatusMapper statusMapper() {
			return StatusMapper.of(Collections.singletonMap(Status.DOWN.getCode(), ServingStatus.SERVING));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomStatusMapperServiceAConfiguration {

		@Bean
		@Qualifier("a")
		StatusMapper statusMapper() {
			return StatusMapper.of(Collections.singletonMap(Status.DOWN.getCode(), ServingStatus.SERVING));
		}

	}

}
