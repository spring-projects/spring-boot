/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.web.reactive.context;

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
