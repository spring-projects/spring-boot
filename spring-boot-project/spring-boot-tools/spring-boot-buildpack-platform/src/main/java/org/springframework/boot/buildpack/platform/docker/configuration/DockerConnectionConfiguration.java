/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.configuration;

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
	record Host(String address, boolean secure, String certificatePath) implements DockerConnectionConfiguration {

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
