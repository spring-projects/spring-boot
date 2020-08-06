/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.test.context;

import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.DefaultPropertiesPropertySource;
import org.springframework.boot.env.RandomValuePropertySource;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ContextConfiguration;

/**
 * {@link ApplicationContextInitializer} that can be used with the
 * {@link ContextConfiguration#initializers()} to trigger loading of {@link ConfigData}
 * such as {@literal application.properties}.
 *
 * @author Phillip Webb
 * @since 2.4.0
 * @see ConfigDataEnvironmentPostProcessor
 */
public class ConfigDataApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		ConfigurableEnvironment environment = applicationContext.getEnvironment();
		RandomValuePropertySource.addToEnvironment(environment);
		ConfigDataEnvironmentPostProcessor.applyTo(environment, applicationContext);
		DefaultPropertiesPropertySource.moveToEnd(environment);
	}

}
