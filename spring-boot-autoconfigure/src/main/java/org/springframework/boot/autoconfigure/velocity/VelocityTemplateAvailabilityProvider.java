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

package org.springframework.boot.autoconfigure.velocity;

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
 * {@link TemplateAvailabilityProvider} that provides availability information for
 * Velocity view templates.
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 * @deprecated as of 1.4 following the deprecation of Velocity support in Spring Framework
 * 4.3
 */
@Deprecated
public class VelocityTemplateAvailabilityProvider
		implements TemplateAvailabilityProvider {

	@Override
	public boolean isTemplateAvailable(String view, Environment environment,
			ClassLoader classLoader, ResourceLoader resourceLoader) {
		if (ClassUtils.isPresent("org.apache.velocity.app.VelocityEngine", classLoader)) {
			VelocityTemplateAvailabilityProperties properties = new VelocityTemplateAvailabilityProperties();
			RelaxedDataBinder binder = new RelaxedDataBinder(properties,
					"spring.velocity");
			binder.bind(new PropertySourcesPropertyValues(
					((ConfigurableEnvironment) environment).getPropertySources()));
			for (String path : properties.getResourceLoaderPath()) {
				if (resourceLoader.getResource(
						path + properties.getPrefix() + view + properties.getSuffix())
						.exists()) {
					return true;
				}
			}
		}
		return false;
	}

	static class VelocityTemplateAvailabilityProperties {

		private List<String> resourceLoaderPath = new ArrayList<String>(
				Arrays.asList(VelocityProperties.DEFAULT_RESOURCE_LOADER_PATH));

		private String prefix = VelocityProperties.DEFAULT_PREFIX;

		private String suffix = VelocityProperties.DEFAULT_SUFFIX;

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
