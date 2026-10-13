/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.sendgrid.autoconfigure;

import com.sendgrid.Client;
import com.sendgrid.SendGrid;
import com.sendgrid.SendGridAPI;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.HttpClientBuilder;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SendGrid.
 *
 * @author Maciej Walkowiak
 * @author Patrick Bray
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass(SendGrid.class)
@ConditionalOnProperty("spring.sendgrid.api-key")
@EnableConfigurationProperties(SendGridProperties.class)
public final class SendGridAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(SendGridAPI.class)
	SendGrid sendGrid(SendGridProperties properties) {
		if (properties.getProxy() != null && properties.getProxy().getHost() != null
				&& properties.getProxy().getPort() != null) {
			HttpHost proxy = new HttpHost(properties.getProxy().getHost(), properties.getProxy().getPort());
			return new SendGrid(properties.getApiKey(), new Client(HttpClientBuilder.create().setProxy(proxy).build()));
		}
		return new SendGrid(properties.getApiKey());
	}

}
