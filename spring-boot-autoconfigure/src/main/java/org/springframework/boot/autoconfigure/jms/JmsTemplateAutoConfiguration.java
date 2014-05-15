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

import javax.jms.ConnectionFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link JmsTemplate}.
 * 
 * @author Greg Turnquist
 */
@Configuration
@ConditionalOnClass(JmsTemplate.class)
@ConditionalOnBean(ConnectionFactory.class)
@EnableConfigurationProperties(JmsTemplateProperties.class)
public class JmsTemplateAutoConfiguration {

	@Autowired
	private JmsTemplateProperties properties;

	@Autowired
	private ConnectionFactory connectionFactory;

	@Bean
	@ConditionalOnMissingBean(JmsTemplate.class)
	public JmsTemplate jmsTemplate() {
		JmsTemplate jmsTemplate = new JmsTemplate(this.connectionFactory);
		jmsTemplate.setPubSubDomain(this.properties.isPubSubDomain());
		return jmsTemplate;
	}

}
