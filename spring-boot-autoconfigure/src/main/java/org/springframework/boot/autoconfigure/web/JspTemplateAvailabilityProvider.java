/*
 * Copyright 2012-2014 the original author or authors.
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

import org.springframework.boot.web.TemplateAvailabilityProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

public class JspTemplateAvailabilityProvider implements TemplateAvailabilityProvider {

	@Override
	public boolean isTemplateAvailable(String view, Environment environment,
			ClassLoader classLoader, ResourceLoader resourceLoader) {
		if (ClassUtils.isPresent("org.apache.jasper.compiler.JspConfig", classLoader)) {
			String prefix = environment.getProperty("spring.view.prefix",
					WebMvcAutoConfiguration.DEFAULT_PREFIX);
			String suffix = environment.getProperty("spring.view.suffix",
					WebMvcAutoConfiguration.DEFAULT_SUFFIX);
			return resourceLoader.getResource(prefix + view + suffix).exists();
		}
		return false;
	}

}
