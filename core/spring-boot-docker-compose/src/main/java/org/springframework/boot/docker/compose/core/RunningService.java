/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.core;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Provides details of a running Docker Compose service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public interface RunningService {

	/**
	 * Return the name of the service.
	 * @return the service name
	 */
	String name();

	/**
	 * Return the image being used by the service.
	 * @return the service image
	 */
	ImageReference image();

	/**
	 * Return the host that can be used to connect to the service.
	 * @return the service host
	 */
	String host();

	/**
	 * Return the ports that can be used to connect to the service.
	 * @return the service ports
	 */
	ConnectionPorts ports();

	/**
	 * Return the environment defined for the service.
	 * @return the service env
	 */
	Map<String, @Nullable String> env();

	/**
	 * Return the labels attached to the service.
	 * @return the service labels
	 */
	Map<String, String> labels();

	/**
	 * Return the Docker Compose file for the service.
	 * @return the Docker Compose file
	 * @since 3.5.0
	 */
	default @Nullable DockerComposeFile composeFile() {
		return null;
	}

}
