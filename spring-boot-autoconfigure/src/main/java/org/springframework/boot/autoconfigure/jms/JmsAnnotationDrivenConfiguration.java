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

package org.springframework.boot.autoconfigure.jms;

import javax.jms.ConnectionFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJndi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerConfigUtils;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.JndiDestinationResolver;

/**
 * Configuration for Spring 4.1 annotation driven JMS.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 1.2.0
 */
@Configuration
@ConditionalOnClass(EnableJms.class)
class JmsAnnotationDrivenConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public JmsListenerContainerFactoryConfigurer jmsListenerContainerFactoryConfigurer() {
		return new JmsListenerContainerFactoryConfigurer();
	}

	@Bean
	@ConditionalOnMissingBean(name = "jmsListenerContainerFactory")
	public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
			JmsListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
		return configurer.createJmsListenerContainerFactory(connectionFactory);
	}

	@EnableJms
	@ConditionalOnMissingBean(name = JmsListenerConfigUtils.JMS_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	protected static class EnableJmsConfiguration {
	}

	@ConditionalOnJndi
	protected static class JndiConfiguration {

		@Bean
		@ConditionalOnMissingBean(DestinationResolver.class)
		public JndiDestinationResolver destinationResolver() {
			JndiDestinationResolver resolver = new JndiDestinationResolver();
			resolver.setFallbackToDynamicDestination(true);
			return resolver;
		}

	}

}
