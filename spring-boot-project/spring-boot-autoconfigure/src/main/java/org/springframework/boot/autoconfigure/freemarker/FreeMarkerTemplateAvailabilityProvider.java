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

package org.springframework.boot.autoconfigure.freemarker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.boot.autoconfigure.template.PathBasedTemplateAvailabilityProvider;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.context.properties.bind.BindableRuntimeHintsRegistrar;
import org.springframework.util.ClassUtils;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for
 * FreeMarker view templates.
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public class FreeMarkerTemplateAvailabilityProvider extends PathBasedTemplateAvailabilityProvider {

	private static final String REQUIRED_CLASS_NAME = "freemarker.template.Configuration";

	/**
	 * Constructs a new {@code FreeMarkerTemplateAvailabilityProvider} with the specified
	 * required class name, properties class, and properties prefix.
	 * @param requiredClassName the name of the required class
	 * @param propertiesClass the class representing the properties
	 * @param propertiesPrefix the prefix for the properties
	 */
	public FreeMarkerTemplateAvailabilityProvider() {
		super(REQUIRED_CLASS_NAME, FreeMarkerTemplateAvailabilityProperties.class, "spring.freemarker");
	}

	/**
	 * FreeMarkerTemplateAvailabilityProperties class.
	 */
	protected static final class FreeMarkerTemplateAvailabilityProperties extends TemplateAvailabilityProperties {

		private List<String> templateLoaderPath = new ArrayList<>(
				Arrays.asList(FreeMarkerProperties.DEFAULT_TEMPLATE_LOADER_PATH));

		/**
		 * Constructs a new {@code FreeMarkerTemplateAvailabilityProperties} with the
		 * default prefix and suffix.
		 */
		FreeMarkerTemplateAvailabilityProperties() {
			super(FreeMarkerProperties.DEFAULT_PREFIX, FreeMarkerProperties.DEFAULT_SUFFIX);
		}

		/**
		 * Returns the list of paths to be used by the loader for loading templates.
		 * @return the list of paths to be used by the loader for loading templates
		 */
		@Override
		protected List<String> getLoaderPath() {
			return this.templateLoaderPath;
		}

		/**
		 * Returns the list of template loader paths.
		 * @return the list of template loader paths
		 */
		public List<String> getTemplateLoaderPath() {
			return this.templateLoaderPath;
		}

		/**
		 * Sets the template loader path for FreeMarker templates.
		 * @param templateLoaderPath the list of template loader paths to be set
		 */
		public void setTemplateLoaderPath(List<String> templateLoaderPath) {
			this.templateLoaderPath = templateLoaderPath;
		}

	}

	/**
	 * FreeMarkerTemplateAvailabilityRuntimeHints class.
	 */
	static class FreeMarkerTemplateAvailabilityRuntimeHints extends BindableRuntimeHintsRegistrar {

		/**
		 * Constructs a new instance of the
		 * {@code FreeMarkerTemplateAvailabilityRuntimeHints} class.
		 * @param properties the {@code FreeMarkerTemplateAvailabilityProperties} object
		 * to use for initialization
		 */
		FreeMarkerTemplateAvailabilityRuntimeHints() {
			super(FreeMarkerTemplateAvailabilityProperties.class);
		}

		/**
		 * Registers the runtime hints for FreeMarker template availability.
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
