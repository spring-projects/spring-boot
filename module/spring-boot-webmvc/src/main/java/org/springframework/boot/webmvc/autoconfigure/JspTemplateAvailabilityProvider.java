/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.webmvc.autoconfigure;

import java.io.File;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

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
 * @author Madhura Bhave
 * @since 4.0.0
 */
public class JspTemplateAvailabilityProvider implements TemplateAvailabilityProvider {

	private static final String WAR_SOURCE_DIRECTORY_ENVIRONMENT_VARIABLE = "WAR_SOURCE_DIRECTORY";

	private final File warSourceDirectory;

	public JspTemplateAvailabilityProvider() {
		this(new File("."), System::getenv);
	}

	JspTemplateAvailabilityProvider(File rootDirectory, Function<String, @Nullable String> systemEnvironment) {
		this.warSourceDirectory = new File(rootDirectory, getWarSourceDirectory(systemEnvironment));
	}

	private static String getWarSourceDirectory(Function<String, @Nullable String> systemEnvironment) {
		String name = systemEnvironment.apply(WAR_SOURCE_DIRECTORY_ENVIRONMENT_VARIABLE);
		return (name != null) ? name : "src/main/webapp";
	}

	@Override
	public boolean isTemplateAvailable(String view, Environment environment, ClassLoader classLoader,
			ResourceLoader resourceLoader) {
		if (ClassUtils.isPresent("org.apache.jasper.compiler.JspConfig", classLoader)) {
			String resourceName = getResourceName(view, environment);
			if (resourceLoader.getResource(resourceName).exists()) {
				return true;
			}
			return new File(this.warSourceDirectory, resourceName).exists();
		}
		return false;
	}

	private String getResourceName(String view, Environment environment) {
		String prefix = environment.getProperty("spring.mvc.view.prefix", WebMvcAutoConfiguration.DEFAULT_PREFIX);
		String suffix = environment.getProperty("spring.mvc.view.suffix", WebMvcAutoConfiguration.DEFAULT_SUFFIX);
		return prefix + view + suffix;
	}

}
