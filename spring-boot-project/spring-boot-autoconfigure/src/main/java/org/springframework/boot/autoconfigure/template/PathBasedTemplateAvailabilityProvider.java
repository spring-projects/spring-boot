/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.template;

import java.util.List;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

/**
 * Abstract base class for {@link TemplateAvailabilityProvider} implementations that find
 * templates from paths.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 1.4.6
 */
public abstract class PathBasedTemplateAvailabilityProvider implements TemplateAvailabilityProvider {

	private final String className;

	private final Class<TemplateAvailabilityProperties> propertiesClass;

	private final String propertyPrefix;

	/**
     * Constructs a new PathBasedTemplateAvailabilityProvider with the specified parameters.
     * 
     * @param className the fully qualified class name of the provider
     * @param propertiesClass the class of the template availability properties
     * @param propertyPrefix the prefix for the properties
     */
    @SuppressWarnings("unchecked")
	public PathBasedTemplateAvailabilityProvider(String className,
			Class<? extends TemplateAvailabilityProperties> propertiesClass, String propertyPrefix) {
		this.className = className;
		this.propertiesClass = (Class<TemplateAvailabilityProperties>) propertiesClass;
		this.propertyPrefix = propertyPrefix;
	}

	/**
     * Checks if a template is available for the given view.
     * 
     * @param view the name of the view
     * @param environment the environment in which the template is being checked
     * @param classLoader the class loader to use for checking the presence of the template class
     * @param resourceLoader the resource loader to use for loading the template resource
     * @return true if the template is available, false otherwise
     */
    @Override
	public boolean isTemplateAvailable(String view, Environment environment, ClassLoader classLoader,
			ResourceLoader resourceLoader) {
		if (ClassUtils.isPresent(this.className, classLoader)) {
			Binder binder = Binder.get(environment);
			TemplateAvailabilityProperties properties = binder.bindOrCreate(this.propertyPrefix, this.propertiesClass);
			return isTemplateAvailable(view, resourceLoader, properties);
		}
		return false;
	}

	/**
     * Checks if a template is available for the given view.
     * 
     * @param view the view name
     * @param resourceLoader the resource loader
     * @param properties the template availability properties
     * @return true if the template is available, false otherwise
     */
    private boolean isTemplateAvailable(String view, ResourceLoader resourceLoader,
			TemplateAvailabilityProperties properties) {
		String location = properties.getPrefix() + view + properties.getSuffix();
		for (String path : properties.getLoaderPath()) {
			if (resourceLoader.getResource(path + location).exists()) {
				return true;
			}
		}
		return false;
	}

	/**
     * TemplateAvailabilityProperties class.
     */
    protected abstract static class TemplateAvailabilityProperties {

		private String prefix;

		private String suffix;

		/**
         * Constructs a new TemplateAvailabilityProperties object with the specified prefix and suffix.
         * 
         * @param prefix the prefix to be used for template availability checks
         * @param suffix the suffix to be used for template availability checks
         */
        protected TemplateAvailabilityProperties(String prefix, String suffix) {
			this.prefix = prefix;
			this.suffix = suffix;
		}

		/**
         * Returns the list of paths to be used for loading templates.
         *
         * @return the list of paths to be used for loading templates
         */
        protected abstract List<String> getLoaderPath();

		/**
         * Returns the prefix used for template availability checks.
         * 
         * @return the prefix used for template availability checks
         */
        public String getPrefix() {
			return this.prefix;
		}

		/**
         * Sets the prefix for template availability properties.
         * 
         * @param prefix the prefix to be set
         */
        public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		/**
         * Returns the suffix used to resolve templates.
         * 
         * @return the suffix used to resolve templates
         */
        public String getSuffix() {
			return this.suffix;
		}

		/**
         * Sets the suffix for template availability properties.
         * 
         * @param suffix the suffix to be set
         */
        public void setSuffix(String suffix) {
			this.suffix = suffix;
		}

	}

}
