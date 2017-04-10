/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for Groovy
 * view templates.
 *
 * @author Dave Syer
 * @since 1.1.0
 */
public class GroovyTemplateAvailabilityProvider implements TemplateAvailabilityProvider {

	@Override
	public boolean isTemplateAvailable(String view, Environment environment,
			ClassLoader classLoader, ResourceLoader resourceLoader) {
		if (ClassUtils.isPresent("groovy.text.TemplateEngine", classLoader)) {
			GroovyTemplateAvailabilityProperties properties = new GroovyTemplateAvailabilityProperties();
			RelaxedDataBinder binder = new RelaxedDataBinder(properties,
					"spring.groovy.template");
			binder.bind(new PropertySourcesPropertyValues(
					((ConfigurableEnvironment) environment).getPropertySources()));
			for (String loaderPath : properties.getResourceLoaderPath()) {
				if (resourceLoader.getResource(loaderPath + properties.getPrefix() + view
						+ properties.getSuffix()).exists()) {
					return true;
				}
			}
		}
		return false;
	}

	static final class GroovyTemplateAvailabilityProperties {

		private List<String> resourceLoaderPath = new ArrayList<String>(
				Arrays.asList(GroovyTemplateProperties.DEFAULT_RESOURCE_LOADER_PATH));

		private String prefix = GroovyTemplateProperties.DEFAULT_PREFIX;

		private String suffix = GroovyTemplateProperties.DEFAULT_SUFFIX;

		public List<String> getResourceLoaderPath() {
			return this.resourceLoaderPath;
		}

		public void setResourceLoaderPath(List<String> resourceLoaderPath) {
			this.resourceLoaderPath = resourceLoaderPath;
		}

		public String getPrefix() {
			return this.prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		public String getSuffix() {
			return this.suffix;
		}

		public void setSuffix(String suffix) {
			this.suffix = suffix;
		}

	}

}
