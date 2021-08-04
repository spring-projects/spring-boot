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

package org.springframework.boot.test.autoconfigure.webservices.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.autoconfigure.filter.StandardAnnotationCustomizableTypeExcludeFilter;
import org.springframework.util.ObjectUtils;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.server.endpoint.annotation.Endpoint;

/**
 * {@link TypeExcludeFilter} for {@link WebServiceServerTest @WebServiceServerTest}.
 *
 * @author Daniil Razorenov
 * @since 2.6.0
 */
public class WebServiceServerTypeExcludeFilter
		extends StandardAnnotationCustomizableTypeExcludeFilter<WebServiceServerTest> {

	private static final Class<?>[] NO_ENDPOINTS = {};

	private static final Set<Class<?>> DEFAULT_INCLUDES;

	private static final Set<Class<?>> DEFAULT_INCLUDES_AND_ENDPOINT;

	static {
		Set<Class<?>> includes = new LinkedHashSet<>();
		includes.add(EndpointInterceptor.class);
		DEFAULT_INCLUDES = Collections.unmodifiableSet(includes);
	}

	static {
		Set<Class<?>> includes = new LinkedHashSet<>(DEFAULT_INCLUDES);
		includes.add(Endpoint.class);
		DEFAULT_INCLUDES_AND_ENDPOINT = Collections.unmodifiableSet(includes);
	}

	private final Class<?>[] endpoints;

	WebServiceServerTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
		this.endpoints = getAnnotation().getValue("endpoints", Class[].class).orElse(NO_ENDPOINTS);
	}

	@Override
	protected Set<Class<?>> getDefaultIncludes() {
		if (ObjectUtils.isEmpty(this.endpoints)) {
			return DEFAULT_INCLUDES_AND_ENDPOINT;
		}
		return DEFAULT_INCLUDES;
	}

	@Override
	protected Set<Class<?>> getComponentIncludes() {
		return new LinkedHashSet<>(Arrays.asList(this.endpoints));
	}

}
