/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jms.amqp10;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.policy.JmsDefaultDeserializationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory of JmsConnectionFactory instances.
 *
 * @author Timothy Bish
 */
public class QpidJMSConnectionFactoryFactory {

	private static final Logger LOG = LoggerFactory
			.getLogger(QpidJMSConnectionFactoryFactory.class);

	private static final String DEFAULT_REMOTE_URL = "amqp://localhost:5672";

	private final QpidJMSProperties properties;

	/**
	 * Create a new factory instance with the given properties.
	 *
	 * @param properties the configuration properties to apply to newly created connections.
	 */
	public QpidJMSConnectionFactoryFactory(QpidJMSProperties properties) {
		Assert.notNull(properties, "Properties must not be null");
		this.properties = properties;
	}

	/**
	 * Creates and returns a JmsConnectionFactory instance using the current configuration
	 * to prepare the factory for use.
	 *
	 * @param factoryClass The type of JmsConnectionFactory to create.
	 *
	 * @return a newly created and configured JmsConnectionFactory instance.
	 */
	public JmsConnectionFactory createConnectionFactory(
			Class<JmsConnectionFactory> factoryClass) {
		try {
			JmsConnectionFactory factory = new JmsConnectionFactory();

			factory.setRemoteURI(getRemoteURI());

			// Override the URI options with configuration values, but only if
			// the value is actually set.

			if (StringUtils.hasLength(this.properties.getUsername())) {
				factory.setUsername(this.properties.getUsername());
			}

			if (StringUtils.hasLength(this.properties.getPassword())) {
				factory.setPassword(this.properties.getPassword());
			}

			if (StringUtils.hasLength(this.properties.getClientId())) {
				factory.setClientID(this.properties.getClientId());
			}

			if (this.properties.isReceiveLocalOnly() != null) {
				factory.setReceiveLocalOnly(this.properties.isReceiveLocalOnly());
			}

			if (this.properties.isReceiveNoWaitLocalOnly() != null) {
				factory.setReceiveNoWaitLocalOnly(this.properties.isReceiveNoWaitLocalOnly());
			}

			configureDeserializationPolicy(this.properties, factory);

			return factory;
		}
		catch (Exception ex) {
			LOG.error("Exception while createing Qpid JMS Connection Factory.", ex);
			throw new IllegalStateException(
					"Failed to create the Qpid JMS ConnectionFactory, "
							+ "make sure the client Jar is on the Classpath.",
					ex);
		}
	}

	public String getRemoteURI() {
		if (StringUtils.hasLength(this.properties.getRemoteURL())) {
			return this.properties.getRemoteURL();
		}
		else {
			return DEFAULT_REMOTE_URL;
		}
	}

	private void configureDeserializationPolicy(QpidJMSProperties properties,
			JmsConnectionFactory factory) {
		JmsDefaultDeserializationPolicy deserializationPolicy = (JmsDefaultDeserializationPolicy) factory
				.getDeserializationPolicy();

		if (StringUtils.hasLength(properties.getDeserializationPolicy().getWhiteList())) {
			deserializationPolicy
					.setWhiteList(properties.getDeserializationPolicy().getWhiteList());
		}

		if (StringUtils.hasLength(properties.getDeserializationPolicy().getBlackList())) {
			deserializationPolicy
					.setBlackList(properties.getDeserializationPolicy().getBlackList());
		}
	}
}
