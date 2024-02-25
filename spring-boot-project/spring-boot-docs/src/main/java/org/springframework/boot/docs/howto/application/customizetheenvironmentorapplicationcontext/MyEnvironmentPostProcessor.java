/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.howto.application.customizetheenvironmentorapplicationcontext;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * MyEnvironmentPostProcessor class.
 */
public class MyEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

	/**
	 * This method is used to post-process the environment of the application. It loads a
	 * YAML configuration file and adds it as a property source to the environment.
	 * @param environment The configurable environment of the application.
	 * @param application The Spring application.
	 */
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Resource path = new ClassPathResource("com/example/myapp/config.yml");
		PropertySource<?> propertySource = loadYaml(path);
		environment.getPropertySources().addLast(propertySource);
	}

	/**
	 * Loads a YAML property source from the given resource path.
	 * @param path the resource path to load the YAML configuration from
	 * @return the loaded YAML property source
	 * @throws IllegalStateException if failed to load the YAML configuration from the
	 * given resource path
	 * @throws IllegalArgumentException if the given resource path does not exist
	 */
	private PropertySource<?> loadYaml(Resource path) {
		Assert.isTrue(path.exists(), () -> "Resource " + path + " does not exist");
		try {
			return this.loader.load("custom-resource", path).get(0);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to load yaml configuration from " + path, ex);
		}
	}

}
