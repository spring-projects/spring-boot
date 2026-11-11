/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient.test.autoconfigure;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;
import org.springframework.util.ClassUtils;

/**
 * {@link TypeExcludeFilter} for {@link WebClientTest @WebClientTest}.
 *
 * @author Andy Wilkinson
 */
class WebClientTypeExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<WebClientTest> {

	private static final Class<?>[] NO_COMPONENTS = {};

	private static final String DATABIND_MODULE_CLASS_NAME = "tools.jackson.databind.JacksonModule";

	private static final Set<Class<?>> KNOWN_INCLUDES;

	static {
		Set<Class<?>> includes = new LinkedHashSet<>();
		if (ClassUtils.isPresent(DATABIND_MODULE_CLASS_NAME, WebClientTypeExcludeFilter.class.getClassLoader())) {
			try {
				includes.add(Class.forName(DATABIND_MODULE_CLASS_NAME, true,
						WebClientTypeExcludeFilter.class.getClassLoader()));
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException("Failed to load " + DATABIND_MODULE_CLASS_NAME, ex);
			}
			includes.add(JacksonComponent.class);
		}
		KNOWN_INCLUDES = Collections.unmodifiableSet(includes);
	}

	private final Class<?>[] components;

	WebClientTypeExcludeFilter(Class<?> testClass) {
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
