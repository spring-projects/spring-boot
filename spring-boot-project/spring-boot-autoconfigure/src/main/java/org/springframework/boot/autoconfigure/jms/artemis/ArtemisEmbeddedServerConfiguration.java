/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.List;
import java.util.stream.Collectors;

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

/**
 * Configuration used to create the embedded Artemis server.
 *
 * @author Eddú Meléndez
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EmbeddedJMS.class)
@ConditionalOnProperty(prefix = "spring.artemis.embedded", name = "enabled", havingValue = "true",
		matchIfMissing = true)
class ArtemisEmbeddedServerConfiguration {

	private final ArtemisProperties properties;

	ArtemisEmbeddedServerConfiguration(ArtemisProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public org.apache.activemq.artemis.core.config.Configuration artemisConfiguration() {
		return new ArtemisEmbeddedConfigurationFactory(this.properties).createConfiguration();
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	@ConditionalOnMissingBean
	public EmbeddedJMS artemisServer(org.apache.activemq.artemis.core.config.Configuration configuration,
			JMSConfiguration jmsConfiguration,
			ObjectProvider<ArtemisConfigurationCustomizer> configurationCustomizers) {
		EmbeddedJMS server = new EmbeddedJMS();
		configurationCustomizers.orderedStream().forEach((customizer) -> customizer.customize(configuration));
		server.setConfiguration(configuration);
		server.setJmsConfiguration(jmsConfiguration);
		server.setRegistry(new ArtemisNoOpBindingRegistry());
		return server;
	}

	@Bean
	@ConditionalOnMissingBean
	public JMSConfiguration artemisJmsConfiguration(ObjectProvider<JMSQueueConfiguration> queuesConfiguration,
			ObjectProvider<TopicConfiguration> topicsConfiguration) {
		JMSConfiguration configuration = new JMSConfigurationImpl();
		addAll(configuration.getQueueConfigurations(), queuesConfiguration);
		addAll(configuration.getTopicConfigurations(), topicsConfiguration);
		addQueues(configuration, this.properties.getEmbedded().getQueues());
		addTopics(configuration, this.properties.getEmbedded().getTopics());
		return configuration;
	}

	private <T> void addAll(List<T> list, ObjectProvider<T> items) {
		if (items != null) {
			list.addAll(items.orderedStream().collect(Collectors.toList()));
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
