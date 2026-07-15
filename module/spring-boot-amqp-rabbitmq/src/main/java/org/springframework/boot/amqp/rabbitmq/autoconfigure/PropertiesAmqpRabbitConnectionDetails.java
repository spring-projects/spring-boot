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

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Adapts {@link AmqpRabbitProperties} to {@link AmqpRabbitConnectionDetails}.
 *
 * @author Eddú Meléndez
 */
class PropertiesAmqpRabbitConnectionDetails implements AmqpRabbitConnectionDetails {

	private final AmqpRabbitProperties properties;

	private final @Nullable SslBundles sslBundles;

	PropertiesAmqpRabbitConnectionDetails(AmqpRabbitProperties properties, @Nullable SslBundles sslBundles) {
		this.properties = properties;
		this.sslBundles = sslBundles;
	}

	@Override
	public Address getAddress() {
		String address = this.properties.determineAddress();
		int portSeparatorIndex = address.lastIndexOf(':');
		String host = address.substring(0, portSeparatorIndex);
		String port = address.substring(portSeparatorIndex + 1);
		return new Address(host, Integer.parseInt(port));
	}

	@Override
	public String getUsername() {
		return this.properties.determineUsername();
	}

	@Override
	public @Nullable String getPassword() {
		return this.properties.determinePassword();
	}

	@Override
	public @Nullable String getVirtualHost() {
		return this.properties.determineVirtualHost();
	}

	@Override
	public @Nullable SslBundle getSslBundle() {
		String bundle = this.properties.getSsl().getBundle();
		if (StringUtils.hasLength(bundle)) {
			Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
			return this.sslBundles.getBundle(bundle);
		}
		return null;
	}

}
