/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.jms.artemis;

import java.util.Collection;
import java.util.List;

import org.apache.activemq.artemis.jms.server.config.JMSConfiguration;
import org.apache.activemq.artemis.jms.server.config.JMSQueueConfiguration;
import org.apache.activemq.artemis.jms.server.config.TopicConfiguration;
import org.apache.activemq.artemis.jms.server.config.impl.JMSConfigurationImpl;
import org.apache.activemq.artemis.jms.server.config.impl.JMSQueueConfigurationImpl;
import org.apache.activemq.artemis.jms.server.config.impl.TopicConfigurationImpl;
import org.apache.activemq.artemis.jms.server.embedded.EmbeddedJMS;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * Configuration used to create the embedded Artemis server.
 *
 * @author Eddú Meléndez
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@Configuration
@ConditionalOnClass(name = ArtemisConnectionFactoryFactory.EMBEDDED_JMS_CLASS)
@ConditionalOnProperty(prefix = "spring.artemis.embedded", name = "enabled", havingValue = "true", matchIfMissing = true)
class ArtemisEmbeddedServerConfiguration {

	private final ArtemisProperties properties;

	private final List<ArtemisConfigurationCustomizer> configurationCustomizers;

	private final List<JMSQueueConfiguration> queuesConfiguration;

	private final List<TopicConfiguration> topicsConfiguration;

	ArtemisEmbeddedServerConfiguration(ArtemisProperties properties,
			ObjectProvider<List<ArtemisConfigurationCustomizer>> configurationCustomizers,
			ObjectProvider<List<JMSQueueConfiguration>> queuesConfiguration,
			ObjectProvider<List<TopicConfiguration>> topicsConfiguration) {
		this.properties = properties;
		this.configurationCustomizers = configurationCustomizers.getIfAvailable();
		this.queuesConfiguration = queuesConfiguration.getIfAvailable();
		this.topicsConfiguration = topicsConfiguration.getIfAvailable();
	}

	@Bean
	@ConditionalOnMissingBean
	public org.apache.activemq.artemis.core.config.Configuration artemisConfiguration() {
		return new ArtemisEmbeddedConfigurationFactory(this.properties)
				.createConfiguration();
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	@ConditionalOnMissingBean
	public EmbeddedJMS artemisServer(
			org.apache.activemq.artemis.core.config.Configuration configuration,
			JMSConfiguration jmsConfiguration) {
		EmbeddedJMS server = new EmbeddedJMS();
		customize(configuration);
		server.setConfiguration(configuration);
		server.setJmsConfiguration(jmsConfiguration);
		server.setRegistry(new ArtemisNoOpBindingRegistry());
		return server;
	}

	private void customize(
			org.apache.activemq.artemis.core.config.Configuration configuration) {
		if (this.configurationCustomizers != null) {
			AnnotationAwareOrderComparator.sort(this.configurationCustomizers);
			for (ArtemisConfigurationCustomizer customizer : this.configurationCustomizers) {
				customizer.customize(configuration);
			}
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public JMSConfiguration artemisJmsConfiguration() {
		JMSConfiguration configuration = new JMSConfigurationImpl();
		addAll(configuration.getQueueConfigurations(), this.queuesConfiguration);
		addAll(configuration.getTopicConfigurations(), this.topicsConfiguration);
		addQueues(configuration, this.properties.getEmbedded().getQueues());
		addTopics(configuration, this.properties.getEmbedded().getTopics());
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
			JMSQueueConfigurationImpl jmsQueueConfiguration = new JMSQueueConfigurationImpl();
			jmsQueueConfiguration.setName(queue);
			jmsQueueConfiguration.setDurable(persistent);
			jmsQueueConfiguration.setBindings("/queue/" + queue);
			configuration.getQueueConfigurations().add(jmsQueueConfiguration);
		}
	}

	private void addTopics(JMSConfiguration configuration, String[] topics) {
		for (String topic : topics) {
			TopicConfigurationImpl topicConfiguration = new TopicConfigurationImpl();
			topicConfiguration.setName(topic);
			topicConfiguration.setBindings("/topic/" + topic);
			configuration.getTopicConfigurations().add(topicConfiguration);
		}
	}

}
