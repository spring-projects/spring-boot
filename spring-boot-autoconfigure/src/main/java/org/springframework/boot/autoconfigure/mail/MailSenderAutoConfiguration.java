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

import java.util.Map;
import java.util.Properties;
import javax.activation.MimeType;
import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.naming.NamingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJndi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * {@link EnableAutoConfiguration Auto configuration} for email support.
 *
 * @author Oliver Gierke
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @since 1.2.0
 */
@Configuration
@ConditionalOnClass({ MimeMessage.class, MimeType.class })
@EnableConfigurationProperties(MailProperties.class)
public class MailSenderAutoConfiguration {

	@Configuration
	@ConditionalOnClass(Session.class)
	@ConditionalOnProperty(prefix = "spring.mail", name = "jndi-name")
	@ConditionalOnJndi
	static class JndiSessionConfiguration {

		@Autowired
		private MailProperties properties;

		@Bean
		@ConditionalOnMissingBean
		public Session session() {
			String jndiName = this.properties.getJndiName();
			try {
				return new JndiLocatorDelegate().lookup(jndiName, Session.class);
			}
			catch (NamingException ex) {
				throw new IllegalStateException(String.format(
						"Unable to find Session in JNDI location %s", jndiName), ex);
			}
		}

	}

	@ConditionalOnMissingBean(MailSender.class)
	@Conditional(MailSenderConfiguration.MailSenderCondition.class)
	static class MailSenderConfiguration {

		@Autowired
		private MailProperties properties;

		@Autowired(required = false)
		private Session session;

		@Bean
		public JavaMailSenderImpl mailSender() {
			JavaMailSenderImpl sender = new JavaMailSenderImpl();
			if (this.session != null) {
				sender.setSession(this.session);
			}
			else {
				applyProperties(sender);
			}
			return sender;
		}

		private void applyProperties(JavaMailSenderImpl sender) {
			sender.setHost(this.properties.getHost());
			if (this.properties.getPort() != null) {
				sender.setPort(this.properties.getPort());
			}
			sender.setUsername(this.properties.getUsername());
			sender.setPassword(this.properties.getPassword());
			sender.setDefaultEncoding(this.properties.getDefaultEncoding());
			if (!this.properties.getProperties().isEmpty()) {
				sender.setJavaMailProperties(asProperties(this.properties.getProperties()));
			}
		}

		private Properties asProperties(Map<String, String> source) {
			Properties properties = new Properties();
			properties.putAll(source);
			return properties;
		}

		/**
		 * Condition to trigger the creation of a {@link JavaMailSenderImpl}. This kicks in if
		 * either the host or jndi name property is set.
		 */
		static class MailSenderCondition extends AnyNestedCondition {

			public MailSenderCondition() {
				super(ConfigurationPhase.PARSE_CONFIGURATION);
			}

			@ConditionalOnProperty(prefix = "spring.mail", name = "host")
			static class HostProperty {
			}

			@ConditionalOnProperty(prefix = "spring.mail", name = "jndi-name")
			static class JndiNameProperty {
			}

		}
	}

	@Configuration
	@ConditionalOnSingleCandidate(JavaMailSenderImpl.class)
	static class MailSenderValidator {

		@Autowired
		private MailProperties properties;

		@Autowired
		private JavaMailSenderImpl mailSender;

		@PostConstruct
		public void validateConnection() {
			if (this.properties.isTestConnection()) {
				try {
					this.mailSender.testConnection();
				}
				catch (MessagingException ex) {
					throw new IllegalStateException("Mail server is not unavailable", ex);
				}
			}
		}
	}

}
