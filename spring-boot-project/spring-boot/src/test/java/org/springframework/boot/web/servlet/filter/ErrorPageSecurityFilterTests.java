/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.servlet.filter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.WebInvocationPrivilegeEvaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ErrorPageSecurityFilter}.
 *
 * @author Madhura Bhave
 */
class ErrorPageSecurityFilterTests {

	private final WebInvocationPrivilegeEvaluator privilegeEvaluator = mock(WebInvocationPrivilegeEvaluator.class);

	private final ApplicationContext context = mock(ApplicationContext.class);

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final FilterChain filterChain = mock(FilterChain.class);

	private ErrorPageSecurityFilter securityFilter;

	@BeforeEach
	void setup() {
		this.request.setDispatcherType(DispatcherType.ERROR);
		given(this.context.getBean(WebInvocationPrivilegeEvaluator.class)).willReturn(this.privilegeEvaluator);
		this.securityFilter = new ErrorPageSecurityFilter(this.context);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void whenAccessIsAllowedShouldContinueDownFilterChain() throws Exception {
		given(this.privilegeEvaluator.isAllowed(anyString(), any())).willReturn(true);
		this.securityFilter.doFilter(this.request, this.response, this.filterChain);
		then(this.filterChain).should().doFilter(this.request, this.response);
	}

	@Test
	void whenAccessIsDeniedShouldCallSendError() throws Exception {
		given(this.privilegeEvaluator.isAllowed(anyString(), any())).willReturn(false);
		this.request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403);
		this.securityFilter.doFilter(this.request, this.response, this.filterChain);
		then(this.filterChain).shouldHaveNoInteractions();
		assertThat(this.response.getStatus()).isEqualTo(403);
	}

	@Test
	void whenAccessIsDeniedAndNoErrorCodeAttributeOnRequest() throws Exception {
		given(this.privilegeEvaluator.isAllowed(anyString(), any())).willReturn(false);
		SecurityContext securityContext = mock(SecurityContext.class);
		SecurityContextHolder.setContext(securityContext);
		given(securityContext.getAuthentication()).willReturn(mock(Authentication.class));
		this.securityFilter.doFilter(this.request, this.response, this.filterChain);
		then(this.filterChain).shouldHaveNoInteractions();
		assertThat(this.response.getStatus()).isEqualTo(401);
	}

	@Test
	void whenPrivilegeEvaluatorIsNotPresentAccessIsAllowed() throws Exception {
		ApplicationContext context = mock(ApplicationContext.class);
		willThrow(NoSuchBeanDefinitionException.class).given(context).getBean(WebInvocationPrivilegeEvaluator.class);
		ErrorPageSecurityFilter securityFilter = new ErrorPageSecurityFilter(context);
		securityFilter.doFilter(this.request, this.response, this.filterChain);
		then(this.filterChain).should().doFilter(this.request, this.response);
	}

	@Test
	void ignorePrivilegeEvaluationForNonErrorDispatchType() throws Exception {
		this.request.setDispatcherType(DispatcherType.REQUEST);
		given(this.privilegeEvaluator.isAllowed(anyString(), any())).willReturn(false);
		this.securityFilter.doFilter(this.request, this.response, this.filterChain);
		then(this.privilegeEvaluator).shouldHaveNoInteractions();
		then(this.filterChain).should().doFilter(this.request, this.response);
	}

	@Test
	void whenThereIsAContextPathAndServletIsMappedToSlashContextPathIsNotPassedToEvaluator() throws Exception {
		SecurityContext securityContext = mock(SecurityContext.class);
		SecurityContextHolder.setContext(securityContext);
		given(securityContext.getAuthentication()).willReturn(mock(Authentication.class));
		this.request.setRequestURI("/example/error");
		this.request.setContextPath("/example");
		// Servlet mapped to /
		this.request.setServletPath("/error");
		this.securityFilter.doFilter(this.request, this.response, this.filterChain);
		then(this.privilegeEvaluator).should().isAllowed(eq("/error"), any());
	}

	@Test
	void whenThereIsAContextPathAndServletIsMappedToWildcardPathCorrectPathIsPassedToEvaluator() throws Exception {
		SecurityContext securityContext = mock(SecurityContext.class);
		SecurityContextHolder.setContext(securityContext);
		given(securityContext.getAuthentication()).willReturn(mock(Authentication.class));
		this.request.setRequestURI("/example/dispatcher/path/error");
		this.request.setContextPath("/example");
		// Servlet mapped to /dispatcher/path/*
		this.request.setServletPath("/dispatcher/path");
		this.securityFilter.doFilter(this.request, this.response, this.filterChain);
		then(this.privilegeEvaluator).should().isAllowed(eq("/dispatcher/path/error"), any());
	}

}
