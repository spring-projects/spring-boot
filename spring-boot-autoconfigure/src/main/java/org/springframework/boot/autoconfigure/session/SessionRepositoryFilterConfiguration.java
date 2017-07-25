/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.DispatcherType;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.SessionRepositoryFilter;

/**
 * Configuration for customizing the registration of the {@link SessionRepositoryFilter}.
 *
 * @author Andy Wilkinson
 */
@Configuration
@ConditionalOnBean(SessionRepositoryFilter.class)
@EnableConfigurationProperties(SessionProperties.class)
class SessionRepositoryFilterConfiguration {

	@Bean
	public FilterRegistrationBean<SessionRepositoryFilter<?>> sessionRepositoryFilterRegistration(
			SessionProperties sessionProperties, SessionRepositoryFilter<?> filter) {
		FilterRegistrationBean<SessionRepositoryFilter<?>> registration = new FilterRegistrationBean<>(
				filter);
		registration.setDispatcherTypes(getDispatcherTypes(sessionProperties));
		registration.setOrder(sessionProperties.getServlet().getFilterOrder());
		return registration;
	}

	private EnumSet<DispatcherType> getDispatcherTypes(
			SessionProperties sessionProperties) {
		Set<String> filterDispatcherTypes = sessionProperties.getServlet()
				.getFilterDispatcherTypes();
		if (filterDispatcherTypes == null) {
			return null;
		}
		return filterDispatcherTypes.stream()
				.map((type) -> type.toUpperCase(Locale.ENGLISH))
				.map(DispatcherType::valueOf).collect(Collectors
						.collectingAndThen(Collectors.toSet(), EnumSet::copyOf));
	}

}
