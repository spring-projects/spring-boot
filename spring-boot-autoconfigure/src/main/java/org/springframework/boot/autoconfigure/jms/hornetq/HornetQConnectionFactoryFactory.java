/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms.hornetq;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Factory to create a {@link HornetQConnectionFactory} instance from properties defined
 * in {@link HornetQProperties}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 1.2.0
 */
class HornetQConnectionFactoryFactory {

	static final String EMBEDDED_JMS_CLASS = "org.hornetq.jms.server.embedded.EmbeddedJMS";

	private final HornetQProperties properties;

	public HornetQConnectionFactoryFactory(HornetQProperties properties) {
		Assert.notNull(properties, "Properties must not be null");
		this.properties = properties;
	}

	public <T extends HornetQConnectionFactory> T createConnectionFactory(
			Class<T> factoryClass) {
		try {
			return doCreateConnectionFactory(factoryClass);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create "
					+ "HornetQConnectionFactory", ex);
		}
	}

	private <T extends HornetQConnectionFactory> T doCreateConnectionFactory(
			Class<T> factoryClass) throws Exception {
		HornetQMode mode = this.properties.getMode();
		if (mode == null) {
			mode = deduceMode();
		}
		if (mode == HornetQMode.EMBEDDED) {
			return createEmbeddedConnectionFactory(factoryClass);
		}
		return createNativeConnectionFactory(factoryClass);
	}

	/**
	 * Deduce the {@link HornetQMode} to use if none has been set.
	 */
	private HornetQMode deduceMode() {
		if (this.properties.getEmbedded().isEnabled()
				&& ClassUtils.isPresent(EMBEDDED_JMS_CLASS, null)) {
			return HornetQMode.EMBEDDED;
		}
		return HornetQMode.NATIVE;
	}

	private <T extends HornetQConnectionFactory> T createEmbeddedConnectionFactory(
			Class<T> factoryClass) throws Exception {
		try {
			TransportConfiguration transportConfiguration = new TransportConfiguration(
					InVMConnectorFactory.class.getName(), this.properties.getEmbedded()
							.generateTransportParameters());
			ServerLocator serviceLocator = HornetQClient
					.createServerLocatorWithoutHA(transportConfiguration);
			return factoryClass.getConstructor(ServerLocator.class).newInstance(
					serviceLocator);
		}
		catch (NoClassDefFoundError ex) {
			throw new IllegalStateException("Unable to create InVM "
					+ "HornetQ connection, ensure that hornet-jms-server.jar "
					+ "is in the classpath", ex);
		}
	}

	private <T extends HornetQConnectionFactory> T createNativeConnectionFactory(
			Class<T> factoryClass) throws Exception {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(TransportConstants.HOST_PROP_NAME, this.properties.getHost());
		params.put(TransportConstants.PORT_PROP_NAME, this.properties.getPort());
		TransportConfiguration transportConfiguration = new TransportConfiguration(
				NettyConnectorFactory.class.getName(), params);
		Constructor<T> constructor = factoryClass.getConstructor(boolean.class,
				TransportConfiguration[].class);
		return constructor.newInstance(false,
				new TransportConfiguration[] { transportConfiguration });
	}

}
