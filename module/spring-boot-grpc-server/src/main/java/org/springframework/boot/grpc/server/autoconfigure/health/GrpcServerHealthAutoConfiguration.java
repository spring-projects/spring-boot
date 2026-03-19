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

import io.grpc.BindableService;
import io.grpc.Grpc;
import io.grpc.protobuf.services.HealthStatusManager;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.grpc.server.autoconfigure.health.GrpcServerHealthAutoConfiguration.NotDisabledAndHasBindableServiceOrExplicitlyEnabledCondition;
import org.springframework.boot.grpc.server.health.GrpcServerHealth;
import org.springframework.boot.grpc.server.health.HealthCheckedGrpcComponents;
import org.springframework.boot.grpc.server.health.StatusAggregator;
import org.springframework.boot.grpc.server.health.StatusMapper;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorMembershipValidator;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GrpcServerFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC server-side health service.
 *
 * @author Daniel Theuke
 * @author Chris Bono
 * @author Phillip Webb
 * @since 4.1.0
 */
@AutoConfiguration(
		afterName = "org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration")
@ConditionalOnClass({ GrpcServerFactory.class, Grpc.class, HealthStatusManager.class })
@ConditionalOnBooleanProperty(name = "spring.grpc.server.enabled", matchIfMissing = true)
@Conditional(NotDisabledAndHasBindableServiceOrExplicitlyEnabledCondition.class)
@EnableConfigurationProperties(GrpcServerHealthProperties.class)
public final class GrpcServerHealthAutoConfiguration {

	@Bean(destroyMethod = "enterTerminalState")
	@ConditionalOnMissingBean
	HealthStatusManager grpcServerHealthStatusManager() {
		return new HealthStatusManager();
	}

	@Bean
	BindableService grpcServerHealthService(HealthStatusManager healthStatusManager) {
		return healthStatusManager.getHealthService();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(type = "org.springframework.boot.health.registry.HealthContributorRegistry")
	static class GrpcServerHealthContributorConfiguration {

		static final String VALIDATE_MEMBERSHIP_PROPERTY = "spring.grpc.server.health.services.validate-membership";

		@Bean
		@ConditionalOnMissingBean
		StatusAggregator grpcServerHealthStatusAggregator(GrpcServerHealthProperties properties) {
			return StatusAggregator.of(properties.getStatus().getOrder());
		}

		@Bean
		@ConditionalOnMissingBean
		StatusMapper grpcServerHealthHttpCodeStatusMapper(GrpcServerHealthProperties properties) {
			return StatusMapper.of(properties.getStatus().getMapping());
		}

		@Bean
		@ConditionalOnMissingBean(HealthCheckedGrpcComponents.class)
		AutoConfiguredHealthCheckedGrpcComponents grpcServerHealthCheckedGrpcComponents(
				ApplicationContext applicationContext, GrpcServerHealthProperties properties) {
			return new AutoConfiguredHealthCheckedGrpcComponents(applicationContext, properties);
		}

		@Bean
		@ConditionalOnMissingBean
		GrpcServerHealth grpcServerHealth(HealthContributorRegistry healthContributorRegistry,
				ObjectProvider<ReactiveHealthContributorRegistry> reactiveHealthContributorRegistry,
				HealthCheckedGrpcComponents healthCheckedGrpcComponents) {
			return new GrpcServerHealth(healthContributorRegistry, reactiveHealthContributorRegistry.getIfAvailable(),
					healthCheckedGrpcComponents);
		}

		@Bean
		@ConditionalOnBooleanProperty(name = VALIDATE_MEMBERSHIP_PROPERTY, matchIfMissing = true)
		HealthContributorMembershipValidator grpcServerHealthServiceMembershipValidator(
				GrpcServerHealthProperties properties, HealthContributorRegistry healthContributorRegistry,
				ObjectProvider<ReactiveHealthContributorRegistry> reactiveHealthContributorRegistry) {
			return new HealthContributorMembershipValidator(healthContributorRegistry,
					reactiveHealthContributorRegistry.getIfAvailable(), VALIDATE_MEMBERSHIP_PROPERTY,
					(members) -> properties.getService().forEach((serviceName, service) -> {
						String property = "spring.grpc.server.health.service." + serviceName;
						members.member(property + ".include".formatted(serviceName), service.getInclude());
						members.member(property + ".exclude".formatted(serviceName), service.getExclude());
					}));
		}

	}

	static class NotDisabledAndHasBindableServiceOrExplicitlyEnabledCondition extends AnyNestedCondition {

		NotDisabledAndHasBindableServiceOrExplicitlyEnabledCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(BindableService.class)
		@ConditionalOnBooleanProperty(name = "spring.grpc.server.health.enabled", matchIfMissing = true)
		static class NotDisabledAndHasBindableService {

		}

		@ConditionalOnBooleanProperty(name = "spring.grpc.server.health.enabled")
		static class ExplicitlyEnabled {

		}

	}

}
