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

package org.springframework.boot.autoconfigure.elasticsearch.rest;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Elasticsearch REST clients.
 *
 * @author Brian Clozel
 * @since 2.1.0
 */
@Configuration
@ConditionalOnClass(RestClient.class)
@EnableConfigurationProperties(RestClientProperties.class)
public class RestClientAutoConfiguration {

	private final RestClientProperties properties;

	private final ObjectProvider<RestClientBuilderCustomizer> builderCustomizers;

	public RestClientAutoConfiguration(RestClientProperties properties,
			ObjectProvider<RestClientBuilderCustomizer> builderCustomizers) {
		this.properties = properties;
		this.builderCustomizers = builderCustomizers;
	}

	@Bean
	@ConditionalOnMissingBean
	public RestClient restClient(RestClientBuilder builder) {
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public RestClientBuilder restClientBuilder() {
		HttpHost[] hosts = this.properties.getUris().stream().map(HttpHost::create)
				.toArray(HttpHost[]::new);
		RestClientBuilder builder = RestClient.builder(hosts);
		PropertyMapper map = PropertyMapper.get();
		map.from(this.properties::getUsername).whenHasText().to((username) -> {
			CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
			Credentials credentials = new UsernamePasswordCredentials(
					this.properties.getUsername(), this.properties.getPassword());
			credentialsProvider.setCredentials(AuthScope.ANY, credentials);
			builder.setHttpClientConfigCallback((httpClientBuilder) -> httpClientBuilder
					.setDefaultCredentialsProvider(credentialsProvider));
		});
		this.builderCustomizers.orderedStream()
				.forEach((customizer) -> customizer.customize(builder));
		return builder;
	}

	@Configuration
	@ConditionalOnClass(RestHighLevelClient.class)
	public static class RestHighLevelClientConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public RestHighLevelClient restHighLevelClient(
				RestClientBuilder restClientBuilder) {
			return new RestHighLevelClient(restClientBuilder);
		}

	}

}
