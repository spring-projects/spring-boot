/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.cli.app;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * A launcher for {@code SpringApplication} or a {@code SpringApplication} subclass. The
 * class that is used can be configured using the System property
 * {@code spring.application.class.name} or the {@code SPRING_APPLICATION_CLASS_NAME}
 * environment variable. Uses reflection to allow the launching code to exist in a
 * separate ClassLoader from the application code.
 *
 * @author Andy Wilkinson
 * @since 1.2.0
 * @see System#getProperty(String)
 * @see System#getenv(String)
 */
public class SpringApplicationLauncher {

	private static final String DEFAULT_SPRING_APPLICATION_CLASS = "org.springframework.boot.SpringApplication";

	private final ClassLoader classLoader;

	/**
	 * Creates a new launcher that will use the given {@code classLoader} to load the
	 * configured {@code SpringApplication} class.
	 * @param classLoader the {@code ClassLoader} to use
	 */
	public SpringApplicationLauncher(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

	/**
	 * Launches the application created using the given {@code sources}. The application
	 * is launched with the given {@code args}.
	 * @param sources the sources for the application
	 * @param args the args for the application
	 * @return the application's {@code ApplicationContext}
	 * @throws Exception if the launch fails
	 */
	public Object launch(Class<?>[] sources, String[] args) throws Exception {
		Map<String, Object> defaultProperties = new HashMap<>();
		defaultProperties.put("spring.groovy.template.check-template-location", "false");
		Class<?> applicationClass = this.classLoader
				.loadClass(getSpringApplicationClassName());
		Constructor<?> constructor = applicationClass.getConstructor(Class[].class);
		Object application = constructor.newInstance((Object) sources);
		applicationClass.getMethod("setDefaultProperties", Map.class).invoke(application,
				defaultProperties);
		Method method = applicationClass.getMethod("run", String[].class);
		return method.invoke(application, (Object) args);
	}

	private String getSpringApplicationClassName() {
		String className = System.getProperty("spring.application.class.name");
		if (className == null) {
			className = getEnvironmentVariable("SPRING_APPLICATION_CLASS_NAME");
		}
		if (className == null) {
			className = DEFAULT_SPRING_APPLICATION_CLASS;
		}
		return className;
	}

	protected String getEnvironmentVariable(String name) {
		return System.getenv(name);
	}

}
