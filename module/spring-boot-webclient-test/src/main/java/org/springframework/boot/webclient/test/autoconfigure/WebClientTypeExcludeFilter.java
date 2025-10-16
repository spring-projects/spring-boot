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
