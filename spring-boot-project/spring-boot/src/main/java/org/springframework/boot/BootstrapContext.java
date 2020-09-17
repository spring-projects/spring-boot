/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * A simple bootstrap context that is available during startup and {@link Environment}
 * post-processing up to the point that the {@link ApplicationContext} is prepared.
 * <p>
 * Provides lazy access to singletons that may be expensive to create, or need to be
 * shared before the {@link ApplicationContext} is available.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public interface BootstrapContext {

	/**
	 * Return an instance from the context, creating it if it hasn't been accessed
	 * previously.
	 * @param <T> the instance type
	 * @param type the instance type
	 * @return the instance managed by the context
	 * @throws IllegalStateException if the type has not been registered
	 */
	<T> T get(Class<T> type) throws IllegalStateException;

}
