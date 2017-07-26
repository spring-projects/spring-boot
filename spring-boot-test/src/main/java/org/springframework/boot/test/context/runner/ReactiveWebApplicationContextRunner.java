/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.context.runner;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.boot.context.annotation.Configurations;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.reactive.context.ConfigurableReactiveWebApplicationContext;
import org.springframework.boot.web.reactive.context.GenericReactiveWebApplicationContext;
import org.springframework.context.ApplicationContext;

/**
 * A {@link AbstractApplicationContextRunner ApplicationContext runner} for a
 * {@link ConfigurableReactiveWebApplicationContext}.
 * <p>
 * See {@link AbstractApplicationContextRunner} for details.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class ReactiveWebApplicationContextRunner extends
		AbstractApplicationContextRunner<ReactiveWebApplicationContextRunner, ConfigurableReactiveWebApplicationContext, AssertableReactiveWebApplicationContext> {

	/**
	 * Create a new {@link ReactiveWebApplicationContextRunner} instance using a
	 * {@link GenericReactiveWebApplicationContext} as the underlying source.
	 */
	public ReactiveWebApplicationContextRunner() {
		this(GenericReactiveWebApplicationContext::new);
	}

	/**
	 * Create a new {@link ApplicationContextRunner} instance using the specified
	 * {@code contextFactory} as the underlying source.
	 * @param contextFactory a supplier that returns a new instance on each call
	 */
	public ReactiveWebApplicationContextRunner(
			Supplier<ConfigurableReactiveWebApplicationContext> contextFactory) {
		super(contextFactory);
	}

	private ReactiveWebApplicationContextRunner(
			Supplier<ConfigurableReactiveWebApplicationContext> contextFactory,
			TestPropertyValues environmentProperties, TestPropertyValues systemProperties,
			ClassLoader classLoader, ApplicationContext parent,
			List<Configurations> configurations) {
		super(contextFactory, environmentProperties, systemProperties, classLoader,
				parent, configurations);
	}

	@Override
	protected ReactiveWebApplicationContextRunner newInstance(
			Supplier<ConfigurableReactiveWebApplicationContext> contextFactory,
			TestPropertyValues environmentProperties, TestPropertyValues systemProperties,
			ClassLoader classLoader, ApplicationContext parent,
			List<Configurations> configurations) {
		return new ReactiveWebApplicationContextRunner(contextFactory,
				environmentProperties, systemProperties, classLoader, parent,
				configurations);
	}

}
