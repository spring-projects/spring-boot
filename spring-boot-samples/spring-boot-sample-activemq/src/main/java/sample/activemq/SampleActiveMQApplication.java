/*
 * Copyright 2012-2015 the original author or authors.
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

package sample.activemq;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.jms.Topic;

import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;

@SpringBootApplication
@EnableJms
public class SampleActiveMQApplication {
	
	private static final Logger LOG = LoggerFactory.getLogger(SampleActiveMQApplication.class);
	
	@Bean
	public Queue testQueue1() {
		return new ActiveMQQueue("TestQueue1");
	}

	@Bean
	public Topic testTopic1() {
		return new ActiveMQTopic("TestTopic1");
	}
	
	@Autowired
	protected ConnectionFactory activeMQConnectionFactory;

	@Bean
	public JmsListenerContainerFactory<?> jmsListenerContainerTopic() {
		DefaultJmsListenerContainerFactory bean = new DefaultJmsListenerContainerFactory();
		bean.setPubSubDomain(true);
		bean.setConnectionFactory(activeMQConnectionFactory);
		return bean;
	}
	
	@Bean
	public JmsListenerContainerFactory<?> jmsListenerContainerQueue() {
		DefaultJmsListenerContainerFactory bean = new DefaultJmsListenerContainerFactory();
		bean.setConnectionFactory(activeMQConnectionFactory);
		return bean;
	}
	
	public static void main(String[] args) {
		LOG.trace("test log TRACE");
		LOG.debug("test log DEBUG");
		LOG.info("test log INFO");
		
		SpringApplication.run(SampleActiveMQApplication.class, args);
	}

}
