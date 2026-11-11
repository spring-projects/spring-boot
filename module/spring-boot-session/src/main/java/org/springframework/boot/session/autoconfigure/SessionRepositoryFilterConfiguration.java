/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.session.autoconfigure;

import java.util.EnumSet;
import java.util.stream.Collectors;

import jakarta.servlet.DispatcherType;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.util.Assert;

/**
 * Configuration for customizing the registration of the {@link SessionRepositoryFilter}.
 *
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(SessionRepositoryFilter.class)
@EnableConfigurationProperties(SessionProperties.class)
class SessionRepositoryFilterConfiguration {

	@Bean
	DelegatingFilterProxyRegistrationBean sessionRepositoryFilterRegistration(SessionProperties sessionProperties,
			ListableBeanFactory beanFactory) {
		String[] targetBeanNames = beanFactory.getBeanNamesForType(SessionRepositoryFilter.class, false, false);
		Assert.state(targetBeanNames.length == 1, "Expected single SessionRepositoryFilter bean");
		DelegatingFilterProxyRegistrationBean registration = new DelegatingFilterProxyRegistrationBean(
				targetBeanNames[0]);
		registration.setDispatcherTypes(getDispatcherTypes(sessionProperties));
		registration.setOrder(sessionProperties.getServlet().getFilterOrder());
		return registration;
	}

	private @Nullable EnumSet<DispatcherType> getDispatcherTypes(SessionProperties sessionProperties) {
		SessionProperties.Servlet servletProperties = sessionProperties.getServlet();
		if (servletProperties.getFilterDispatcherTypes() == null) {
			return null;
		}
		return servletProperties.getFilterDispatcherTypes()
			.stream()
			.map((type) -> DispatcherType.valueOf(type.name()))
			.collect(Collectors.toCollection(() -> EnumSet.noneOf(DispatcherType.class)));
	}

}
