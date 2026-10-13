/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context.filter;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * {@link ApplicationContextInitializer} to register the {@link TestTypeExcludeFilter} for
 * when {@link SpringApplication#from} is being used with the test classpath.
 *
 * @author Phillip Webb
 */
class ExcludeFilterApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		TestTypeExcludeFilter.registerWith(applicationContext.getBeanFactory());
	}

}
