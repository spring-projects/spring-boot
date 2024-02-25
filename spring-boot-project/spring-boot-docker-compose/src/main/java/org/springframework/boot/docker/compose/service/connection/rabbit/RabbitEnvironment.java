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

package org.springframework.boot.docker.compose.service.connection.rabbit;

import java.util.Map;

/**
 * RabbitMQ environment details.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class RabbitEnvironment {

	private final String username;

	private final String password;

	/**
     * Constructs a RabbitEnvironment object with the provided environment variables.
     * 
     * @param env a Map containing the environment variables
     * 
     * The constructor initializes the username and password fields of the RabbitEnvironment object
     * using the values from the environment variables. If the "RABBITMQ_DEFAULT_USER" variable is
     * present, it is used as the username. Otherwise, if the "RABBITMQ_USERNAME" variable is present,
     * it is used as the username. If neither of these variables are present, the default username is
     * set to "guest".
     * 
     * Similarly, the constructor initializes the password field using the values from the environment
     * variables. If the "RABBITMQ_DEFAULT_PASS" variable is present, it is used as the password.
     * Otherwise, if the "RABBITMQ_PASSWORD" variable is present, it is used as the password. If neither
     * of these variables are present, the default password is set to "guest".
     */
    RabbitEnvironment(Map<String, String> env) {
		this.username = env.getOrDefault("RABBITMQ_DEFAULT_USER", env.getOrDefault("RABBITMQ_USERNAME", "guest"));
		this.password = env.getOrDefault("RABBITMQ_DEFAULT_PASS", env.getOrDefault("RABBITMQ_PASSWORD", "guest"));
	}

	/**
     * Returns the username associated with the RabbitEnvironment object.
     *
     * @return the username associated with the RabbitEnvironment object
     */
    String getUsername() {
		return this.username;
	}

	/**
     * Returns the password of the RabbitEnvironment.
     *
     * @return the password of the RabbitEnvironment
     */
    String getPassword() {
		return this.password;
	}

}
