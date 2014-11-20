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

package org.springframework.boot.cli.app;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * A launcher for {@code SpringApplication}. Uses reflection to allow the launching code
 * to exist in a separate ClassLoader from the application code.
 *
 * @author Andy Wilkinson
 * @since 1.2.0
 */
public class SpringApplicationLauncher {

	private static final String SPRING_APPLICATION_CLASS = "org.springframework.boot.SpringApplication";

	private final ClassLoader classLoader;

	/**
	 * Creates a new launcher that will use the given {@code classLoader} to load
	 * {@code SpringApplication}.
	 * @param classLoader the {@code ClassLoader} to use
	 */
	public SpringApplicationLauncher(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Launches the application created using the given {@code sources}. The application
	 * is launched with the given {@code args}.
	 * @param sources The sources for the application
	 * @param args The args for the application
	 * @return The application's {@code ApplicationContext}
	 * @throws Exception if the launch fails
	 */
	public Object launch(Object[] sources, String[] args) throws Exception {
		Map<String, Object> defaultProperties = new HashMap<String, Object>();
		defaultProperties.put("spring.groovy.template.check-template-location", "false");
		Class<?> applicationClass = this.classLoader.loadClass(SPRING_APPLICATION_CLASS);
		Constructor<?> constructor = applicationClass.getConstructor(Object[].class);
		Object application = constructor.newInstance((Object) sources);
		applicationClass.getMethod("setDefaultProperties", Map.class).invoke(application,
				defaultProperties);
		Method method = applicationClass.getMethod("run", String[].class);
		return method.invoke(application, (Object) args);
	}

}
