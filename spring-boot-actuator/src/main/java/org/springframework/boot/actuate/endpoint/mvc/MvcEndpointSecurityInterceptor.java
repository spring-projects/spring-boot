/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Security interceptor for MvcEndpoints.
 *
 * @author Madhura Bhave
 * @since 1.5.0
 */
public class MvcEndpointSecurityInterceptor extends HandlerInterceptorAdapter {

	private static final Log logger = LogFactory
			.getLog(MvcEndpointSecurityInterceptor.class);

	private final boolean secure;

	private final List<String> roles;

	private AtomicBoolean loggedUnauthorizedAttempt = new AtomicBoolean();

	public MvcEndpointSecurityInterceptor(boolean secure, List<String> roles) {
		this.secure = secure;
		this.roles = roles;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
			Object handler) throws Exception {
		if (CorsUtils.isPreFlightRequest(request) || !this.secure) {
			return true;
		}
		HandlerMethod handlerMethod = (HandlerMethod) handler;
		if (HttpMethod.OPTIONS.matches(request.getMethod())
				&& !(handlerMethod.getBean() instanceof MvcEndpoint)) {
			return true;
		}
		MvcEndpoint mvcEndpoint = (MvcEndpoint) handlerMethod.getBean();
		if (!mvcEndpoint.isSensitive()) {
			return true;
		}
		if (isUserAllowedAccess(request)) {
			return true;
		}
		sendFailureResponse(request, response);
		return false;
	}

	private boolean isUserAllowedAccess(HttpServletRequest request) {
		AuthoritiesValidator authoritiesValidator = null;
		if (isSpringSecurityAvailable()) {
			authoritiesValidator = new AuthoritiesValidator();
		}
		for (String role : this.roles) {
			if (request.isUserInRole(role)) {
				return true;
			}
			if (authoritiesValidator != null && authoritiesValidator.hasAuthority(role)) {
				return true;
			}
		}
		return false;
	}

	private boolean isSpringSecurityAvailable() {
		return ClassUtils.isPresent(
				"org.springframework.security.config.annotation.web.WebSecurityConfigurer",
				getClass().getClassLoader());
	}

	private void sendFailureResponse(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		if (request.getUserPrincipal() != null) {
			String roles = StringUtils.collectionToDelimitedString(this.roles, " ");
			response.sendError(HttpStatus.FORBIDDEN.value(),
					"Access is denied. User must have one of the these roles: " + roles);
		}
		else {
			logUnauthorizedAttempt();
			response.sendError(HttpStatus.UNAUTHORIZED.value(),
					"Full authentication is required to access this resource.");
		}
	}

	private void logUnauthorizedAttempt() {
		if (this.loggedUnauthorizedAttempt.compareAndSet(false, true)
				&& logger.isInfoEnabled()) {
			logger.info("Full authentication is required to access "
					+ "actuator endpoints. Consider adding Spring Security "
					+ "or set 'management.security.enabled' to false.");
		}
	}

	/**
	 * Inner class to check authorities using Spring Security (when available).
	 */
	private static class AuthoritiesValidator {

		private boolean hasAuthority(String role) {
			Authentication authentication = SecurityContextHolder.getContext()
					.getAuthentication();
			if (authentication != null) {
				for (GrantedAuthority authority : authentication.getAuthorities()) {
					if (authority.getAuthority().equals(role)) {
						return true;
					}
				}
			}
			return false;
		}

	}

}
