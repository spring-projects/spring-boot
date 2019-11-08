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

package org.springframework.boot.autoconfigure.data.elasticsearch;

import reactor.netty.http.client.HttpClient;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Elasticsearch Reactive REST
 * clients.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ ReactiveRestClients.class, WebClient.class, HttpClient.class })
@EnableConfigurationProperties(ReactiveRestClientProperties.class)
public class ReactiveRestClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public ClientConfiguration clientConfiguration(ReactiveRestClientProperties properties) {
		ClientConfiguration.MaybeSecureClientConfigurationBuilder builder = ClientConfiguration.builder()
				.connectedTo(properties.getEndpoints().toArray(new String[0]));
		if (properties.isUseSsl()) {
			builder.usingSsl();
		}
		configureTimeouts(builder, properties);
		return builder.build();
	}

	private void configureTimeouts(ClientConfiguration.TerminalClientConfigurationBuilder builder,
			ReactiveRestClientProperties properties) {
		PropertyMapper map = PropertyMapper.get();
		map.from(properties.getConnectionTimeout()).whenNonNull().to(builder::withConnectTimeout);
		map.from(properties.getSocketTimeout()).whenNonNull().to(builder::withSocketTimeout);
		map.from(properties.getUsername()).whenHasText().to((username) -> {
			HttpHeaders headers = new HttpHeaders();
			headers.setBasicAuth(username, properties.getPassword());
			builder.withDefaultHeaders(headers);
		});
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactiveElasticsearchClient reactiveElasticsearchClient(ClientConfiguration clientConfiguration) {
		return ReactiveRestClients.create(clientConfiguration);
	}

}
