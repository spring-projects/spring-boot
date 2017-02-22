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

import javax.servlet.DispatcherType;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
class SessionRepositoryFilterConfiguration {

	@Bean
	public FilterRegistrationBean<SessionRepositoryFilter<?>> sessionRepositoryFilterRegistration(
			SessionRepositoryFilter<?> filter) {
		FilterRegistrationBean<SessionRepositoryFilter<?>> registration = new FilterRegistrationBean<SessionRepositoryFilter<?>>(
				filter);
		registration.setDispatcherTypes(EnumSet.of(DispatcherType.ASYNC,
				DispatcherType.ERROR, DispatcherType.REQUEST));
		registration.setOrder(SessionRepositoryFilter.DEFAULT_ORDER);
		return registration;
	}

}
