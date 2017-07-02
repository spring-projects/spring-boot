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

package org.springframework.boot.test.context;

import org.springframework.context.ConfigurableApplicationContext;

/**
 * Callback interface used in tests to process a running
 * {@link ConfigurableApplicationContext} with the ability to throw a (checked) exception.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 2.0.0
 * @param <T> the type of the context that can be consumed
 */
@FunctionalInterface
public interface ContextConsumer<T extends ConfigurableApplicationContext> {

	/**
	 * Performs this operation on the supplied {@code context}.
	 * @param context the application context to consume
	 * @throws Throwable any exception that might occur in assertions
	 */
	void accept(T context) throws Throwable;

}
