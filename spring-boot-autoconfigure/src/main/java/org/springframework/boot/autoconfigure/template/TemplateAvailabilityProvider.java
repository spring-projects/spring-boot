/*
 * Copyright 2012-2015 the original author or authors.
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

import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;

/**
 * Indicates the availability of view templates for a particular templating engine such as
 * FreeMarker or Thymeleaf.
 *
 * @author Andy Wilkinson
 * @since 1.1.0
 */
public interface TemplateAvailabilityProvider {

	/**
	 * Returns {@code true} if a template is available for the given {@code view}.
	 * @param view the view name
	 * @param environment the environment
	 * @param classLoader the class loader
	 * @param resourceLoader the resource loader
	 * @return if the template is available
	 */
	boolean isTemplateAvailable(String view, Environment environment,
			ClassLoader classLoader, ResourceLoader resourceLoader);

}
