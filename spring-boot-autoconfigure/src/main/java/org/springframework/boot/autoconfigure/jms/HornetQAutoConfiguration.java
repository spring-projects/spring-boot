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

package org.springframework.boot.autoconfigure.jms;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration}
 * to integrate with an HornetQ broker. Connect by default to a broker available on the
 * local machine with the default settings. If the necessary classes are present, the broker
 * can also be embedded in the application itself.
 *
 * @author Stephane Nicoll
 * @since 1.1
 */
@Configuration
@AutoConfigureBefore(JmsTemplateAutoConfiguration.class)
@ConditionalOnClass(ConnectionFactory.class)
@ConditionalOnMissingBean(ConnectionFactory.class)
public class HornetQAutoConfiguration {

	@Configuration
	@ConditionalOnClass({NettyConnectorFactory.class, HornetQJMSClient.class})
	@ConditionalOnExpression("'${spring.hornetq.mode:netty}' == 'netty'")
	protected static class NettyConnection {

		@Configuration
		@EnableConfigurationProperties(HornetQProperties.class)
		static class HornetQNettyConfiguration {

			@Autowired
			private HornetQProperties properties;

			@Bean
			public ConnectionFactory jmsConnectionFactory() {
				Map<String, Object> connectionParams = new HashMap<String, Object>();
				connectionParams.put(TransportConstants.HOST_PROP_NAME, properties.getHost());
				connectionParams.put(TransportConstants.PORT_PROP_NAME, properties.getPort());
				TransportConfiguration transportConfiguration =
						new TransportConfiguration(NettyConnectorFactory.class.getName(), connectionParams);
				return HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF, transportConfiguration);
			}

		}
	}

	@Configuration
	@ConditionalOnClass({InVMConnectorFactory.class, EmbeddedJMS.class})
	@ConditionalOnExpression("'${spring.hornetq.mode:netty}' == 'embedded'")
	protected static class EmbeddedConnection {

		@Configuration
		@EnableConfigurationProperties(HornetQProperties.class)
		static class HornetQEmbeddedConfiguration {

			@Autowired
			private HornetQProperties properties;

			@Autowired(required = false)
			private Collection<JMSQueueConfiguration> queuesConfiguration;

			@Autowired(required = false)
			private Collection<TopicConfiguration> topicsConfiguration;

			@Bean
			public ConnectionFactory jmsConnectionFactory() {
				ServerLocator serverLocator = HornetQClient.createServerLocatorWithoutHA(
						new TransportConfiguration(InVMConnectorFactory.class.getName()));
				return new HornetQConnectionFactory(serverLocator);
			}

			@Bean(initMethod = "start", destroyMethod = "stop")
			@ConditionalOnMissingBean(EmbeddedJMS.class)
			public SpringEmbeddedHornetQ hornetQServer(org.hornetq.core.config.Configuration hornetQConfiguration,
					JMSConfiguration hornetQJmsConfiguration) {
				SpringEmbeddedHornetQ bootstrap = new SpringEmbeddedHornetQ();
				bootstrap.setConfiguration(hornetQConfiguration);
				bootstrap.setJmsConfiguration(hornetQJmsConfiguration);
				return bootstrap;
			}

			@Bean
			@ConditionalOnMissingBean(org.hornetq.core.config.Configuration.class)
			public org.hornetq.core.config.Configuration hornetQConfiguration() {
				ConfigurationImpl configuration = new ConfigurationImpl();

				configuration.setSecurityEnabled(false);

				properties.getEmbedded().configure(configuration);

				configuration.getAcceptorConfigurations().add(
						new TransportConfiguration(InVMAcceptorFactory.class.getName()));

				// https://issues.jboss.org/browse/HORNETQ-1143
				configuration.setClusterPassword("SpringBootRules");
				return configuration;
			}

			@Bean
			@ConditionalOnMissingBean(JMSConfiguration.class)
			public JMSConfiguration hornetQJmsConfiguration() {
				JMSConfiguration jmsConfig = new JMSConfigurationImpl();

				if (this.queuesConfiguration != null) {
					jmsConfig.getQueueConfigurations().addAll(queuesConfiguration);
				}
				if (this.topicsConfiguration != null) {
					jmsConfig.getTopicConfigurations().addAll(this.topicsConfiguration);
				}

				for (String queue : properties.getEmbedded().getQueues()) {
					JMSQueueConfiguration queueConfig = createSimpleQueueConfiguration(queue);
					jmsConfig.getQueueConfigurations().add(queueConfig);
				}
				for (String topic : properties.getEmbedded().getTopics()) {
					TopicConfiguration topicConfig = createSimpleTopicConfiguration(topic);
					jmsConfig.getTopicConfigurations().add(topicConfig);
				}

				return jmsConfig;
			}


			private JMSQueueConfiguration createSimpleQueueConfiguration(String name) {
				return new JMSQueueConfigurationImpl(name, null,
						this.properties.getEmbedded().isPersistent(), "/queue/" + name);
			}

			private TopicConfiguration createSimpleTopicConfiguration(String name) {
				return new TopicConfigurationImpl(name, "/topic/" + name);
			}

		}

	}
}
