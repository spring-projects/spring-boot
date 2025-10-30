/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webmvc.test.autoconfigure;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.WebMvcRegistrations;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * {@link TypeExcludeFilter} for {@link WebMvcTest @WebMvcTest}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Yanming Zhou
 */
class WebMvcTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<WebMvcTest> {

	private static final Class<?>[] NO_CONTROLLERS = {};

	private static final String[] OPTIONAL_INCLUDES = { "tools.jackson.databind.JacksonModule",
			"org.springframework.boot.jackson.JacksonComponent",
			"org.springframework.security.config.annotation.web.WebSecurityConfigurer",
			"org.springframework.security.web.SecurityFilterChain", "org.thymeleaf.dialect.IDialect",
			"com.fasterxml.jackson.databind.Module", "org.springframework.boot.jackson2.JsonComponent" };

	private static final Set<Class<?>> KNOWN_INCLUDES;

	static {
		Set<Class<?>> includes = new LinkedHashSet<>();
		includes.add(ControllerAdvice.class);
		includes.add(WebMvcConfigurer.class);
		includes.add(WebMvcRegistrations.class);
		includes.add(jakarta.servlet.Filter.class);
		includes.add(FilterRegistrationBean.class);
		includes.add(DelegatingFilterProxyRegistrationBean.class);
		includes.add(HandlerMethodArgumentResolver.class);
		includes.add(HttpMessageConverter.class);
		includes.add(ErrorAttributes.class);
		includes.add(Converter.class);
		includes.add(GenericConverter.class);
		includes.add(HandlerInterceptor.class);
		for (String optionalInclude : OPTIONAL_INCLUDES) {
			try {
				includes.add(ClassUtils.forName(optionalInclude, null));
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		KNOWN_INCLUDES = Collections.unmodifiableSet(includes);
	}

	private static final Set<Class<?>> KNOWN_INCLUDES_AND_CONTROLLER;

	static {
		Set<Class<?>> includes = new LinkedHashSet<>(KNOWN_INCLUDES);
		includes.add(Controller.class);
		KNOWN_INCLUDES_AND_CONTROLLER = Collections.unmodifiableSet(includes);
	}

	private final Class<?>[] controllers;

	WebMvcTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
		this.controllers = getAnnotation().getValue("controllers", Class[].class).orElse(NO_CONTROLLERS);
	}

	@Override
	protected Set<Class<?>> getKnownIncludes() {
		if (ObjectUtils.isEmpty(this.controllers)) {
			return KNOWN_INCLUDES_AND_CONTROLLER;
		}
		return KNOWN_INCLUDES;
	}

	@Override
	protected Set<Class<?>> getComponentIncludes() {
		return new LinkedHashSet<>(Arrays.asList(this.controllers));
	}

}
