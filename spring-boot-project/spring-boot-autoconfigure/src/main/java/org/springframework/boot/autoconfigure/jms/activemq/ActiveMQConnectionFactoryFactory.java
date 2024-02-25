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

package org.springframework.boot.autoconfigure.jms.activemq;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;

import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQProperties.Packages;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory to create a {@link ActiveMQConnectionFactory} instance from properties defined
 * in {@link ActiveMQProperties}.
 *
 * @author Phillip Webb
 * @author Venil Noronha
 * @author Eddú Meléndez
 */
class ActiveMQConnectionFactoryFactory {

	private final ActiveMQProperties properties;

	private final List<ActiveMQConnectionFactoryCustomizer> factoryCustomizers;

	private final ActiveMQConnectionDetails connectionDetails;

	/**
	 * Constructs a new ActiveMQConnectionFactoryFactory with the specified properties,
	 * factory customizers, and connection details.
	 * @param properties the ActiveMQProperties to be used for configuring the factory
	 * @param factoryCustomizers the list of ActiveMQConnectionFactoryCustomizer objects
	 * to customize the factory
	 * @param connectionDetails the ActiveMQConnectionDetails to be used for establishing
	 * the connection
	 * @throws IllegalArgumentException if the properties argument is null
	 */
	ActiveMQConnectionFactoryFactory(ActiveMQProperties properties,
			List<ActiveMQConnectionFactoryCustomizer> factoryCustomizers, ActiveMQConnectionDetails connectionDetails) {
		Assert.notNull(properties, "Properties must not be null");
		this.properties = properties;
		this.factoryCustomizers = (factoryCustomizers != null) ? factoryCustomizers : Collections.emptyList();
		this.connectionDetails = connectionDetails;
	}

	/**
	 * Creates a new instance of ActiveMQConnectionFactory based on the provided
	 * factoryClass.
	 * @param factoryClass the class of the factory to be created
	 * @return a new instance of ActiveMQConnectionFactory
	 * @throws IllegalStateException if unable to create ActiveMQConnectionFactory
	 */
	<T extends ActiveMQConnectionFactory> T createConnectionFactory(Class<T> factoryClass) {
		try {
			return doCreateConnectionFactory(factoryClass);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create ActiveMQConnectionFactory", ex);
		}
	}

	/**
	 * Creates a connection factory of the specified type.
	 * @param factoryClass the class of the connection factory to create
	 * @return the created connection factory
	 * @throws Exception if an error occurs while creating the connection factory
	 */
	private <T extends ActiveMQConnectionFactory> T doCreateConnectionFactory(Class<T> factoryClass) throws Exception {
		T factory = createConnectionFactoryInstance(factoryClass);
		if (this.properties.getCloseTimeout() != null) {
			factory.setCloseTimeout((int) this.properties.getCloseTimeout().toMillis());
		}
		factory.setNonBlockingRedelivery(this.properties.isNonBlockingRedelivery());
		if (this.properties.getSendTimeout() != null) {
			factory.setSendTimeout((int) this.properties.getSendTimeout().toMillis());
		}
		Packages packages = this.properties.getPackages();
		if (packages.getTrustAll() != null) {
			factory.setTrustAllPackages(packages.getTrustAll());
		}
		if (!packages.getTrusted().isEmpty()) {
			factory.setTrustedPackages(packages.getTrusted());
		}
		customize(factory);
		return factory;
	}

	/**
	 * Creates an instance of the ActiveMQConnectionFactory class based on the provided
	 * factoryClass.
	 * @param factoryClass the class of the ActiveMQConnectionFactory to be instantiated
	 * @return an instance of the ActiveMQConnectionFactory class
	 * @throws InstantiationException if an error occurs during instantiation
	 * @throws IllegalAccessException if access to the constructor is denied
	 * @throws InvocationTargetException if an error occurs during invocation of the
	 * constructor
	 * @throws NoSuchMethodException if the constructor does not exist
	 */
	private <T extends ActiveMQConnectionFactory> T createConnectionFactoryInstance(Class<T> factoryClass)
			throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		String brokerUrl = this.connectionDetails.getBrokerUrl();
		String user = this.connectionDetails.getUser();
		String password = this.connectionDetails.getPassword();
		if (StringUtils.hasLength(user) && StringUtils.hasLength(password)) {
			return factoryClass.getConstructor(String.class, String.class, String.class)
				.newInstance(user, password, brokerUrl);
		}
		return factoryClass.getConstructor(String.class).newInstance(brokerUrl);
	}

	/**
	 * Customizes the given ActiveMQConnectionFactory by applying all registered factory
	 * customizers.
	 * @param connectionFactory the ActiveMQConnectionFactory to be customized
	 */
	private void customize(ActiveMQConnectionFactory connectionFactory) {
		for (ActiveMQConnectionFactoryCustomizer factoryCustomizer : this.factoryCustomizers) {
			factoryCustomizer.customize(connectionFactory);
		}
	}

}
