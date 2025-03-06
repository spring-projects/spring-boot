/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms.artemis;

import java.util.function.Function;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Factory to create an Artemis {@link ActiveMQConnectionFactory} instance from properties
 * defined in {@link ArtemisProperties}.
 *
 * @author Eddú Meléndez
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Justin Bertram
 */
class ArtemisConnectionFactoryFactory {

	private static final String DEFAULT_BROKER_URL = "tcp://localhost:61616";

	static final String[] EMBEDDED_JMS_CLASSES = { "org.apache.activemq.artemis.jms.server.embedded.EmbeddedJMS",
			"org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ" };

	private final ArtemisProperties properties;

	private final ArtemisConnectionDetails connectionDetails;

	private final ListableBeanFactory beanFactory;

	ArtemisConnectionFactoryFactory(ListableBeanFactory beanFactory, ArtemisProperties properties,
			ArtemisConnectionDetails connectionDetails) {
		Assert.notNull(beanFactory, "'beanFactory' must not be null");
		Assert.notNull(properties, "'properties' must not be null");
		Assert.notNull(connectionDetails, "'connectionDetails' must not be null");
		this.beanFactory = beanFactory;
		this.properties = properties;
		this.connectionDetails = connectionDetails;
	}

	<T extends ActiveMQConnectionFactory> T createConnectionFactory(Function<String, T> nativeFactoryCreator,
			Function<ServerLocator, T> embeddedFactoryCreator) {
		try {
			startEmbeddedJms();
			return doCreateConnectionFactory(nativeFactoryCreator, embeddedFactoryCreator);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create ActiveMQConnectionFactory", ex);
		}
	}

	private void startEmbeddedJms() {
		for (String embeddedJmsClass : EMBEDDED_JMS_CLASSES) {
			if (ClassUtils.isPresent(embeddedJmsClass, null)) {
				try {
					this.beanFactory.getBeansOfType(Class.forName(embeddedJmsClass));
				}
				catch (Exception ex) {
					// Ignore
				}
			}
		}
	}

	private <T extends ActiveMQConnectionFactory> T doCreateConnectionFactory(Function<String, T> nativeFactoryCreator,
			Function<ServerLocator, T> embeddedFactoryCreator) throws Exception {
		ArtemisMode mode = this.connectionDetails.getMode();
		if (mode == null) {
			mode = deduceMode();
		}
		if (mode == ArtemisMode.EMBEDDED) {
			return createEmbeddedConnectionFactory(embeddedFactoryCreator);
		}
		return createNativeConnectionFactory(nativeFactoryCreator);
	}

	/**
	 * Deduce the {@link ArtemisMode} to use if none has been set.
	 * @return the mode
	 */
	private ArtemisMode deduceMode() {
		if (this.properties.getEmbedded().isEnabled() && isEmbeddedJmsClassPresent()) {
			return ArtemisMode.EMBEDDED;
		}
		return ArtemisMode.NATIVE;
	}

	private boolean isEmbeddedJmsClassPresent() {
		for (String embeddedJmsClass : EMBEDDED_JMS_CLASSES) {
			if (ClassUtils.isPresent(embeddedJmsClass, null)) {
				return true;
			}
		}
		return false;
	}

	private <T extends ActiveMQConnectionFactory> T createEmbeddedConnectionFactory(
			Function<ServerLocator, T> factoryCreator) throws Exception {
		try {
			TransportConfiguration transportConfiguration = new TransportConfiguration(
					InVMConnectorFactory.class.getName(), this.properties.getEmbedded().generateTransportParameters());
			ServerLocator serverLocator = ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
			return factoryCreator.apply(serverLocator);
		}
		catch (NoClassDefFoundError ex) {
			throw new IllegalStateException("Unable to create InVM "
					+ "Artemis connection, ensure that artemis-jms-server.jar is in the classpath", ex);
		}
	}

	private <T extends ActiveMQConnectionFactory> T createNativeConnectionFactory(Function<String, T> factoryCreator) {
		T connectionFactory = newNativeConnectionFactory(factoryCreator);
		String user = this.connectionDetails.getUser();
		if (StringUtils.hasText(user)) {
			connectionFactory.setUser(user);
			connectionFactory.setPassword(this.connectionDetails.getPassword());
		}
		return connectionFactory;
	}

	private <T extends ActiveMQConnectionFactory> T newNativeConnectionFactory(Function<String, T> factoryCreator) {
		String brokerUrl = StringUtils.hasText(this.connectionDetails.getBrokerUrl())
				? this.connectionDetails.getBrokerUrl() : DEFAULT_BROKER_URL;
		return factoryCreator.apply(brokerUrl);
	}

}
