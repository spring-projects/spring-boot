/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.configuration;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * Configuration for how to connect to Docker.
 *
 * @author Phillip Webb
 * @since 3.5.0
 */
public sealed interface DockerConnectionConfiguration {

	/**
	 * Connect to specific host.
	 *
	 * @param address the host address
	 * @param secure if connection is secure
	 * @param certificatePath a path to the certificate used for secure connections
	 */
	record Host(String address, boolean secure,
			@Nullable String certificatePath) implements DockerConnectionConfiguration {

		public Host(String address) {
			this(address, false, null);
		}

		public Host {
			Assert.hasLength(address, "'address' must not be empty");
		}

	}

	/**
	 * Connect using a specific context reference.
	 *
	 * @param context a reference to the Docker context
	 */
	record Context(String context) implements DockerConnectionConfiguration {

		public Context {
			Assert.hasLength(context, "'context' must not be empty");
		}

	}

}
