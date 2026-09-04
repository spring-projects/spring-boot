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

package org.springframework.boot.amqp.docker.compose;

import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * RabbitMQ environment details.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class RabbitEnvironment {

	private final @Nullable String username;

	private final @Nullable String password;

	RabbitEnvironment(Map<String, @Nullable String> env) {
		this.username = extract(env, "RABBITMQ_DEFAULT_USER", "RABBITMQ_USERNAME");
		this.password = extract(env, "RABBITMQ_DEFAULT_PASS", "RABBITMQ_PASSWORD");
	}

	private static String extract(Map<String, @Nullable String> env, String key, String fallbackKey) {
		String value = env.get(key);
		value = (value != null) ? value : env.get(fallbackKey);
		return (value != null) ? value : "guest";
	}

	@Nullable String getUsername() {
		return this.username;
	}

	@Nullable String getPassword() {
		return this.password;
	}

}
