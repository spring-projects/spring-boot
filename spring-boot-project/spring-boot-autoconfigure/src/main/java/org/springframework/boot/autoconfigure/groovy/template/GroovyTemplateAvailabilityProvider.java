/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.groovy.template;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.boot.autoconfigure.template.PathBasedTemplateAvailabilityProvider;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import org.springframework.util.ClassUtils;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for Groovy
 * view templates.
 *
 * @author Dave Syer
 * @since 1.1.0
 */
public class GroovyTemplateAvailabilityProvider extends PathBasedTemplateAvailabilityProvider {

	private static final String REQUIRED_CLASS_NAME = "groovy.text.TemplateEngine";

	/**
	 * Constructs a new GroovyTemplateAvailabilityProvider.
	 * @param requiredClassName the name of the required class
	 * @param propertiesClass the class representing the properties for
	 * GroovyTemplateAvailability
	 * @param prefix the prefix for the GroovyTemplateAvailability properties
	 */
	public GroovyTemplateAvailabilityProvider() {
		super(REQUIRED_CLASS_NAME, GroovyTemplateAvailabilityProperties.class, "spring.groovy.template");
	}

	/**
	 * GroovyTemplateAvailabilityProperties class.
	 */
	protected static final class GroovyTemplateAvailabilityProperties extends TemplateAvailabilityProperties {

		private List<String> resourceLoaderPath = new ArrayList<>(
				Arrays.asList(GroovyTemplateProperties.DEFAULT_RESOURCE_LOADER_PATH));

		/**
		 * Constructs a new instance of GroovyTemplateAvailabilityProperties with the
		 * default prefix and suffix.
		 */
		GroovyTemplateAvailabilityProperties() {
			super(GroovyTemplateProperties.DEFAULT_PREFIX, GroovyTemplateProperties.DEFAULT_SUFFIX);
		}

		/**
		 * Returns the loader path for the Groovy template availability.
		 * @return the loader path for the Groovy template availability
		 */
		@Override
		protected List<String> getLoaderPath() {
			return this.resourceLoaderPath;
		}

		/**
		 * Returns the resource loader path.
		 * @return the resource loader path as a List of Strings
		 */
		public List<String> getResourceLoaderPath() {
			return this.resourceLoaderPath;
		}

		/**
		 * Sets the resource loader path for the Groovy template availability properties.
		 * @param resourceLoaderPath the list of resource loader paths to be set
		 */
		public void setResourceLoaderPath(List<String> resourceLoaderPath) {
			this.resourceLoaderPath = resourceLoaderPath;
		}

	}

	/**
	 * GroovyTemplateAvailabilityRuntimeHints class.
	 */
	static class GroovyTemplateAvailabilityRuntimeHints extends BindableRuntimeHintsRegistrar {

		/**
		 * Constructs a new instance of GroovyTemplateAvailabilityRuntimeHints.
		 * @param properties the GroovyTemplateAvailabilityProperties object to be used
		 * @see GroovyTemplateAvailabilityProperties
		 */
		GroovyTemplateAvailabilityRuntimeHints() {
			super(GroovyTemplateAvailabilityProperties.class);
		}

		/**
		 * Registers the runtime hints for the Groovy template availability.
		 * @param hints the runtime hints to register
		 * @param classLoader the class loader to use for checking the presence of a
		 * required class
		 */
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			if (ClassUtils.isPresent(REQUIRED_CLASS_NAME, classLoader)) {
				super.registerHints(hints, classLoader);
			}
		}

	}

}
