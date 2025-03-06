/*
 * Copyright 2012-2025 the original author or authors.
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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mail.MailProperties.Ssl;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

/**
 * Auto-configure a {@link MailSender} based on properties configuration.
 *
 * @author Oliver Gierke
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("spring.mail.host")
class MailSenderPropertiesConfiguration {

	@Bean
	@ConditionalOnMissingBean(JavaMailSender.class)
	JavaMailSenderImpl mailSender(MailProperties properties, ObjectProvider<SslBundles> sslBundles) {
		JavaMailSenderImpl sender = new JavaMailSenderImpl();
		applyProperties(properties, sender, sslBundles.getIfAvailable());
		return sender;
	}

	private void applyProperties(MailProperties properties, JavaMailSenderImpl sender, SslBundles sslBundles) {
		sender.setHost(properties.getHost());
		if (properties.getPort() != null) {
			sender.setPort(properties.getPort());
		}
		sender.setUsername(properties.getUsername());
		sender.setPassword(properties.getPassword());
		sender.setProtocol(properties.getProtocol());
		if (properties.getDefaultEncoding() != null) {
			sender.setDefaultEncoding(properties.getDefaultEncoding().name());
		}
		Properties javaMailProperties = asProperties(properties.getProperties());
		String protocol = properties.getProtocol();
		protocol = (!StringUtils.hasLength(protocol)) ? "smtp" : protocol;
		Ssl ssl = properties.getSsl();
		if (ssl.isEnabled()) {
			javaMailProperties.setProperty("mail." + protocol + ".ssl.enable", "true");
		}
		if (ssl.getBundle() != null) {
			SslBundle sslBundle = sslBundles.getBundle(ssl.getBundle());
			javaMailProperties.put("mail." + protocol + ".ssl.socketFactory",
					sslBundle.createSslContext().getSocketFactory());
		}
		if (!javaMailProperties.isEmpty()) {
			sender.setJavaMailProperties(javaMailProperties);
		}
	}

	private Properties asProperties(Map<String, String> source) {
		Properties properties = new Properties();
		properties.putAll(source);
		return properties;
	}

}
