/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.springframework.boot.bootstrap.DefaultBootstrapContext;
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
		DefaultBootstrapContext bootstrapContext = new DefaultBootstrapContext();
		ConfigDataEnvironmentPostProcessor.applyTo(environment, applicationContext, bootstrapContext);
		bootstrapContext.close(applicationContext);
		DefaultPropertiesPropertySource.moveToEnd(environment);
	}

}
