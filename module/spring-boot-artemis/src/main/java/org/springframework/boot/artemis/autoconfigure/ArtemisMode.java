/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.artemis.autoconfigure;

/**
 * Define the mode in which Artemis can operate.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public enum ArtemisMode {

	/**
	 * Connect to a broker using the native Artemis protocol (i.e. netty).
	 */
	NATIVE,

	/**
	 * Embed (i.e. start) the broker in the application.
	 */
	EMBEDDED

}
