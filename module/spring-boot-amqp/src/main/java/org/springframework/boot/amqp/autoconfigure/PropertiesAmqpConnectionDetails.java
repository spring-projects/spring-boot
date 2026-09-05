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

/**
 * Adapts {@link AmqpProperties} to {@link AmqpConnectionDetails}.
 *
 * @author Stephane Nicoll
 */
class PropertiesAmqpConnectionDetails implements AmqpConnectionDetails {

	private final AmqpProperties properties;

	PropertiesAmqpConnectionDetails(AmqpProperties properties) {
		this.properties = properties;
	}

	@Override
	public Address getAddress() {
		return new Address(this.properties.getHost(), this.properties.getPort());
	}

	@Override
	public @Nullable String getUsername() {
		return this.properties.getUsername();
	}

	@Override
	public @Nullable String getPassword() {
		return this.properties.getPassword();
	}

}
