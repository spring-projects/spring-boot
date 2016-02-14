/*
 * Copyright 2012-2015 the original author or authors.
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

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for
 * Velocity view templates.
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public class VelocityTemplateAvailabilityProvider
		implements TemplateAvailabilityProvider {

	@Override
	public boolean isTemplateAvailable(String view, Environment environment,
			ClassLoader classLoader, ResourceLoader resourceLoader) {
		if (ClassUtils.isPresent("org.apache.velocity.app.VelocityEngine", classLoader)) {
			PropertyResolver resolver = new RelaxedPropertyResolver(environment,
					"spring.velocity.");
			String loaderPath = resolver.getProperty("resource-loader-path",
					VelocityProperties.DEFAULT_RESOURCE_LOADER_PATH);
			String prefix = resolver.getProperty("prefix",
					VelocityProperties.DEFAULT_PREFIX);
			String suffix = resolver.getProperty("suffix",
					VelocityProperties.DEFAULT_SUFFIX);
			return resourceLoader.getResource(loaderPath + prefix + view + suffix)
					.exists();
		}
		return false;
	}

}
