/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.autoconfigure.web.servlet;

import java.util.function.Consumer;

import jakarta.servlet.http.HttpServletRequest;
import org.assertj.core.api.AssertDelegateTarget;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.h2console.autoconfigure.H2ConsoleProperties;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link PathRequest}.
 *
 * @author Madhura Bhave
 */
class PathRequestTests {

	@Test
	void toStaticResourcesShouldReturnStaticResourceRequest() {
		assertThat(PathRequest.toStaticResources()).isInstanceOf(StaticResourceRequest.class);
	}

	@Test
	void toH2ConsoleShouldMatchH2ConsolePath() {
		RequestMatcher matcher = PathRequest.toH2Console();
		assertMatcher(matcher).matches("/h2-console");
		assertMatcher(matcher).matches("/h2-console/subpath");
		assertMatcher(matcher).doesNotMatch("/js/file.js");
	}

	@ParameterizedTest
	@ValueSource(strings = { "/custom", "/custom/" })
	void toH2ConsoleShouldMatchCustomH2ConsolePath(String consolePath) {
		RequestMatcher matcher = PathRequest.toH2Console();
		assertMatcher(matcher, (properties) -> properties.setPath(consolePath)).matches("/custom");
		assertMatcher(matcher, (properties) -> properties.setPath(consolePath)).matches("/custom/subpath");
		assertMatcher(matcher, (properties) -> properties.setPath(consolePath)).doesNotMatch("/h2-console");
		assertMatcher(matcher, (properties) -> properties.setPath(consolePath)).doesNotMatch("/h2-console/subpath");
		assertMatcher(matcher, (properties) -> properties.setPath(consolePath)).doesNotMatch("/js/file.js");
	}

	@Test
	void toH2ConsoleWhenManagementContextShouldNeverMatch() {
		RequestMatcher matcher = PathRequest.toH2Console();
		assertMatcher(matcher, "management").doesNotMatch("/h2-console");
		assertMatcher(matcher, "management").doesNotMatch("/h2-console/subpath");
		assertMatcher(matcher, "management").doesNotMatch("/js/file.js");
	}

	@Test
	@ClassPathExclusions(packages = "org.springframework.boot.web.server.context")
	void toH2ConsoleWhenNoWebServerContextClassPresent() {
		assertThatExceptionOfType(NoClassDefFoundError.class)
			.isThrownBy(() -> WebServerApplicationContext.class.getName());
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		RequestMatcher matcher = PathRequest.toH2Console();
		assertMatcher(matcher, context).matches("/h2-console");
		assertMatcher(matcher, context).matches("/h2-console/subpath");
		assertMatcher(matcher, context).doesNotMatch("/js/file.js");
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher) {
		return assertMatcher(matcher, (String) null);
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher, WebApplicationContext context) {
		return assertMatcher(matcher, context, null);
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher, @Nullable String serverNamespace) {
		return assertMatcher(matcher, serverNamespace, null);
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher,
			Consumer<H2ConsoleProperties> consolePropertiesConfigurer) {
		return assertMatcher(matcher, (String) null, consolePropertiesConfigurer);
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher, @Nullable String serverNamespace,
			@Nullable Consumer<H2ConsoleProperties> consolePropertiesConfigurer) {
		return assertMatcher(matcher, new TestWebApplicationContext(serverNamespace), consolePropertiesConfigurer);
	}

	private RequestMatcherAssert assertMatcher(RequestMatcher matcher, WebApplicationContext context,
			@Nullable Consumer<H2ConsoleProperties> consolePropertiesConfigurer) {
		GenericApplicationContext genericContext = (GenericApplicationContext) context;
		genericContext.registerBean(ServerProperties.class);
		genericContext.registerBean(H2ConsoleProperties.class);
		if (consolePropertiesConfigurer != null) {
			consolePropertiesConfigurer.accept(context.getBean(H2ConsoleProperties.class));
		}
		return assertThat(new RequestMatcherAssert(context, matcher));
	}

	static class RequestMatcherAssert implements AssertDelegateTarget {

		private final WebApplicationContext context;

		private final RequestMatcher matcher;

		RequestMatcherAssert(WebApplicationContext context, RequestMatcher matcher) {
			this.context = context;
			this.matcher = matcher;
		}

		void matches(String path) {
			matches(mockRequest(path));
		}

		private void matches(HttpServletRequest request) {
			assertThat(this.matcher.matches(request)).as("Matches " + getRequestPath(request)).isTrue();
		}

		void doesNotMatch(String path) {
			doesNotMatch(mockRequest(path));
		}

		private void doesNotMatch(HttpServletRequest request) {
			assertThat(this.matcher.matches(request)).as("Does not match " + getRequestPath(request)).isFalse();
		}

		private MockHttpServletRequest mockRequest(String path) {
			MockServletContext servletContext = new MockServletContext();
			servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, this.context);
			MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
			request.setRequestURI(path);
			return request;
		}

		private String getRequestPath(HttpServletRequest request) {
			String url = request.getServletPath();
			if (StringUtils.hasText(request.getRequestURI())) {
				url += request.getRequestURI();
			}
			return url;
		}

	}

}
