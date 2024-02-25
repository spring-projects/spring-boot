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

package org.springframework.boot.autoconfigure.mail;

import javax.naming.NamingException;

import jakarta.mail.Session;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJndi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Auto-configure a {@link MailSender} based on a {@link Session} available on JNDI.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Session.class)
@ConditionalOnProperty(prefix = "spring.mail", name = "jndi-name")
@ConditionalOnJndi
class MailSenderJndiConfiguration {

	private final MailProperties properties;

	/**
     * Constructs a new MailSenderJndiConfiguration object with the specified MailProperties.
     * 
     * @param properties the MailProperties object containing the configuration properties for the mail sender
     */
    MailSenderJndiConfiguration(MailProperties properties) {
		this.properties = properties;
	}

	/**
     * Creates and configures a JavaMailSenderImpl instance using the provided session.
     * 
     * @param session the session to be used by the mail sender
     * @return the configured JavaMailSenderImpl instance
     */
    @Bean
	JavaMailSenderImpl mailSender(Session session) {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		sender.setDefaultEncoding(this.properties.getDefaultEncoding().name());
		sender.setSession(session);
		return sender;
	}

	/**
     * Creates a session using the JNDI lookup.
     * 
     * @return the session object
     * @throws IllegalStateException if unable to find the session in the specified JNDI location
     */
    @Bean
	@ConditionalOnMissingBean
	Session session() {
		String jndiName = this.properties.getJndiName();
		try {
			return JndiLocatorDelegate.createDefaultResourceRefLocator().lookup(jndiName, Session.class);
		}
		catch (NamingException ex) {
			throw new IllegalStateException(String.format("Unable to find Session in JNDI location %s", jndiName), ex);
		}
	}

}
