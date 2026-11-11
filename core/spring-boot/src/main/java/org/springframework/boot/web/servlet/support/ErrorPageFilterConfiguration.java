/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.servlet.support;

import jakarta.servlet.DispatcherType;

import org.springframework.boot.web.error.ErrorPageRegistrarBeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for {@link ErrorPageFilter}.
 *
 * @author Andy Wilkinson
 * @author Jay Choi
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

	@Bean
	static ErrorPageRegistrarBeanPostProcessor errorPageRegistrarBeanPostProcessor() {
		return new ErrorPageRegistrarBeanPostProcessor();
	}

}
