/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testcontainers.lifecycle;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import org.testcontainers.lifecycle.Startable;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * {@link ApplicationContextInitializer} to manage the lifecycle of {@link Startable
 * startable containers}.
 *
 * @author Phillip Webb
 * @since 3.1.0
 */
public class TestcontainersLifecycleApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	private static final Set<ConfigurableApplicationContext> applied = Collections.newSetFromMap(new WeakHashMap<>());

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		synchronized (applied) {
			if (!applied.add(applicationContext)) {
				return;
			}
		}
		ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
		applicationContext.addBeanFactoryPostProcessor(new TestcontainersLifecycleBeanFactoryPostProcessor());
		TestcontainersStartup startup = TestcontainersStartup.get(applicationContext.getEnvironment());
		TestcontainersLifecycleBeanPostProcessor beanPostProcessor = new TestcontainersLifecycleBeanPostProcessor(
				beanFactory, startup);
		beanFactory.addBeanPostProcessor(beanPostProcessor);
	}

}
