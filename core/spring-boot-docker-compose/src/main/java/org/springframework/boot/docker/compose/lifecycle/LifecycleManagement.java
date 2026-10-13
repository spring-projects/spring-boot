/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.lifecycle;

/**
 * Docker Compose lifecycle management.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public enum LifecycleManagement {

	/**
	 * Don't start or stop Docker Compose.
	 */
	NONE(false, false),

	/**
	 * Start Docker Compose if it's not running.
	 */
	START_ONLY(true, false),

	/**
	 * Start Docker Compose if it's not running and stop it when the JVM exits.
	 */
	START_AND_STOP(true, true);

	private final boolean start;

	private final boolean stop;

	LifecycleManagement(boolean start, boolean stop) {
		this.start = start;
		this.stop = stop;
	}

	/**
	 * Return whether Docker Compose should be started.
	 * @return whether Docker Compose should be started
	 */
	boolean shouldStart() {
		return this.start;
	}

	/**
	 * Return whether Docker Compose should be stopped.
	 * @return whether Docker Compose should be stopped
	 */
	boolean shouldStop() {
		return this.stop;
	}

}
