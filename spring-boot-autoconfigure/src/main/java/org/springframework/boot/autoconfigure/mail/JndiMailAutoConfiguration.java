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

package org.springframework.boot.autoconfigure.mail;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJndi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jndi.JndiLocatorDelegate;

import javax.mail.Session;
import javax.naming.NamingException;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Mail provided from JNDI.
 *
 * @author Eddú Meléndez
 * @since 1.3.0
 */
@Configuration
@AutoConfigureBefore(MailSenderAutoConfiguration.class)
@ConditionalOnClass(Session.class)
@ConditionalOnMissingBean(Session.class)
@ConditionalOnProperty(prefix = "spring.mail", name = "jndi-name")
@EnableConfigurationProperties(MailProperties.class)
@ConditionalOnJndi
public class JndiMailAutoConfiguration {

	@Autowired
	private MailProperties properties;

	@Bean
	@ConditionalOnMissingBean
	public Session session() {
		try {
			return new JndiLocatorDelegate()
				.lookup(this.properties.getJndiName(), Session.class);
		} catch (NamingException e) {

		}

		throw new IllegalStateException(String
			.format("Unable to find Session in JNDI location %s", this.properties.getJndiName()));
	}

}
