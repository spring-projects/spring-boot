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

package org.springframework.boot.graphql.test.autoconfigure;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import graphql.execution.instrumentation.Instrumentation;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.graphql.autoconfigure.GraphQlSourceBuilderCustomizer;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Controller;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * {@link TypeExcludeFilter} for {@link GraphQlTest @GraphQlTest}.
 *
 * @author Brian Clozel
 */
class GraphQlTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<GraphQlTest> {

	private static final Class<?>[] NO_CONTROLLERS = {};

	private static final String[] OPTIONAL_INCLUDES = { "tools.jackson.databind.JacksonModule" };

	private static final Set<Class<?>> KNOWN_INCLUDES;

	static {
		Set<Class<?>> includes = new LinkedHashSet<>();
		includes.add(JsonComponent.class);
		includes.add(RuntimeWiringConfigurer.class);
		includes.add(Converter.class);
		includes.add(GenericConverter.class);
		includes.add(DataFetcherExceptionResolver.class);
		includes.add(Instrumentation.class);
		includes.add(GraphQlSourceBuilderCustomizer.class);
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

	GraphQlTypeExcludeFilter(Class<?> testClass) {
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
