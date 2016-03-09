/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.sendgrid;

import com.sendgrid.SendGrid;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.HttpClientBuilder;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for SendGrid.
 *
 * @author Maciej Walkowiak
 * @author Patrick Bray
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass(SendGrid.class)
@Conditional(SendGridAutoConfiguration.SendGridPropertyCondition.class)
@EnableConfigurationProperties(SendGridProperties.class)
public class SendGridAutoConfiguration {

	private final SendGridProperties properties;

	public SendGridAutoConfiguration(SendGridProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean(SendGrid.class)
	public SendGrid sendGrid() {
		SendGrid sendGrid = createSendGrid();
		if (this.properties.isProxyConfigured()) {
			HttpHost proxy = new HttpHost(this.properties.getProxy().getHost(),
					this.properties.getProxy().getPort());
			sendGrid.setClient(HttpClientBuilder.create().setProxy(proxy)
					.setUserAgent("sendgrid/" + sendGrid.getVersion() + ";java").build());
		}
		return sendGrid;
	}

	private SendGrid createSendGrid() {
		if (this.properties.getApiKey() != null) {
			return new SendGrid(this.properties.getApiKey());
		}
		return new SendGrid(this.properties.getUsername(), this.properties.getPassword());
	}

	static class SendGridPropertyCondition extends AnyNestedCondition {

		SendGridPropertyCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnProperty(prefix = "spring.sendgrid", value = "username")
		static class SendGridUserProperty {
		}

		@ConditionalOnProperty(prefix = "spring.sendgrid", value = "api-key")
		static class SendGridApiKeyProperty {
		}

	}
}
