/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.web.servlet.support;

import javax.servlet.DispatcherType;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for {@link ErrorPageFilter}.
 *
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
class ErrorPageFilterConfiguration {

	@Bean
	ErrorPageFilter errorPageFilter() {
		return new ErrorPageFilter();
	}

	@Bean
	FilterRegistrationBean<ErrorPageFilter> errorPageFilterRegistration(ErrorPageFilter filter) {
		FilterRegistrationBean<ErrorPageFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setOrder(filter.getOrder());
		registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
		return registration;
	}

}
