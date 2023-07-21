/*
 * Copyright 2012-2021 the original author or authors.
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

/**
 * Interface that can be used to add or remove code that should run when the JVM is
 * shutdown. Shutdown handlers are similar to JVM {@link Runtime#addShutdownHook(Thread)
 * shutdown hooks} except that they run sequentially rather than concurrently.
 * <p>
 * Shutdown handlers are guaranteed to be called only after registered
 * {@link ApplicationContext} instances have been closed and are no longer active.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.5.1
 * @see SpringApplication#getShutdownHandlers()
 * @see SpringApplication#setRegisterShutdownHook(boolean)
 */
public interface SpringApplicationShutdownHandlers {

	/**
	 * Add an action to the handlers that will be run when the JVM exits.
	 * @param action the action to add
	 */
	void add(Runnable action);

	/**
	 * Remove a previously added an action so that it no longer runs when the JVM exits.
	 * @param action the action to remove
	 */
	void remove(Runnable action);

}
