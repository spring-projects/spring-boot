/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure.actuate.web;

import io.micrometer.observation.ObservationRegistry;
import jakarta.servlet.DispatcherType;

import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.server.observation.ServerRequestObservationConvention;
import org.springframework.web.filter.ServerHttpObservationFilter;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Registers HTTP request observations in a separate management servlet context.
 *
 * @author Aditya Reddy
 */
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ DispatcherServlet.class, ObservationRegistry.class })
@ConditionalOnBean({ ObservationRegistry.class, ServerRequestObservationConvention.class })
class WebMvcObservationManagementContextConfiguration {

	@Bean
	@ConditionalOnMissingBean(value = ServerHttpObservationFilter.class,
			parameterizedContainer = FilterRegistrationBean.class, search = SearchStrategy.CURRENT)
	FilterRegistrationBean<ServerHttpObservationFilter> managementWebMvcObservationFilter(ObservationRegistry registry,
			ServerRequestObservationConvention convention) {
		ServerHttpObservationFilter filter = new ServerHttpObservationFilter(registry, convention);
		FilterRegistrationBean<ServerHttpObservationFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
		registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC);
		return registration;
	}

}
