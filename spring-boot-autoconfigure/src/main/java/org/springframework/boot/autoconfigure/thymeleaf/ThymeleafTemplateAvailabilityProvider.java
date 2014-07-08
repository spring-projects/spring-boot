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

package org.springframework.boot.autoconfigure.thymeleaf;

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.ClassUtils;

/**
 * {@link TemplateAvailabilityProvider} that provides availability information for
 * Thymeleaf view templates
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public class ThymeleafTemplateAvailabilityProvider implements
		TemplateAvailabilityProvider {

	@Override
	public boolean isTemplateAvailable(String view, Environment environment,
			ClassLoader classLoader, ResourceLoader resourceLoader) {
		if (ClassUtils.isPresent("org.thymeleaf.spring4.SpringTemplateEngine",
				classLoader)) {
			String prefix = environment.getProperty("spring.thymeleaf.prefix",
					ThymeleafAutoConfiguration.DEFAULT_PREFIX);
			String suffix = environment.getProperty("spring.thymeleaf.suffix",
					ThymeleafAutoConfiguration.DEFAULT_SUFFIX);
			return resourceLoader.getResource(prefix + view + suffix).exists();
		}

		return false;
	}

}
