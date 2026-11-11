/*
 * Copyright 2012-present the original author or authors.
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
