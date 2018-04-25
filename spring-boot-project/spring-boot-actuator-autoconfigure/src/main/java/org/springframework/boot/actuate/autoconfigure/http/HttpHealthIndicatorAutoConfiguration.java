/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.http;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthIndicatorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorAutoConfiguration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.http.HttpHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HttpHealthIndicator}.
 *
 * @author Harry Martland
 * @since 2.1.0
 */
@Configuration
@ConditionalOnEnabledHealthIndicator("http")
@AutoConfigureBefore(HealthIndicatorAutoConfiguration.class)
@EnableConfigurationProperties(HttpHealthIndicatorProperties.class)
public class HttpHealthIndicatorAutoConfiguration
		extends CompositeHealthIndicatorConfiguration<HttpHealthIndicator, String>
		implements InitializingBean {

	private final HttpClient httpClient;
	private final HttpHealthIndicatorProperties httpHealthIndicatorProperties;

	public HttpHealthIndicatorAutoConfiguration(
			HttpHealthIndicatorProperties properties) {

		this.httpClient = HttpClientBuilder.create()
				.setDefaultRequestConfig(RequestConfig.custom()
						.setConnectionRequestTimeout(
								(int) properties.getConnectionRequestTimeout().toMillis())
						.setSocketTimeout((int) properties.getSocketTimeout().toMillis())
						.setConnectTimeout(
								(int) properties.getConnectionRequestTimeout().toMillis())
						.build())
				.build();

		this.httpHealthIndicatorProperties = properties;
	}

	@Bean
	@ConditionalOnProperty("management.health.http.urls")
	@ConditionalOnMissingBean(name = "httpHealthIndicator")
	public HealthIndicator httpHealthIndicator() {
		return createHealthIndicator();
	}

	@Bean
	@ConditionalOnProperty("management.health.http.urls[0]")
	@ConditionalOnMissingBean(name = "httpHealthIndicator")
	public HealthIndicator httpHealthIndicatorArray() {
		return createHealthIndicator();
	}

	private HealthIndicator createHealthIndicator() {
		return createHealthIndicator(urlsToMap());
	}

	private Map<String, String> urlsToMap() {
		return this.httpHealthIndicatorProperties.getUrls().stream()
				.collect(Collectors.toMap(url -> url, url -> url));
	}

	@Override
	protected HttpHealthIndicator createHealthIndicator(String url) {
		return new HttpHealthIndicator(this.httpClient, url);
	}

	@Override
	public void afterPropertiesSet() {
	}
}
