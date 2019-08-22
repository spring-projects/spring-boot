/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.test.context.assertj;

import java.util.function.Supplier;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * An {@link ApplicationContext} that additionally supports AssertJ style assertions. Can
 * be used to decorate an existing application context or an application context that
 * failed to start.
 * <p>
 * See {@link ApplicationContextAssertProvider} for more details.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @see ApplicationContextRunner
 * @see ApplicationContext
 */
public interface AssertableApplicationContext
		extends ApplicationContextAssertProvider<ConfigurableApplicationContext>, ConfigurableApplicationContext {

	/**
	 * Factory method to create a new {@link AssertableApplicationContext} instance.
	 * @param contextSupplier a supplier that will either return a fully configured
	 * {@link ConfigurableApplicationContext} or throw an exception if the context fails
	 * to start.
	 * @return an {@link AssertableApplicationContext} instance
	 */
	static AssertableApplicationContext get(Supplier<? extends ConfigurableApplicationContext> contextSupplier) {
		return ApplicationContextAssertProvider.get(AssertableApplicationContext.class,
				ConfigurableApplicationContext.class, contextSupplier);
	}

}
