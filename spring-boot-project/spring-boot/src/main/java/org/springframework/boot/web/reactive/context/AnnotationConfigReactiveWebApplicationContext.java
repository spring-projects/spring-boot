/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.web.reactive.context;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * {@link ConfigurableReactiveWebApplicationContext} that accepts annotated classes as
 * input - in particular {@link Configuration @Configuration}-annotated classes, but also
 * plain {@link Component @Component} classes and JSR-330 compliant classes using
 * {@code javax.inject} annotations. Allows for registering classes one by one (specifying
 * class names as config location) as well as for classpath scanning (specifying base
 * packages as config location).
 * <p>
 * Note: In case of multiple {@code @Configuration} classes, later {@code @Bean}
 * definitions will override ones defined in earlier loaded files. This can be leveraged
 * to deliberately override certain bean definitions through an extra Configuration class.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 2.0.0
 * @see AnnotationConfigApplicationContext
 */
public class AnnotationConfigReactiveWebApplicationContext extends AnnotationConfigApplicationContext
		implements ConfigurableReactiveWebApplicationContext {

	/**
	 * Create a new AnnotationConfigReactiveWebApplicationContext that needs to be
	 * populated through {@link #register} calls and then manually {@linkplain #refresh
	 * refreshed}.
	 */
	public AnnotationConfigReactiveWebApplicationContext() {
	}

	/**
	 * Create a new AnnotationConfigApplicationContext with the given
	 * DefaultListableBeanFactory.
	 * @param beanFactory the DefaultListableBeanFactory instance to use for this context
	 * @since 2.2.0
	 */
	public AnnotationConfigReactiveWebApplicationContext(DefaultListableBeanFactory beanFactory) {
		super(beanFactory);
	}

	/**
	 * Create a new AnnotationConfigApplicationContext, deriving bean definitions from the
	 * given annotated classes and automatically refreshing the context.
	 * @param annotatedClasses one or more annotated classes, e.g.
	 * {@link Configuration @Configuration} classes
	 * @since 2.2.0
	 */
	public AnnotationConfigReactiveWebApplicationContext(Class<?>... annotatedClasses) {
		super(annotatedClasses);
	}

	/**
	 * Create a new AnnotationConfigApplicationContext, scanning for bean definitions in
	 * the given packages and automatically refreshing the context.
	 * @param basePackages the packages to check for annotated classes
	 * @since 2.2.0
	 */
	public AnnotationConfigReactiveWebApplicationContext(String... basePackages) {
		super(basePackages);
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
