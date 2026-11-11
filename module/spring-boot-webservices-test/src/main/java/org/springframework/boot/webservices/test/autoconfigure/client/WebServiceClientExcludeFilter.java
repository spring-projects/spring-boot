/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.test.autoconfigure.client;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.filter.annotation.StandardAnnotationCustomizableTypeExcludeFilter;

/**
 * {@link TypeExcludeFilter} for {@link WebServiceClientTest @WebServiceClientTest}.
 *
 * @author Dmytro Nosan
 */
class WebServiceClientExcludeFilter extends StandardAnnotationCustomizableTypeExcludeFilter<WebServiceClientTest> {

	private final Class<?>[] components;

	WebServiceClientExcludeFilter(Class<?> testClass) {
		super(testClass);
		this.components = getAnnotation().getValue("components", Class[].class).orElseGet(() -> new Class<?>[0]);
	}

	@Override
	protected Set<Class<?>> getComponentIncludes() {
		return new LinkedHashSet<>(Arrays.asList(this.components));
	}

}
