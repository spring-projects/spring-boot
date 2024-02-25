/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.messaging.jms.receiving.custom;

import jakarta.jms.ConnectionFactory;

import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;

/**
 * MyJmsConfiguration class.
 */
@Configuration(proxyBeanMethods = false)
public class MyJmsConfiguration {

	/**
     * Creates a custom JMS listener container factory.
     * 
     * @param configurer The default JMS listener container factory configurer.
     * @return The custom JMS listener container factory.
     */
    @Bean
	public DefaultJmsListenerContainerFactory myFactory(DefaultJmsListenerContainerFactoryConfigurer configurer) {
		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		ConnectionFactory connectionFactory = getCustomConnectionFactory();
		configurer.configure(factory, connectionFactory);
		factory.setMessageConverter(new MyMessageConverter());
		return factory;
	}

	/**
     * Returns a custom ConnectionFactory.
     *
     * @return the custom ConnectionFactory
     */
    private ConnectionFactory getCustomConnectionFactory() {
		return /**/ null;
	}

}
