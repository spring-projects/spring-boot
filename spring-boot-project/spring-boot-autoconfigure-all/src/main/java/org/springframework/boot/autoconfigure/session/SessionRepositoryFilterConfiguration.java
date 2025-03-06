/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import java.util.EnumSet;
import java.util.stream.Collectors;

import jakarta.servlet.DispatcherType;

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

	private EnumSet<DispatcherType> getDispatcherTypes(SessionProperties sessionProperties) {
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
