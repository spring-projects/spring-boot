/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.groovy.template.autoconfigure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.autoconfigure.template.PathBasedTemplateAvailabilityProvider;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import org.springframework.util.ClassUtils;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for Groovy
 * view templates.
 *
 * @author Dave Syer
 * @since 4.0.0
 */
public class GroovyTemplateAvailabilityProvider extends PathBasedTemplateAvailabilityProvider {

	private static final String REQUIRED_CLASS_NAME = "groovy.text.TemplateEngine";

	public GroovyTemplateAvailabilityProvider() {
		super(REQUIRED_CLASS_NAME, GroovyTemplateAvailabilityProperties.class, "spring.groovy.template");
	}

	protected static final class GroovyTemplateAvailabilityProperties extends TemplateAvailabilityProperties {

		private List<String> resourceLoaderPath = new ArrayList<>(
				Arrays.asList(GroovyTemplateProperties.DEFAULT_RESOURCE_LOADER_PATH));

		GroovyTemplateAvailabilityProperties() {
			super(GroovyTemplateProperties.DEFAULT_PREFIX, GroovyTemplateProperties.DEFAULT_SUFFIX);
		}

		@Override
		protected List<String> getLoaderPath() {
			return this.resourceLoaderPath;
		}

		public List<String> getResourceLoaderPath() {
			return this.resourceLoaderPath;
		}

		public void setResourceLoaderPath(List<String> resourceLoaderPath) {
			this.resourceLoaderPath = resourceLoaderPath;
		}

	}

	static class GroovyTemplateAvailabilityRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			if (ClassUtils.isPresent(REQUIRED_CLASS_NAME, classLoader)) {
				BindableRuntimeHintsRegistrar.forTypes(GroovyTemplateAvailabilityProperties.class)
					.registerHints(hints, classLoader);
			}
		}

	}

}
