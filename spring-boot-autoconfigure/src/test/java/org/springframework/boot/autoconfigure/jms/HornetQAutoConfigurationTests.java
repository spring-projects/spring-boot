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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.hornetq.jms.server.config.JMSConfiguration;
import org.hornetq.jms.server.config.JMSQueueConfiguration;
import org.hornetq.jms.server.config.TopicConfiguration;
import org.hornetq.jms.server.config.impl.JMSConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSQueueConfigurationImpl;
import org.hornetq.jms.server.config.impl.TopicConfigurationImpl;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.SessionCallback;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

/**
 * @author Stephane Nicoll
 */
public class HornetQAutoConfigurationTests {

	@Rule
	public final TemporaryFolder folder = new TemporaryFolder();

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void nettyConnectionFactory() {
		this.context = createContext(EmptyConfiguration.class);
		this.context.refresh();

		HornetQProperties hornetQProperties = context.getBean(HornetQProperties.class);
		assertEquals(HornetQMode.netty, hornetQProperties.getMode());

		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		HornetQConnectionFactory connectionFactory = this.context
				.getBean(HornetQConnectionFactory.class);
		assertEquals(jmsTemplate.getConnectionFactory(), connectionFactory);
		assertNettyConnectionFactory(connectionFactory, "localhost", 5445);
	}

	@Test
	public void nettyConnectionFactoryCustomHost() {
		this.context = createContext(EmptyConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.hornetq.host:192.168.1.144", "spring.hornetq.port:9876");
		this.context.refresh();

		HornetQConnectionFactory connectionFactory = this.context
				.getBean(HornetQConnectionFactory.class);
		assertNettyConnectionFactory(connectionFactory, "192.168.1.144", 9876);
	}

	@Test
	public void embeddedConnectionFactory() {
		this.context = createContext(EmptyConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.hornetq.mode:embedded");
		this.context.refresh();

		HornetQProperties hornetQProperties = context.getBean(HornetQProperties.class);
		assertEquals(HornetQMode.embedded, hornetQProperties.getMode());


		assertEquals(1, context.getBeansOfType(SpringEmbeddedHornetQ.class).size());
		org.hornetq.core.config.Configuration configuration =
				context.getBean(org.hornetq.core.config.Configuration.class);
		assertFalse("Persistence disabled by default", configuration.isPersistenceEnabled());
		assertFalse("Security disabled by default", configuration.isSecurityEnabled());

		HornetQConnectionFactory connectionFactory = this.context
				.getBean(HornetQConnectionFactory.class);
		assertInVmConnectionFactory(connectionFactory);
	}

	@Test
	public void embeddedServerWithDestinations() {
		this.context = createContext(EmptyConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.hornetq.mode:embedded",
				"spring.hornetq.embedded.queues=Queue1,Queue2",
				"spring.hornetq.embedded.topics=Topic1");
		this.context.refresh();

		DestinationChecker checker = new DestinationChecker(this.context);
		checker.checkQueue("Queue1", true);
		checker.checkQueue("Queue2", true);
		checker.checkQueue("QueueDoesNotExist", false);

		checker.checkTopic("Topic1", true);
		checker.checkTopic("TopicDoesNotExist", false);
	}

	@Test
	public void embeddedServerWithDestinationConfig() {
		this.context = createContext(DestinationConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.hornetq.mode:embedded");
		this.context.refresh();

		DestinationChecker checker = new DestinationChecker(this.context);
		checker.checkQueue("sampleQueue", true);
		checker.checkTopic("sampleTopic", true);
	}

	@Test
	public void embeddedServiceWithCustomJmsConfiguration() {
		this.context = createContext(CustomJmsConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.hornetq.mode:embedded",
				"spring.hornetq.embedded.queues=Queue1,Queue2"); // Ignored with custom config
		this.context.refresh();
		DestinationChecker checker = new DestinationChecker(this.context);
		checker.checkQueue("custom", true); // See CustomJmsConfiguration

		checker.checkQueue("Queue1", false);
		checker.checkQueue("Queue2", false);
	}

	@Test
	public void embeddedServiceWithCustomHornetQConfiguration() {
		this.context = createContext(CustomHornetQConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.hornetq.mode:embedded");
		this.context.refresh();
		org.hornetq.core.config.Configuration configuration =
				context.getBean(org.hornetq.core.config.Configuration.class);
		assertEquals("customFooBar", configuration.getName());
	}

	@Test
	public void embeddedWithPersistentMode() throws IOException, JMSException {
		File dataFolder = folder.newFolder();

		// Start the server and post a message to some queue
		this.context = createContext(EmptyConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.hornetq.mode:embedded",
				"spring.hornetq.embedded.queues=TestQueue",
				"spring.hornetq.embedded.persistent:true",
				"spring.hornetq.embedded.dataDirectory:" + dataFolder.getAbsolutePath());
		this.context.refresh();

		final String msgId = UUID.randomUUID().toString();
		JmsTemplate jmsTemplate = this.context.getBean(JmsTemplate.class);
		jmsTemplate.send("TestQueue", new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage(msgId);
			}
		});
		this.context.close(); // Shutdown the broker

		// Start the server again and check if our message is still here
		this.context = createContext(EmptyConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.hornetq.mode:embedded",
				"spring.hornetq.embedded.queues=TestQueue",
				"spring.hornetq.embedded.persistent:true",
				"spring.hornetq.embedded.dataDirectory:" + dataFolder.getAbsolutePath());
		this.context.refresh();

		JmsTemplate jmsTemplate2 = this.context.getBean(JmsTemplate.class);
		jmsTemplate2.setReceiveTimeout(1000L);
		Message message = jmsTemplate2.receive("TestQueue");
		assertNotNull("No message on persistent queue", message);
		assertEquals("Invalid message received on queue", msgId, ((TextMessage) message).getText());
	}


	private TransportConfiguration assertInVmConnectionFactory(HornetQConnectionFactory connectionFactory) {
		TransportConfiguration transportConfig = getSingleTransportConfiguration(connectionFactory);
		assertEquals(InVMConnectorFactory.class.getName(), transportConfig.getFactoryClassName());
		return transportConfig;
	}

	private TransportConfiguration assertNettyConnectionFactory(HornetQConnectionFactory connectionFactory,
			String host, int port) {
		TransportConfiguration transportConfig = getSingleTransportConfiguration(connectionFactory);
		assertEquals(NettyConnectorFactory.class.getName(), transportConfig.getFactoryClassName());
		assertEquals(host, transportConfig.getParams().get("host"));
		assertEquals(port, transportConfig.getParams().get("port"));
		return transportConfig;
	}

	private TransportConfiguration getSingleTransportConfiguration(HornetQConnectionFactory connectionFactory) {
		TransportConfiguration[] transportConfigurations =
				connectionFactory.getServerLocator().getStaticTransportConfigurations();
		assertEquals(1, transportConfigurations.length);
		return transportConfigurations[0];
	}


	private AnnotationConfigApplicationContext createContext(
			Class<?>... additionalClasses) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(additionalClasses);
		context.register(HornetQAutoConfiguration.class,
				JmsTemplateAutoConfiguration.class);
		return context;
	}

	private static class DestinationChecker {

		private final JmsTemplate jmsTemplate;

		private final DestinationResolver destinationResolver;

		private DestinationChecker(ApplicationContext applicationContext) {
			this.jmsTemplate = applicationContext.getBean(JmsTemplate.class);
			this.destinationResolver = new DynamicDestinationResolver();
		}

		public void checkQueue(String name, boolean shouldExist) {
			checkDestination(name, false, shouldExist);
		}

		public void checkTopic(String name, boolean shouldExist) {
			checkDestination(name, true, shouldExist);
		}

		public void checkDestination(final String name, final boolean pubSub, final boolean shouldExist) {
			jmsTemplate.execute(new SessionCallback<Void>() {
				@Override
				public Void doInJms(Session session) throws JMSException {
					try {
						Destination destination = destinationResolver.resolveDestinationName(session, name, pubSub);
						if (!shouldExist) {
							throw new IllegalStateException("Destination '" + name
									+ "' was not expected but got " + destination);
						}
					}
					catch (JMSException e) {
						if (shouldExist) {
							throw new IllegalStateException("Destination '" + name
									+ "' was expected but got " + e.getMessage());
						}
					}
					return null;
				}
			});
		}
	}

	@Configuration
	protected static class EmptyConfiguration {
	}

	@Configuration
	protected static class DestinationConfiguration {

		@Bean
		JMSQueueConfiguration sampleQueueConfiguration() {
			return new JMSQueueConfigurationImpl("sampleQueue", "foo=bar", false, "/queue/1");
		}

		@Bean
		TopicConfiguration sampleTopicConfiguration() {
			return new TopicConfigurationImpl("sampleTopic", "/topic/1");
		}
	}

	@Configuration
	protected static class CustomJmsConfiguration {

		@Bean
		public JMSConfiguration myJmsConfiguration() {
			JMSConfiguration config = new JMSConfigurationImpl();
			config.getQueueConfigurations().add(new JMSQueueConfigurationImpl("custom", null, false));
			return config;
		}
	}

	@Configuration
	protected static class CustomHornetQConfiguration {

		@Autowired
		private HornetQProperties properties;

		@Bean
		public org.hornetq.core.config.Configuration myHornetQConfiguration() {
			ConfigurationImpl config = new ConfigurationImpl();
			properties.getEmbedded().configure(config);

			// This is the real customization
			config.setClusterPassword("Foobar");
			config.setName("customFooBar");

			return config;
		}
	}

}
