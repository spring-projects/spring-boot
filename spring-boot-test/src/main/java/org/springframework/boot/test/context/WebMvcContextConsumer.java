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

import org.springframework.web.context.ConfigurableWebApplicationContext;

/**
 * Specialized callback interface used in tests to process a running
 * {@link ConfigurableWebApplicationContext} with the ability to throw a (checked)
 * exception.
 *
 * @author Stephane Nicoll
 * @see ContextConsumer
 * @since 2.0.0
 */
@FunctionalInterface
public interface WebMvcContextConsumer {

	/**
	 * Performs this operation on the supplied {@code context}.
	 * @param context the application context to consume
	 * @throws Throwable any exception that might occur in assertions
	 */
	void accept(ConfigurableWebApplicationContext context) throws Throwable;

}
