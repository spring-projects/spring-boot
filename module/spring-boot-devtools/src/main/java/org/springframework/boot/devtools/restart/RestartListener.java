/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.restart;

/**
 * Listener that is notified of application restarts.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 */
@FunctionalInterface
public interface RestartListener {

	/**
	 * Called before an application restart.
	 */
	void beforeRestart();

}
