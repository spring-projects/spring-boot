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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.ConnectionFactory;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.hornetq.jms.server.config.JMSConfiguration;
import org.hornetq.jms.server.config.JMSQueueConfiguration;
import org.hornetq.jms.server.config.TopicConfiguration;
import org.hornetq.jms.server.config.impl.JMSConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSQueueConfigurationImpl;
import org.hornetq.jms.server.config.impl.TopicConfigurationImpl;
import org.hornetq.jms.server.embedded.EmbeddedJMS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} to integrate with an HornetQ broker. Connect by default to a broker
 * available on the local machine with the default settings. If the necessary classes are
 * present, the broker can also be embedded in the application itself.
 * 
 * @author Stephane Nicoll
 * @since 1.1.0
 */
@Configuration
@AutoConfigureBefore(JmsAutoConfiguration.class)
@ConditionalOnClass({ ConnectionFactory.class, HornetQJMSClient.class })
@EnableConfigurationProperties(HornetQProperties.class)
public class HornetQAutoConfiguration {

	private static final String EMBEDDED_JMS_CLASS = "org.hornetq.jms.server.embedded.EmbeddedJMS";

	@Autowired
	private HornetQProperties properties;

	/**
	 * Create the {@link ConnectionFactory} to use if none is provided. If no
	 * {@linkplain HornetQProperties#getMode() mode} has been explicitly set, connect to
	 * the embedded server if it has been requested or to a broker available on the local
	 * machine with the default settings otherwise.
	 */
	@Bean
	@ConditionalOnMissingBean
	public ConnectionFactory jmsConnectionFactory() {
		HornetQMode mode = this.properties.getMode();
		if (mode == null) {
			mode = deduceMode();
		}
		if (mode == HornetQMode.EMBEDDED) {
			return createEmbeddedConnectionFactory();
		}
		return createNativeConnectionFactory();
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

	private ConnectionFactory createEmbeddedConnectionFactory() {
		try {
			TransportConfiguration transportConfiguration = new TransportConfiguration(
					InVMConnectorFactory.class.getName());
			ServerLocator serviceLocator = HornetQClient
					.createServerLocatorWithoutHA(transportConfiguration);
			return new HornetQConnectionFactory(serviceLocator);
		}
		catch (NoClassDefFoundError ex) {
			throw new IllegalStateException("Unable to create InVM "
					+ "HornetQ connection, ensure that hornet-jms-server.jar "
					+ "is in the classpath", ex);
		}
	}

	private ConnectionFactory createNativeConnectionFactory() {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(TransportConstants.HOST_PROP_NAME, this.properties.getHost());
		params.put(TransportConstants.PORT_PROP_NAME, this.properties.getPort());
		TransportConfiguration transportConfiguration = new TransportConfiguration(
				NettyConnectorFactory.class.getName(), params);
		return HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF,
				transportConfiguration);
	}

	/**
	 * Configuration used to create the embedded HornetQ server.
	 */
	@Configuration
	@ConditionalOnClass(name = EMBEDDED_JMS_CLASS)
	@ConditionalOnProperty(prefix = "spring.hornetq.embedded", value = "enabled")
	static class EmbeddedServerConfiguration {

		@Autowired
		private HornetQProperties properties;

		@Autowired(required = false)
		private List<HornetQConfigurationCustomizer> configurationCustomizers;

		@Autowired(required = false)
		private List<JMSQueueConfiguration> queuesConfiguration;

		@Autowired(required = false)
		private List<TopicConfiguration> topicsConfiguration;

		@Bean
		@ConditionalOnMissingBean
		public org.hornetq.core.config.Configuration hornetQConfiguration() {
			return new HornetQEmbeddedConfigurationFactory(this.properties)
					.createConfiguration();
		}

		@Bean(initMethod = "start", destroyMethod = "stop")
		@ConditionalOnMissingBean
		public EmbeddedJMS hornetQServer(
				org.hornetq.core.config.Configuration configuration,
				JMSConfiguration jmsConfiguration) {
			EmbeddedJMS server = new EmbeddedJMS();
			applyCustomizers(configuration);
			server.setConfiguration(configuration);
			server.setJmsConfiguration(jmsConfiguration);
			server.setRegistry(new HornetQNoOpBindingRegistry());
			return server;
		}

		private void applyCustomizers(org.hornetq.core.config.Configuration configuration) {
			if (this.configurationCustomizers != null) {
				AnnotationAwareOrderComparator.sort(this.configurationCustomizers);
				for (HornetQConfigurationCustomizer customizer : this.configurationCustomizers) {
					customizer.customize(configuration);
				}
			}
		}

		@Bean
		@ConditionalOnMissingBean
		public JMSConfiguration hornetQJmsConfiguration() {
			JMSConfiguration configuration = new JMSConfigurationImpl();
			addAll(configuration.getQueueConfigurations(), this.queuesConfiguration);
			addAll(configuration.getTopicConfigurations(), this.topicsConfiguration);
			addQueues(configuration, this.properties.getEmbedded().getQueues());
			addTopis(configuration, this.properties.getEmbedded().getTopics());
			return configuration;
		}

		private <T> void addAll(List<T> list, Collection<? extends T> items) {
			if (items != null) {
				list.addAll(items);
			}
		}

		private void addQueues(JMSConfiguration configuration, String[] queues) {
			boolean persistent = this.properties.getEmbedded().isPersistent();
			for (String queue : queues) {
				configuration.getQueueConfigurations().add(
						new JMSQueueConfigurationImpl(queue, null, persistent, "/queue/"
								+ queue));
			}
		}

		private void addTopis(JMSConfiguration configuration, String[] topics) {
			for (String topic : topics) {
				configuration.getTopicConfigurations().add(
						new TopicConfigurationImpl(topic, "/topic/" + topic));
			}
		}

	}

}
