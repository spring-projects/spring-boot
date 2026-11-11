/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.context.reactive;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;

/**
 * Subclass of {@link GenericApplicationContext}, suitable for reactive web environments.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 2.0.0
 */
public class GenericReactiveWebApplicationContext extends GenericApplicationContext
		implements ConfigurableReactiveWebApplicationContext {

	/**
	 * Create a new {@link GenericReactiveWebApplicationContext}.
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericReactiveWebApplicationContext() {
	}

	/**
	 * Create a new {@link GenericReactiveWebApplicationContext} with the given
	 * DefaultListableBeanFactory.
	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
	 * @see #registerBeanDefinition
	 * @see #refresh
	 */
	public GenericReactiveWebApplicationContext(DefaultListableBeanFactory beanFactory) {
		super(beanFactory);
	}

	@Override
	protected ConfigurableEnvironment createEnvironment() {
		return new StandardReactiveWebEnvironment();
	}

	@Override
	protected Resource getResourceByPath(String path) {
		// We must be careful not to expose classpath resources
		return new FilteredReactiveWebContextResource(path);
	}

}
