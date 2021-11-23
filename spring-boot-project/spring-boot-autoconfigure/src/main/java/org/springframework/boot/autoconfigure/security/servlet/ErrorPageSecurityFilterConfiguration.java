/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.servlet;

import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.filter.ErrorPageSecurityFilter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;

/**
 * Configures the {@link ErrorPageSecurityFilter}.
 *
 * @author Madhura Bhave
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(WebInvocationPrivilegeEvaluator.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class ErrorPageSecurityFilterConfiguration {

	@Bean
	@ConditionalOnBean(WebInvocationPrivilegeEvaluator.class)
	FilterRegistrationBean<ErrorPageSecurityFilter> errorPageSecurityInterceptor(ApplicationContext context) {
		FilterRegistrationBean<ErrorPageSecurityFilter> registration = new FilterRegistrationBean<>(
				new ErrorPageSecurityFilter(context));
		registration.setDispatcherTypes(EnumSet.of(DispatcherType.ERROR));
		return registration;
	}

}
