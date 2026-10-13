/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.autoconfigure.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.grpc.server.GrpcServletRegistration;
import org.springframework.boot.grpc.server.autoconfigure.security.web.servlet.GrpcRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.ObjectUtils;

/**
 * A custom {@link AbstractHttpConfigurer} that disables CSRF protection for gRPC
 * requests.
 * <p>
 * This configurer checks the application context to determine if CSRF protection should
 * be disabled for gRPC requests based on the property
 * {@code spring.grpc.server.security.csrf.enabled}. By default, CSRF protection is
 * disabled unless explicitly enabled in the application properties.
 * </p>
 *
 * @author Dave Syer
 * @see AbstractHttpConfigurer
 * @see HttpSecurity
 */
class GrpcDisableCsrfHttpConfigurer extends AbstractHttpConfigurer<GrpcDisableCsrfHttpConfigurer, HttpSecurity> {

	@Override
	public void init(HttpSecurity http) {
		ApplicationContext context = http.getSharedObject(ApplicationContext.class);
		if (context != null && isCsrfConfigurerPresent(http) && hasBean(context, GrpcServiceDiscoverer.class)
				&& hasBean(context, GrpcServletRegistration.class) && !isCsrfEnabled(context)) {
			http.csrf(this::disable);
		}
	}

	@SuppressWarnings("unchecked")
	private boolean isCsrfConfigurerPresent(HttpSecurity http) {
		return http.getConfigurer(CsrfConfigurer.class) != null;
	}

	private boolean hasBean(ApplicationContext context, Class<?> type) {
		return !ObjectUtils.isEmpty(BeanFactoryUtils.beanNamesForTypeIncludingAncestors(context, type));
	}

	private boolean isCsrfEnabled(ApplicationContext context) {
		return context.getEnvironment().getProperty("spring.grpc.server.security.csrf.enabled", Boolean.class, false);
	}

	private void disable(CsrfConfigurer<HttpSecurity> csrf) {
		csrf.requireCsrfProtectionMatcher(GrpcCsrfRequestMatcher.INSTANCE);
	}

	static class GrpcCsrfRequestMatcher implements RequestMatcher {

		static final GrpcCsrfRequestMatcher INSTANCE = new GrpcCsrfRequestMatcher();

		@Override
		public boolean matches(HttpServletRequest request) {
			return CsrfFilter.DEFAULT_CSRF_MATCHER.matches(request) && !GrpcRequest.toAnyService().matches(request);
		}

	}

}
