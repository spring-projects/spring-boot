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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.boot.grpc.server.autoconfigure.health.GrpcServerHealthProperties.Service;
import org.springframework.boot.grpc.server.autoconfigure.health.GrpcServerHealthProperties.Status;
import org.springframework.boot.grpc.server.health.HealthCheckedGrpcComponent;
import org.springframework.boot.grpc.server.health.HealthCheckedGrpcComponents;
import org.springframework.boot.grpc.server.health.StatusAggregator;
import org.springframework.boot.grpc.server.health.StatusMapper;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorMembership;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Auto-configured {@link HealthCheckedGrpcComponents}.
 *
 * @author Phillip Webb
 */
class AutoConfiguredHealthCheckedGrpcComponents implements HealthCheckedGrpcComponents {

	private final HealthCheckedGrpcComponent server;

	private final Map<String, HealthCheckedGrpcComponent> services;

	/**
	 * Create a new {@link AutoConfiguredHealthCheckedGrpcComponents} instance.
	 * @param applicationContext the application context used to check for override beans
	 * @param properties the gRPC server health properties
	 */
	AutoConfiguredHealthCheckedGrpcComponents(ApplicationContext applicationContext,
			GrpcServerHealthProperties properties) {
		ListableBeanFactory beanFactory = (applicationContext instanceof ConfigurableApplicationContext configurableContext)
				? configurableContext.getBeanFactory() : applicationContext;
		StatusAggregator statusAggregator = getNonQualifiedBean(beanFactory, StatusAggregator.class,
				() -> StatusAggregator.of(properties.getStatus().getOrder()));
		StatusMapper statusMapper = getNonQualifiedBean(beanFactory, StatusMapper.class,
				() -> StatusMapper.of(properties.getStatus().getMapping()));
		this.server = new AutoConfiguredHealthCheckedGrpcComponent(HealthContributorMembership.always(),
				statusAggregator, statusMapper);
		this.services = createServices(properties.getService(), beanFactory, statusAggregator, statusMapper);
	}

	private Map<String, HealthCheckedGrpcComponent> createServices(Map<String, Service> serviceProperties,
			BeanFactory beanFactory, StatusAggregator defaultStatusAggregator, StatusMapper defaultStatusMapper) {
		Map<String, HealthCheckedGrpcComponent> services = new TreeMap<>();
		serviceProperties.forEach((serviceName, service) -> {
			Status status = service.getStatus();
			StatusAggregator statusAggregator = getQualifiedBean(beanFactory, StatusAggregator.class, serviceName,
					() -> createStatusAggregator(status.getOrder(), defaultStatusAggregator));
			StatusMapper statusMapper = getQualifiedBean(beanFactory, StatusMapper.class, serviceName,
					() -> createStatusMapper(status.getMapping(), defaultStatusMapper));
			HealthContributorMembership membership = HealthContributorMembership.byIncludeExclude(service.getInclude(),
					service.getExclude());
			services.put(serviceName,
					new AutoConfiguredHealthCheckedGrpcComponent(membership, statusAggregator, statusMapper));
		});
		return Collections.unmodifiableMap(services);
	}

	private StatusAggregator createStatusAggregator(List<String> order, StatusAggregator defaultStatusAggregator) {
		return (!CollectionUtils.isEmpty(order)) ? StatusAggregator.of(order) : defaultStatusAggregator;
	}

	private StatusMapper createStatusMapper(Map<String, ServingStatus> mapping, StatusMapper defaultStatusMapper) {
		return (!CollectionUtils.isEmpty(mapping)) ? StatusMapper.of(mapping) : defaultStatusMapper;
	}

	private <T> T getNonQualifiedBean(ListableBeanFactory beanFactory, Class<T> type, Supplier<T> fallback) {
		List<String> candidates = new ArrayList<>();
		for (String beanName : BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, type)) {
			String[] aliases = beanFactory.getAliases(beanName);
			if (!BeanFactoryAnnotationUtils.isQualifierMatch(
					(qualifier) -> !qualifier.equals(beanName) && !ObjectUtils.containsElement(aliases, qualifier),
					beanName, beanFactory)) {
				candidates.add(beanName);
			}
		}
		if (candidates.isEmpty()) {
			return fallback.get();
		}
		if (candidates.size() == 1) {
			return beanFactory.getBean(candidates.get(0), type);
		}
		return beanFactory.getBean(type);
	}

	private <T> T getQualifiedBean(BeanFactory beanFactory, Class<T> type, String qualifier, Supplier<T> fallback) {
		try {
			return BeanFactoryAnnotationUtils.qualifiedBeanOfType(beanFactory, type, qualifier);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return fallback.get();
		}
	}

	@Override
	public @Nullable HealthCheckedGrpcComponent getServer() {
		return this.server;
	}

	@Override
	public Set<String> getServiceNames() {
		return this.services.keySet();
	}

	@Override
	public @Nullable HealthCheckedGrpcComponent getService(String serviceName) {
		return this.services.get(serviceName);
	}

}
