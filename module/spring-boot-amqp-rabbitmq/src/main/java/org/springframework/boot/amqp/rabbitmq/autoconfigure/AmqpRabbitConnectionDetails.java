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

package org.springframework.boot.amqp.rabbitmq.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

/**
 * Details required to establish a connection to a RabbitMQ AMQP service.
 *
 * @author Eddú Meléndez
 * @since 4.2.0
 */
public interface AmqpRabbitConnectionDetails extends ConnectionDetails {

	/**
	 * Return the address of the broker.
	 * @return the address
	 */
	Address getAddress();

	/**
	 * Login user to authenticate to the broker.
	 * @return the login user to authenticate to the broker or {@code null}
	 */
	default @Nullable String getUsername() {
		return null;
	}

	/**
	 * Password used to authenticate to the broker.
	 * @return the password to authenticate to the broker or {@code null}
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

	/**
	 * SSL bundle to use.
	 * @return the SSL bundle to use or {@code null}
	 */
	default @Nullable SslBundle getSslBundle() {
		return null;
	}

	/**
	 * A RabbitMQ address.
	 *
	 * @param host the host
	 * @param port the port
	 */
	record Address(String host, int port) {
	}

}
