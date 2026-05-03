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

package org.springframework.boot.health.autoconfigure.actuate.endpoint;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroups;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroupsPostProcessor;
import org.springframework.boot.health.actuate.endpoint.HttpCodeStatusMapper;
import org.springframework.boot.health.actuate.endpoint.StatusAggregator;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorMembershipValidator;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for {@link HealthEndpoint} infrastructure beans.
 *
 * @author Phillip Webb
 * @see HealthEndpointAutoConfiguration
 */
@Configuration(proxyBeanMethods = false)
class HealthEndpointConfiguration {

	private static final String VALIDATE_MEMBERSHIP_PROPERTY = "management.endpoint.health.validate-group-membership";

	@Bean
	@ConditionalOnMissingBean
	StatusAggregator healthStatusAggregator(HealthEndpointProperties properties) {
		return StatusAggregator.of(properties.getStatus().getOrder());
	}

	@Bean
	@ConditionalOnMissingBean
	HttpCodeStatusMapper healthHttpCodeStatusMapper(HealthEndpointProperties properties) {
		return HttpCodeStatusMapper.of(properties.getStatus().getHttpMapping());
	}

	@Bean
	@ConditionalOnMissingBean(HealthEndpointGroups.class)
	AutoConfiguredHealthEndpointGroups healthEndpointGroups(ApplicationContext applicationContext,
			HealthEndpointProperties properties) {
		return new AutoConfiguredHealthEndpointGroups(applicationContext, properties);
	}

	@Bean
	GroupsHealthContributorNameValidator groupsHealthContributorNameValidator(
			ObjectProvider<HealthEndpointGroups> healthEndpointGroups) {
		return new GroupsHealthContributorNameValidator(healthEndpointGroups.getIfAvailable());
	}

	@Bean
	@ConditionalOnBooleanProperty(name = VALIDATE_MEMBERSHIP_PROPERTY, matchIfMissing = true)
	HealthContributorMembershipValidator healthEndpointGroupMembershipValidator(HealthEndpointProperties properties,
			HealthContributorRegistry healthContributorRegistry,
			ObjectProvider<ReactiveHealthContributorRegistry> reactiveHealthContributorRegistry) {
		return new HealthContributorMembershipValidator(healthContributorRegistry,
				reactiveHealthContributorRegistry.getIfAvailable(), VALIDATE_MEMBERSHIP_PROPERTY,
				(members) -> properties.getGroup().forEach((groupName, group) -> {
					String property = "management.endpoint.health.group." + groupName;
					members.member(property + ".include".formatted(groupName), group.getInclude());
					members.member(property + ".exclude".formatted(groupName), group.getExclude());
				}));
	}

	@Bean
	@ConditionalOnMissingBean
	HealthEndpoint healthEndpoint(HealthContributorRegistry healthContributorRegistry,
			ObjectProvider<ReactiveHealthContributorRegistry> reactiveHealthContributorRegistry,
			HealthEndpointGroups groups, HealthEndpointProperties properties) {
		return new HealthEndpoint(healthContributorRegistry, reactiveHealthContributorRegistry.getIfAvailable(), groups,
				properties.getLogging().getSlowIndicatorThreshold());
	}

	@Bean
	static HealthEndpointGroupsBeanPostProcessor healthEndpointGroupsBeanPostProcessor(
			ObjectProvider<HealthEndpointGroupsPostProcessor> healthEndpointGroupsPostProcessors) {
		return new HealthEndpointGroupsBeanPostProcessor(healthEndpointGroupsPostProcessors);
	}

	/**
	 * {@link BeanPostProcessor} to invoke {@link HealthEndpointGroupsPostProcessor}
	 * beans.
	 */
	static class HealthEndpointGroupsBeanPostProcessor implements BeanPostProcessor {

		private final ObjectProvider<HealthEndpointGroupsPostProcessor> postProcessors;

		HealthEndpointGroupsBeanPostProcessor(ObjectProvider<HealthEndpointGroupsPostProcessor> postProcessors) {
			this.postProcessors = postProcessors;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof HealthEndpointGroups groups) {
				return applyPostProcessors(groups);
			}
			return bean;
		}

		private Object applyPostProcessors(HealthEndpointGroups bean) {
			for (HealthEndpointGroupsPostProcessor postProcessor : this.postProcessors.orderedStream()
				.toArray(HealthEndpointGroupsPostProcessor[]::new)) {
				bean = postProcessor.postProcessHealthEndpointGroups(bean);
			}
			return bean;
		}

	}

}
