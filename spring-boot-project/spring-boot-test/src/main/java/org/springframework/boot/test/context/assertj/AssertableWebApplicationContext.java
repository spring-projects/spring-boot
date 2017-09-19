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

package org.springframework.boot.test.context.assertj;

import java.util.function.Supplier;

import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;

/**
 * A {@link WebApplicationContext} that additionally supports AssertJ style assertions.
 * Can be used to decorate and existing servlet web application context or an application
 * context that failed to start.
 * <p>
 * See {@link ApplicationContextAssertProvider} for more details.
 *
 * @author Phillip Webb
 * @since 2.0.0
 * @see WebApplicationContextRunner
 * @see WebApplicationContext
 */
public interface AssertableWebApplicationContext
		extends ApplicationContextAssertProvider<ConfigurableWebApplicationContext>,
		WebApplicationContext {

	/**
	 * Factory method to create a new {@link AssertableWebApplicationContext} instance.
	 * @param contextSupplier a supplier that will either return a fully configured
	 * {@link ConfigurableWebApplicationContext} or throw an exception if the context
	 * fails to start.
	 * @return a {@link AssertableWebApplicationContext} instance
	 */
	static AssertableWebApplicationContext get(
			Supplier<? extends ConfigurableWebApplicationContext> contextSupplier) {
		return ApplicationContextAssertProvider.get(AssertableWebApplicationContext.class,
				ConfigurableWebApplicationContext.class, contextSupplier);
	}

}
