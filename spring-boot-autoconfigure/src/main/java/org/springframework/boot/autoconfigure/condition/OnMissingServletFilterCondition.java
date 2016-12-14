/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link Condition} that checks for the absence of specific Servlet Filters.
 * @author Brian Clozel
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class OnMissingServletFilterCondition extends SpringBootCondition implements ConfigurationCondition {

	@Override
	public ConfigurationPhase getConfigurationPhase() {
		return ConfigurationPhase.REGISTER_BEAN;
	}

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		ConditionMessage matchMessage = ConditionMessage.empty();
		if (metadata.isAnnotated(ConditionalOnMissingServletFilter.class.getName())) {
			List<String> servletFilters = parseAnnotationForServletFilters(context, metadata);
			Set<String> matching = getMatchingBeans(context, servletFilters);
			if (matching.isEmpty()) {
				return ConditionOutcome.match(ConditionMessage
						.forCondition(ConditionalOnMissingServletFilter.class)
						.didNotFind("any Servlet filter").atAll());
			}
			else {
				return ConditionOutcome.noMatch(ConditionMessage
						.forCondition(ConditionalOnMissingServletFilter.class)
						.found("Servlet filter", "Servlet filters")
						.items(ConditionMessage.Style.QUOTE, matching));
			}
		}
		return ConditionOutcome.match(matchMessage);
	}

	private List<String> parseAnnotationForServletFilters(ConditionContext context, AnnotatedTypeMetadata metadata) {
		List<String> servletFilters = new ArrayList<String>();
		String annotationType = ConditionalOnMissingServletFilter.class.getName();
		List<Object> values = metadata
				.getAllAnnotationAttributes(annotationType, true).get("value");
		for (Object value : values) {
			if (value instanceof String[]) {
				Collections.addAll(servletFilters, (String[]) value);
			}
			else {
				servletFilters.add((String) value);
			}
		}
		return servletFilters;
	}

	private Set<String> getMatchingBeans(ConditionContext context, List<String> servletFilters) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		if (beanFactory == null) {
			return Collections.emptySet();
		}
		Set<String> result = new LinkedHashSet<String>();
		BeanTypeRegistry beanTypeRegistry = BeanTypeRegistry.get(context.getBeanFactory());
		try {
			Map<String, FilterRegistrationBean> filterRegistrations = findFilterRegistrationBeans(context);
			for (String servletFilter : servletFilters) {
				// checking for servlet filter beans
				Class<?> filterType = ClassUtils.forName(servletFilter, context.getClassLoader());
				Assert.state(ClassUtils.isAssignable(Filter.class, filterType),
						"Type " + filterType.getName() + " is not a Servlet Filter");
				result.addAll(beanTypeRegistry.getNamesForType(filterType));
				// checking for servlet filter registrations
				for (Map.Entry<String, FilterRegistrationBean> entry : filterRegistrations.entrySet()) {
					FilterRegistrationBean registrationBean = entry.getValue();
					if (ClassUtils.isAssignableValue(filterType, registrationBean.getFilter())) {
						result.add(entry.getKey());
					}
				}
			}
		}
		catch (Throwable e) {
			return Collections.emptySet();
		}
		return result;
	}

	private Map<String, FilterRegistrationBean> findFilterRegistrationBeans(ConditionContext context) {
		BeanTypeRegistry beanTypeRegistry = BeanTypeRegistry.get(context.getBeanFactory());
		Map<String, FilterRegistrationBean> filterRegistrations = new HashMap<String, FilterRegistrationBean>();
		for (String registrationName : beanTypeRegistry.getNamesForType(FilterRegistrationBean.class)) {
			filterRegistrations.put(registrationName,
					context.getBeanFactory().getBean(registrationName, FilterRegistrationBean.class));
		}
		return filterRegistrations;
	}

}
