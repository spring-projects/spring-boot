/*
 * Copyright 2012-2022 the original author or authors.
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
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointProperties.Group;
import org.springframework.boot.actuate.autoconfigure.health.HealthProperties.Show;
import org.springframework.boot.actuate.autoconfigure.health.HealthProperties.Status;
import org.springframework.boot.actuate.health.AdditionalHealthEndpointPath;
import org.springframework.boot.actuate.health.HealthEndpointGroup;
import org.springframework.boot.actuate.health.HealthEndpointGroups;
import org.springframework.boot.actuate.health.HttpCodeStatusMapper;
import org.springframework.boot.actuate.health.SimpleHttpCodeStatusMapper;
import org.springframework.boot.actuate.health.SimpleStatusAggregator;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * Auto-configured {@link HealthEndpointGroups}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class AutoConfiguredHealthEndpointGroups implements HealthEndpointGroups {

	private static final Predicate<String> ALL = (name) -> true;

	private final HealthEndpointGroup primaryGroup;

	private final Map<String, HealthEndpointGroup> groups;

	/**
	 * Create a new {@link AutoConfiguredHealthEndpointGroups} instance.
	 * @param applicationContext the application context used to check for override beans
	 * @param properties the health endpoint properties
	 */
	AutoConfiguredHealthEndpointGroups(ApplicationContext applicationContext, HealthEndpointProperties properties) {
		ListableBeanFactory beanFactory = (applicationContext instanceof ConfigurableApplicationContext configurableContext)
				? configurableContext.getBeanFactory() : applicationContext;
		Show showComponents = properties.getShowComponents();
		Show showDetails = properties.getShowDetails();
		Set<String> roles = properties.getRoles();
		StatusAggregator statusAggregator = getNonQualifiedBean(beanFactory, StatusAggregator.class);
		if (statusAggregator == null) {
			statusAggregator = new SimpleStatusAggregator(properties.getStatus().getOrder());
		}
		HttpCodeStatusMapper httpCodeStatusMapper = getNonQualifiedBean(beanFactory, HttpCodeStatusMapper.class);
		if (httpCodeStatusMapper == null) {
			httpCodeStatusMapper = new SimpleHttpCodeStatusMapper(properties.getStatus().getHttpMapping());
		}
		this.primaryGroup = new AutoConfiguredHealthEndpointGroup(ALL, statusAggregator, httpCodeStatusMapper,
				showComponents, showDetails, roles, null);
		this.groups = createGroups(properties.getGroup(), beanFactory, statusAggregator, httpCodeStatusMapper,
				showComponents, showDetails, roles);
	}

	private Map<String, HealthEndpointGroup> createGroups(Map<String, Group> groupProperties, BeanFactory beanFactory,
			StatusAggregator defaultStatusAggregator, HttpCodeStatusMapper defaultHttpCodeStatusMapper,
			Show defaultShowComponents, Show defaultShowDetails, Set<String> defaultRoles) {
		Map<String, HealthEndpointGroup> groups = new LinkedHashMap<>();
		groupProperties.forEach((groupName, group) -> {
			Status status = group.getStatus();
			Show showComponents = (group.getShowComponents() != null) ? group.getShowComponents()
					: defaultShowComponents;
			Show showDetails = (group.getShowDetails() != null) ? group.getShowDetails() : defaultShowDetails;
			Set<String> roles = !CollectionUtils.isEmpty(group.getRoles()) ? group.getRoles() : defaultRoles;
			StatusAggregator statusAggregator = getQualifiedBean(beanFactory, StatusAggregator.class, groupName, () -> {
				if (!CollectionUtils.isEmpty(status.getOrder())) {
					return new SimpleStatusAggregator(status.getOrder());
				}
				return defaultStatusAggregator;
			});
			HttpCodeStatusMapper httpCodeStatusMapper = getQualifiedBean(beanFactory, HttpCodeStatusMapper.class,
					groupName, () -> {
						if (!CollectionUtils.isEmpty(status.getHttpMapping())) {
							return new SimpleHttpCodeStatusMapper(status.getHttpMapping());
						}
						return defaultHttpCodeStatusMapper;
					});
			Predicate<String> members = new IncludeExcludeGroupMemberPredicate(group.getInclude(), group.getExclude());
			AdditionalHealthEndpointPath additionalPath = (group.getAdditionalPath() != null)
					? AdditionalHealthEndpointPath.from(group.getAdditionalPath()) : null;
			groups.put(groupName, new AutoConfiguredHealthEndpointGroup(members, statusAggregator, httpCodeStatusMapper,
					showComponents, showDetails, roles, additionalPath));
		});
		return Collections.unmodifiableMap(groups);
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
