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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointProperties.Group;
import org.springframework.boot.actuate.autoconfigure.health.HealthProperties.Status;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroup.Show;
import org.springframework.boot.actuate.health.HealthEndpointGroupConfigurer;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HealthEndpointGroupsRegistry;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.SimpleHttpCodeStatusMapper;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Auto-configured {@link HealthEndpointGroupsRegistry}.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 */
class AutoConfiguredHealthEndpointGroupsRegistry implements HealthEndpointGroupsRegistry {

	private static Predicate<String> ALL = (name) -> true;

	private final StatusAggregator defaultStatusAggregator;

	private final HttpCodeStatusMapper defaultHttpCodeStatusMapper;

	private final Show defaultShowComponents;

	private final Show defaultShowDetails;

	private final Set<String> defaultRoles;

	private HealthEndpointGroup primaryGroup;

	private final Map<String, HealthEndpointGroup> groups;

	/**
	 * Create a new {@link AutoConfiguredHealthEndpointGroupsRegistry} instance.
	 * @param applicationContext the application context used to check for override beans
	 * @param properties the health endpoint properties
	 */
	AutoConfiguredHealthEndpointGroupsRegistry(ApplicationContext applicationContext,
			HealthEndpointProperties properties) {
		ListableBeanFactory beanFactory = (applicationContext instanceof ConfigurableApplicationContext)
				? ((ConfigurableApplicationContext) applicationContext).getBeanFactory() : applicationContext;
		this.defaultShowComponents = convertVisibility(properties.getShowComponents());
		this.defaultShowDetails = convertVisibility(properties.getShowDetails());
		this.defaultRoles = properties.getRoles();

		StatusAggregator statusAggregator = getNonQualifiedBean(beanFactory, StatusAggregator.class);
		if (statusAggregator == null) {
			statusAggregator = new SimpleStatusAggregator(properties.getStatus().getOrder());
		}
		this.defaultStatusAggregator = statusAggregator;

		HttpCodeStatusMapper httpCodeStatusMapper = getNonQualifiedBean(beanFactory, HttpCodeStatusMapper.class);
		if (httpCodeStatusMapper == null) {
			httpCodeStatusMapper = new SimpleHttpCodeStatusMapper(properties.getStatus().getHttpMapping());
		}
		this.defaultHttpCodeStatusMapper = httpCodeStatusMapper;

		this.primaryGroup = new DefaultHealthEndpointGroup(ALL, statusAggregator, httpCodeStatusMapper,
				this.defaultShowComponents, this.defaultShowDetails, this.defaultRoles);

		this.groups = createGroups(properties.getGroup(), beanFactory);
	}

	@Override
	public HealthEndpointGroupsRegistry add(String groupName, Consumer<HealthEndpointGroupConfigurer> consumer) {
		DefaultHealthEndpointGroupConfigurer groupConfigurer = new DefaultHealthEndpointGroupConfigurer(
				this.defaultStatusAggregator, this.defaultHttpCodeStatusMapper, this.defaultShowComponents,
				this.defaultShowDetails, this.defaultRoles);
		consumer.accept(groupConfigurer);
		this.groups.put(groupName, groupConfigurer.toHealthEndpointGroup());
		return this;
	}

	@Override
	public HealthEndpointGroupsRegistry remove(String groupName) {
		this.groups.remove(groupName);
		return this;
	}

	@Override
	public HealthEndpointGroups toGroups() {
		return HealthEndpointGroups.of(this.primaryGroup, Collections.unmodifiableMap(this.groups));
	}

	private Map<String, HealthEndpointGroup> createGroups(Map<String, Group> groupProperties, BeanFactory beanFactory) {
		Map<String, HealthEndpointGroup> groups = new LinkedHashMap<>();
		groupProperties
				.forEach((groupName, group) -> groups.put(groupName, createGroup(groupName, group, beanFactory)));
		return groups;
	}

	private HealthEndpointGroup createGroup(String groupName, Group groupProperties, BeanFactory beanFactory) {
		Status status = groupProperties.getStatus();
		Show showComponents = (groupProperties.getShowComponents() != null)
				? convertVisibility(groupProperties.getShowComponents()) : this.defaultShowComponents;
		Show showDetails = (groupProperties.getShowDetails() != null)
				? convertVisibility(groupProperties.getShowDetails()) : this.defaultShowDetails;
		Set<String> roles = !CollectionUtils.isEmpty(groupProperties.getRoles()) ? groupProperties.getRoles()
				: this.defaultRoles;
		StatusAggregator statusAggregator = getQualifiedBean(beanFactory, StatusAggregator.class, groupName, () -> {
			if (!CollectionUtils.isEmpty(status.getOrder())) {
				return new SimpleStatusAggregator(status.getOrder());
			}
			return this.defaultStatusAggregator;
		});
		HttpCodeStatusMapper httpCodeStatusMapper = getQualifiedBean(beanFactory, HttpCodeStatusMapper.class, groupName,
				() -> {
					if (!CollectionUtils.isEmpty(status.getHttpMapping())) {
						return new SimpleHttpCodeStatusMapper(status.getHttpMapping());
					}
					return this.defaultHttpCodeStatusMapper;
				});
		Predicate<String> members = new IncludeExcludeGroupMemberPredicate(groupProperties.getInclude(),
				groupProperties.getExclude());
		return new DefaultHealthEndpointGroup(members, statusAggregator, httpCodeStatusMapper, showComponents,
				showDetails, roles);
	}

	private <T> T getNonQualifiedBean(ListableBeanFactory beanFactory, Class<T> type) {
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
			return null;
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

	private Show convertVisibility(HealthProperties.Show show) {
		if (show == null) {
			return null;
		}
		switch (show) {
		case ALWAYS:
			return Show.ALWAYS;
		case NEVER:
			return Show.NEVER;
		case WHEN_AUTHORIZED:
			return Show.WHEN_AUTHORIZED;
		}
		throw new IllegalStateException("Unsupported 'show' value " + show);
	}

	@Override
	public HealthEndpointGroup getPrimary() {
		return this.primaryGroup;
	}

	@Override
	public Set<String> getNames() {
		return this.groups.keySet();
	}

	@Override
	public HealthEndpointGroup get(String name) {
		return this.groups.get(name);
	}

}
