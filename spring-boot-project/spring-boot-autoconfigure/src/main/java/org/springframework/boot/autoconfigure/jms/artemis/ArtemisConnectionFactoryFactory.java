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

package org.springframework.boot.autoconfigure.jms.artemis;

import java.lang.reflect.Constructor;

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

	/**
     * Constructs a new instance of ArtemisConnectionFactoryFactory.
     * 
     * @param beanFactory the ListableBeanFactory to be used for creating the factory
     * @param properties the ArtemisProperties containing the configuration properties
     * @param connectionDetails the ArtemisConnectionDetails containing the connection details
     * @throws IllegalArgumentException if any of the parameters are null
     */
    ArtemisConnectionFactoryFactory(ListableBeanFactory beanFactory, ArtemisProperties properties,
			ArtemisConnectionDetails connectionDetails) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		Assert.notNull(properties, "Properties must not be null");
		Assert.notNull(connectionDetails, "ConnectionDetails must not be null");
		this.beanFactory = beanFactory;
		this.properties = properties;
		this.connectionDetails = connectionDetails;
	}

	/**
     * Creates a connection factory of the specified type.
     * 
     * @param factoryClass the class of the connection factory to create
     * @return the created connection factory
     * @throws IllegalStateException if unable to create the connection factory
     */
    <T extends ActiveMQConnectionFactory> T createConnectionFactory(Class<T> factoryClass) {
		try {
			startEmbeddedJms();
			return doCreateConnectionFactory(factoryClass);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create ActiveMQConnectionFactory", ex);
		}
	}

	/**
     * Starts the embedded JMS server.
     * This method iterates through a list of embedded JMS classes and checks if they are present.
     * If a class is present, it attempts to get all beans of that class type from the bean factory.
     * Any exceptions thrown during this process are ignored.
     */
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

	/**
     * Creates a connection factory of the specified type.
     * 
     * @param factoryClass the class of the connection factory to create
     * @return the created connection factory
     * @throws Exception if an error occurs during the creation of the connection factory
     */
    private <T extends ActiveMQConnectionFactory> T doCreateConnectionFactory(Class<T> factoryClass) throws Exception {
		ArtemisMode mode = this.connectionDetails.getMode();
		if (mode == null) {
			mode = deduceMode();
		}
		if (mode == ArtemisMode.EMBEDDED) {
			return createEmbeddedConnectionFactory(factoryClass);
		}
		return createNativeConnectionFactory(factoryClass);
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

	/**
     * Checks if any of the embedded JMS classes are present.
     * 
     * @return true if any of the embedded JMS classes are present, false otherwise
     */
    private boolean isEmbeddedJmsClassPresent() {
		for (String embeddedJmsClass : EMBEDDED_JMS_CLASSES) {
			if (ClassUtils.isPresent(embeddedJmsClass, null)) {
				return true;
			}
		}
		return false;
	}

	/**
     * Creates an embedded connection factory of the specified type.
     * 
     * @param factoryClass the class of the connection factory to create
     * @return the created embedded connection factory
     * @throws Exception if an error occurs while creating the connection factory
     * @throws IllegalStateException if the required Artemis JMS server library is not in the classpath
     */
    private <T extends ActiveMQConnectionFactory> T createEmbeddedConnectionFactory(Class<T> factoryClass)
			throws Exception {
		try {
			TransportConfiguration transportConfiguration = new TransportConfiguration(
					InVMConnectorFactory.class.getName(), this.properties.getEmbedded().generateTransportParameters());
			ServerLocator serviceLocator = ActiveMQClient.createServerLocatorWithoutHA(transportConfiguration);
			return factoryClass.getConstructor(ServerLocator.class).newInstance(serviceLocator);
		}
		catch (NoClassDefFoundError ex) {
			throw new IllegalStateException("Unable to create InVM "
					+ "Artemis connection, ensure that artemis-jms-server.jar is in the classpath", ex);
		}
	}

	/**
     * Creates a native connection factory of the specified factory class.
     * 
     * @param factoryClass the class of the connection factory to create
     * @return the created native connection factory
     * @throws Exception if an error occurs while creating the connection factory
     */
    private <T extends ActiveMQConnectionFactory> T createNativeConnectionFactory(Class<T> factoryClass)
			throws Exception {
		T connectionFactory = newNativeConnectionFactory(factoryClass);
		String user = this.connectionDetails.getUser();
		if (StringUtils.hasText(user)) {
			connectionFactory.setUser(user);
			connectionFactory.setPassword(this.connectionDetails.getPassword());
		}
		return connectionFactory;
	}

	/**
     * Creates a new instance of the native connection factory based on the provided factory class.
     * 
     * @param factoryClass the class of the native connection factory
     * @return a new instance of the native connection factory
     * @throws Exception if an error occurs during the creation of the native connection factory
     */
    private <T extends ActiveMQConnectionFactory> T newNativeConnectionFactory(Class<T> factoryClass) throws Exception {
		String brokerUrl = StringUtils.hasText(this.connectionDetails.getBrokerUrl())
				? this.connectionDetails.getBrokerUrl() : DEFAULT_BROKER_URL;
		Constructor<T> constructor = factoryClass.getConstructor(String.class);
		return constructor.newInstance(brokerUrl);

	}

}
