/*
 * Copyright 2012-2017 the original author or authors.
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

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

import javax.activation.MimeType;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration.MailSenderCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
@Conditional(MailSenderCondition.class)
@EnableConfigurationProperties(MailProperties.class)
@Import(JndiSessionConfiguration.class)
public class MailSenderAutoConfiguration {

	private final MailProperties properties;

	private final Session session;

	public MailSenderAutoConfiguration(MailProperties properties,
			ObjectProvider<Session> session) {
		this.properties = properties;
		this.session = session.getIfAvailable();
	}

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
		PropertyMapper map = PropertyMapper.get();
		map.from(this.properties::getHost).to(sender::setHost);
		map.from(this.properties::getPort).whenNonNull().to(sender::setPort);
		map.from(this.properties::getUsername).to(sender::setUsername);
		map.from(this.properties::getPassword).to(sender::setPassword);
		map.from(this.properties::getProtocol).to(sender::setProtocol);
		map.from(this.properties::getDefaultEncoding).whenNonNull().as(Charset::name)
				.to(sender::setDefaultEncoding);
		map.from(this.properties::getProperties).whenNot(Map::isEmpty)
				.as(this::asProperties).to(sender::setJavaMailProperties);
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

		MailSenderCondition() {
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
