/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.activemq;

import java.util.Map;

/**
 * ActiveMQ environment details.
 *
 * @author Stephane Nicoll
 */
class ActiveMQEnvironment {

	private final String user;

	private final String password;

	/**
	 * Constructs a new ActiveMQEnvironment object with the provided environment
	 * variables.
	 * @param env a map containing the environment variables
	 * @throws NullPointerException if the provided map is null
	 */
	ActiveMQEnvironment(Map<String, String> env) {
		this.user = env.get("ACTIVEMQ_USERNAME");
		this.password = env.get("ACTIVEMQ_PASSWORD");
	}

	/**
	 * Returns the user associated with the ActiveMQEnvironment.
	 * @return the user associated with the ActiveMQEnvironment
	 */
	String getUser() {
		return this.user;
	}

	/**
	 * Returns the password used for authentication.
	 * @return the password used for authentication
	 */
	String getPassword() {
		return this.password;
	}

}
