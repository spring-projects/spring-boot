/*
 * Copyright 2012-2013 the original author or authors.
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

package sample.hornetq;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class SampleHornetQApplication {

	@Autowired
	private ConnectionFactory connectionFactory;

	@Bean
	public ScheduledAnnotationBeanPostProcessor scheduledAnnotationBeanPostProcessor() {
		return new ScheduledAnnotationBeanPostProcessor();
	}

	@Bean
	public DefaultMessageListenerContainer messageListener() {
		DefaultMessageListenerContainer container = new DefaultMessageListenerContainer();
		container.setConnectionFactory(this.connectionFactory);
		container.setDestinationName("testQueue");
		container.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(Message message) {
				try {
					System.out.println(message.getBody(Object.class));
				}
				catch (JMSException ex) {
					ex.printStackTrace();
				}
			}
		});
		return container;
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleHornetQApplication.class, args);
	}
}
