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

package org.springframework.boot.autoconfigure.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.web.servlet.error.ErrorController;
import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.endpoint.EndpointPathResolver;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Provides request matchers that can be used to configure security for static resources
 * and the error controller path in a custom {@link WebSecurityConfigurerAdapter}.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public final class SpringBootSecurity {

	/**
	 * Used as a wildcard matcher for all endpoints.
	 */
	public final static String ALL_ENDPOINTS = "**";

	private static String[] STATIC_RESOURCES = new String[] { "/css/**", "/js/**",
			"/images/**", "/webjars/**", "/**/favicon.ico" };

	private final EndpointPathResolver endpointPathResolver;

	private final ErrorController errorController;

	SpringBootSecurity(EndpointPathResolver endpointPathResolver,
			ErrorController errorController) {
		this.endpointPathResolver = endpointPathResolver;
		this.errorController = errorController;
	}

	/**
	 * Returns a {@link RequestMatcher} that matches on all endpoint paths with given ids.
	 * @param ids the endpoint ids
	 * @return the request matcher
	 */
	public RequestMatcher endpointIds(String... ids) {
		Assert.notEmpty(ids, "At least one endpoint id must be specified.");
		List<String> pathList = Arrays.asList(ids);
		if (pathList.contains(ALL_ENDPOINTS)) {
			return new AntPathRequestMatcher(
					this.endpointPathResolver.resolvePath(ALL_ENDPOINTS), null);
		}
		return getEndpointsRequestMatcher(pathList);
	}

	/**
	 * Returns a {@link RequestMatcher} that matches on all endpoint paths for the given
	 * classes with the {@link Endpoint} annotation.
	 * @param endpoints the endpoint classes
	 * @return the request matcher
	 */
	public RequestMatcher endpoints(Class<?>... endpoints) {
		Assert.notEmpty(endpoints, "At least one endpoint must be specified.");
		List<String> paths = Arrays.stream(endpoints).map((e) -> {
			if (e.isAnnotationPresent(Endpoint.class)) {
				return e.getAnnotation(Endpoint.class).id();
			}
			throw new IllegalArgumentException(
					"Only classes annotated with @Endpoint are supported.");
		}).collect(Collectors.toList());
		return getEndpointsRequestMatcher(paths);
	}

	/**
	 * Returns a {@link RequestMatcher} that matches on all static resources.
	 * @return the request matcher
	 */
	public RequestMatcher staticResources() {
		return getRequestMatcher(STATIC_RESOURCES);
	}

	/**
	 * Returns a {@link RequestMatcher} that matches on the {@link ErrorController} path,
	 * if present.
	 * @return the request matcher
	 */
	public RequestMatcher error() {
		if (this.errorController == null) {
			throw new IllegalStateException(
					"Path for error controller could not be determined.");
		}
		String path = normalizePath(this.errorController.getErrorPath());
		return new AntPathRequestMatcher(path + "/**", null);
	}

	private RequestMatcher getEndpointsRequestMatcher(List<String> ids) {
		List<RequestMatcher> matchers = new ArrayList<>();
		for (String id : ids) {
			String path = this.endpointPathResolver.resolvePath(id);
			matchers.add(new AntPathRequestMatcher(path + "/**", null));
		}
		return new OrRequestMatcher(matchers);
	}

	private static RequestMatcher getRequestMatcher(String... paths) {
		List<RequestMatcher> matchers = new ArrayList<>();
		for (String path : paths) {
			matchers.add(new AntPathRequestMatcher(path, null));
		}
		return new OrRequestMatcher(matchers);
	}

	private String normalizePath(String errorPath) {
		String result = StringUtils.cleanPath(errorPath);
		if (!result.startsWith("/")) {
			result = "/" + result;
		}
		return result;
	}

}
