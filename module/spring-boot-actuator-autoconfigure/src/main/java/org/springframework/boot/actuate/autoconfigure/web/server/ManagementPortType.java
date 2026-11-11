/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.web.server;

import org.jspecify.annotations.Nullable;

import org.springframework.core.env.Environment;

/**
 * Port types that can be used to control how the management server is started.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public enum ManagementPortType {

	/**
	 * The management port has been disabled.
	 */
	DISABLED,

	/**
	 * The management port is the same as the server port.
	 */
	SAME,

	/**
	 * The management port and server port are different.
	 */
	DIFFERENT;

	/**
	 * Look at the given environment to determine if the {@link ManagementPortType} is
	 * {@link #DISABLED}, {@link #SAME} or {@link #DIFFERENT}.
	 * @param environment the Spring environment
	 * @return {@link #DISABLED} if {@code management.server.port} is set to a negative
	 * value, {@link #SAME} if {@code management.server.port} is not specified or equal to
	 * {@code server.port} and {@link #DIFFERENT} otherwise.
	 * @since 2.1.4
	 */
	public static ManagementPortType get(Environment environment) {
		Integer managementPort = getPortProperty(environment, "management.server.");
		if (managementPort != null && managementPort < 0) {
			return DISABLED;
		}
		Integer serverPort = getPortProperty(environment, "server.");
		return ((managementPort == null || (serverPort == null && managementPort.equals(8080))
				|| (managementPort != 0 && managementPort.equals(serverPort))) ? SAME : DIFFERENT);
	}

	private static @Nullable Integer getPortProperty(Environment environment, String prefix) {
		return environment.getProperty(prefix + "port", Integer.class);
	}

}
