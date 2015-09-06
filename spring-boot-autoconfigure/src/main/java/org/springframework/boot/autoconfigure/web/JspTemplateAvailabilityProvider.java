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

package org.springframework.boot.autoconfigure.web;

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for JSP
 * view templates.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 1.1.0
 */
public class JspTemplateAvailabilityProvider implements TemplateAvailabilityProvider {

	@Override
	public boolean isTemplateAvailable(String view, Environment environment,
			ClassLoader classLoader, ResourceLoader resourceLoader) {
		if (ClassUtils.isPresent("org.apache.jasper.compiler.JspConfig", classLoader)) {
			String prefix = getProperty(environment, "spring.mvc.view.prefix",
					"spring.view.prefix", WebMvcAutoConfiguration.DEFAULT_PREFIX);
			String suffix = getProperty(environment, "spring.mvc.view.suffix",
					"spring.view.suffix", WebMvcAutoConfiguration.DEFAULT_SUFFIX);
			return resourceLoader.getResource(prefix + view + suffix).exists();
		}
		return false;
	}

	private String getProperty(Environment environment, String key, String deprecatedKey,
			String defaultValue) {
		if (environment.containsProperty(key)) {
			return environment.getProperty(key);
		}
		return environment.getProperty(deprecatedKey, defaultValue);
	}

}
