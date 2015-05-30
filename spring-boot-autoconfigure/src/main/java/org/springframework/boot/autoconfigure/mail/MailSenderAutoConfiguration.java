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

import java.util.Properties;

import javax.activation.MimeType;
import javax.mail.internet.MimeMessage;
import javax.mail.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration.PropertiesCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
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
@ConditionalOnMissingBean(MailSender.class)
@Conditional(PropertiesCondition.class)
@EnableConfigurationProperties(MailProperties.class)
public class MailSenderAutoConfiguration {

	@Autowired(required = false)
	private Session session;

	@Autowired
	private MailProperties properties;

	@Bean
	public JavaMailSenderImpl mailSender() {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		if (this.session != null) {
			sender.setSession(this.session);
		} else {
			sender.setHost(this.properties.getHost());
			if (this.properties.getPort() != null) {
				sender.setPort(this.properties.getPort());
			}
			sender.setUsername(this.properties.getUsername());
			sender.setPassword(this.properties.getPassword());
			sender.setDefaultEncoding(this.properties.getDefaultEncoding());
			if (!this.properties.getProperties().isEmpty()) {
				Properties properties = new Properties();
				properties.putAll(this.properties.getProperties());
				sender.setJavaMailProperties(properties);
			}
		}
		return sender;
	}

	/**
	 * Condition for jndi-name or host property
	 */
	static class PropertiesCondition extends AnyNestedCondition {

		public PropertiesCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnProperty(prefix = "spring.mail", name = "jndi-name")
		static class JndiNameProperty {
		}

		@ConditionalOnProperty(prefix = "spring.mail", name = "host")
		static class HostProperty {
		}

	}

}
