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

package org.springframework.boot.amqp.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to establish a connection to a RabbitMQ Stream service.
 *
 * @author Eddú Meléndez
 * @since 4.1.0
 */
public interface RabbitStreamConnectionDetails extends ConnectionDetails {

	/**
	 * Rabbit Stream server host.
	 * @return the Rabbit Stream server host
	 */
	String getHost();

	/**
	 * Rabbit Stream server port.
	 * @return the Rabbit Stream server port
	 */
	int getPort();

	/**
	 * Username for authentication.
	 * @return the username for authentication or {@code null}
	 */
	default @Nullable String getUsername() {
		return null;
	}

	/**
	 * Password for authentication.
	 * @return the password for authentication or {@code null}
	 */
	default @Nullable String getPassword() {
		return null;
	}

	/**
	 * Virtual host to use when connecting to the broker.
	 * @return the virtual host to use when connecting to the broker or {@code null}
	 */
	default @Nullable String getVirtualHost() {
		return null;
	}

}
