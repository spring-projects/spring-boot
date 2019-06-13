/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.server;

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

	private static Integer getPortProperty(Environment environment, String prefix) {
		return environment.getProperty(prefix + "port", Integer.class);
	}

}
