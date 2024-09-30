/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.amqp;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

/**
 * Details required to establish a connection to a RabbitMQ Stream service.
 *
 * @author Eddú Meléndez
 * @since 3.4.0
 */
public interface RabbitStreamConnectionDetails extends ConnectionDetails {

	/**
	 * Rabbit server host.
	 * @return the rabbit server host
	 */
	String getHost();

	/**
	 * Rabbit Stream server port.
	 * @return the rabbit stream server port
	 */
	int getPort();

	/**
	 * Login user to authenticate to the broker.
	 * @return the login user to authenticate to the broker or {@code null}
	 */
	default String getUsername() {
		return null;
	}

	/**
	 * Login to authenticate against the broker.
	 * @return the login to authenticate against the broker or {@code null}
	 */
	default String getPassword() {
		return null;
	}

	/**
	 * Virtual host to use when connecting to the broker.
	 * @return the virtual host to use when connecting to the broker or {@code null}
	 */
	default String getVirtualHost() {
		return null;
	}

}
