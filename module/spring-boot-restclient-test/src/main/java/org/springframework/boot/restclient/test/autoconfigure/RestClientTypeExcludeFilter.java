/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;
import org.springframework.util.ClassUtils;

/**
 * {@link TypeExcludeFilter} for {@link RestClientTest @RestClientTest}.
 *
 * @author Stephane Nicoll
 */
class RestClientTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<RestClientTest> {

	private static final Class<?>[] NO_COMPONENTS = {};

	private static final String[] OPTIONAL_INCLUDES = { "tools.jackson.databind.JacksonModule",
			"org.springframework.boot.jackson.JacksonComponent", "com.fasterxml.jackson.databind.Module",
			"org.springframework.boot.jackson2.JsonComponent" };

	private static final Set<Class<?>> KNOWN_INCLUDES;

	static {
		Set<Class<?>> includes = new LinkedHashSet<>();
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

	private final Class<?>[] components;

	RestClientTypeExcludeFilter(Class<?> testClass) {
		super(testClass);
		this.components = getAnnotation().getValue("components", Class[].class).orElse(NO_COMPONENTS);
	}

	@Override
	protected Set<Class<?>> getKnownIncludes() {
		return KNOWN_INCLUDES;
	}

	@Override
	protected Set<Class<?>> getComponentIncludes() {
		return new LinkedHashSet<>(Arrays.asList(this.components));
	}

}
