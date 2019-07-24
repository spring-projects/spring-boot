/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.Map;
import java.util.Properties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Auto-configure a {@link MailSender} based on properties configuration.
 *
 * @author Oliver Gierke
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
class MailSenderPropertiesConfiguration {

	private final MailProperties properties;

	MailSenderPropertiesConfiguration(MailProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public JavaMailSenderImpl mailSender() {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		applyProperties(sender);
		return sender;
	}

	private void applyProperties(JavaMailSenderImpl sender) {
		sender.setHost(this.properties.getHost());
		if (this.properties.getPort() != null) {
			sender.setPort(this.properties.getPort());
		}
		sender.setUsername(this.properties.getUsername());
		sender.setPassword(this.properties.getPassword());
		sender.setProtocol(this.properties.getProtocol());
		if (this.properties.getDefaultEncoding() != null) {
			sender.setDefaultEncoding(this.properties.getDefaultEncoding().name());
		}
		if (!this.properties.getProperties().isEmpty()) {
			sender.setJavaMailProperties(asProperties(this.properties.getProperties()));
		}
	}

	private Properties asProperties(Map<String, String> source) {
		Properties properties = new Properties();
		properties.putAll(source);
		return properties;
	}

}
