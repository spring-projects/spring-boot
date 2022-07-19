/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.servlet.context;

import org.springframework.aot.AotDetector;
import org.springframework.boot.ApplicationContextFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * {@link ApplicationContextFactory} registered in {@code spring.factories} to support
 * {@link AnnotationConfigServletWebServerApplicationContext} and
 * {@link ServletWebServerApplicationContext}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ServletWebServerApplicationContextFactory implements ApplicationContextFactory {

	@Override
	public ConfigurableApplicationContext create(WebApplicationType webApplicationType) {
		if (webApplicationType != WebApplicationType.SERVLET) {
			return null;
		}
		return AotDetector.useGeneratedArtifacts() ? new ServletWebServerApplicationContext()
				: new AnnotationConfigServletWebServerApplicationContext();
	}

}
