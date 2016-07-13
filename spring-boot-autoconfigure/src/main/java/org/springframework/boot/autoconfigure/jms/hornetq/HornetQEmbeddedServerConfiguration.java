/*
 * Copyright 2012-2016 the original author or authors.
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
import java.util.List;

import org.hornetq.jms.server.config.JMSConfiguration;
import org.hornetq.jms.server.config.JMSQueueConfiguration;
import org.hornetq.jms.server.config.TopicConfiguration;
import org.hornetq.jms.server.config.impl.JMSConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSQueueConfigurationImpl;
import org.hornetq.jms.server.config.impl.TopicConfigurationImpl;
import org.hornetq.jms.server.embedded.EmbeddedJMS;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

/**
 * Configuration used to create the embedded HornetQ server.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 1.2.0
 * @deprecated as of 1.4 in favor of the artemis support
 */
@Configuration
@ConditionalOnClass(name = HornetQConnectionFactoryFactory.EMBEDDED_JMS_CLASS)
@ConditionalOnProperty(prefix = "spring.hornetq.embedded", name = "enabled", havingValue = "true", matchIfMissing = true)
@Deprecated
class HornetQEmbeddedServerConfiguration {

	private final HornetQProperties properties;

	private final List<HornetQConfigurationCustomizer> configurationCustomizers;

	private final List<JMSQueueConfiguration> queuesConfiguration;

	private final List<TopicConfiguration> topicsConfiguration;

	HornetQEmbeddedServerConfiguration(HornetQProperties properties,
			ObjectProvider<List<HornetQConfigurationCustomizer>> configurationCustomizersProvider,
			ObjectProvider<List<JMSQueueConfiguration>> queuesConfigurationProvider,
			ObjectProvider<List<TopicConfiguration>> topicsConfigurationProvider) {
		this.properties = properties;
		this.configurationCustomizers = configurationCustomizersProvider.getIfAvailable();
		this.queuesConfiguration = queuesConfigurationProvider.getIfAvailable();
		this.topicsConfiguration = topicsConfigurationProvider.getIfAvailable();
	}

	@Bean
	@ConditionalOnMissingBean
	public org.hornetq.core.config.Configuration hornetQConfiguration() {
		return new HornetQEmbeddedConfigurationFactory(this.properties)
				.createConfiguration();
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	@ConditionalOnMissingBean
	public EmbeddedJMS hornetQServer(org.hornetq.core.config.Configuration configuration,
			JMSConfiguration jmsConfiguration) {
		EmbeddedJMS server = new EmbeddedJMS();
		customize(configuration);
		server.setConfiguration(configuration);
		server.setJmsConfiguration(jmsConfiguration);
		server.setRegistry(new HornetQNoOpBindingRegistry());
		return server;
	}

	private void customize(org.hornetq.core.config.Configuration configuration) {
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
			configuration.getQueueConfigurations().add(new JMSQueueConfigurationImpl(
					queue, null, persistent, "/queue/" + queue));
		}
	}

	private void addTopics(JMSConfiguration configuration, String[] topics) {
		for (String topic : topics) {
			configuration.getTopicConfigurations()
					.add(new TopicConfigurationImpl(topic, "/topic/" + topic));
		}
	}

}
