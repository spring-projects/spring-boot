/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.elasticsearch;

import java.time.Duration;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch rest client infrastructure configurations.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
class ElasticsearchRestClientConfigurations {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(RestClientBuilder.class)
	static class RestClientBuilderConfiguration {

		@Bean
		RestClientBuilderCustomizer defaultRestClientBuilderCustomizer(ElasticsearchRestClientProperties properties) {
			return new DefaultRestClientBuilderCustomizer(properties);
		}

		@Bean
		RestClientBuilder elasticsearchRestClientBuilder(ElasticsearchRestClientProperties properties,
				ObjectProvider<RestClientBuilderCustomizer> builderCustomizers) {
			HttpHost[] hosts = properties.getUris().stream().map(HttpHost::create).toArray(HttpHost[]::new);
			RestClientBuilder builder = RestClient.builder(hosts);
			builder.setHttpClientConfigCallback((httpClientBuilder) -> {
				builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(httpClientBuilder));
				return httpClientBuilder;
			});
			builder.setRequestConfigCallback((requestConfigBuilder) -> {
				builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(requestConfigBuilder));
				return requestConfigBuilder;
			});
			builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(RestHighLevelClient.class)
	static class RestHighLevelClientConfiguration {

		@Bean
		@ConditionalOnMissingBean
		RestHighLevelClient elasticsearchRestHighLevelClient(RestClientBuilder restClientBuilder) {
			return new RestHighLevelClient(restClientBuilder);
		}

		@Bean
		@ConditionalOnMissingBean
		RestClient elasticsearchRestClient(RestClientBuilder builder,
				ObjectProvider<RestHighLevelClient> restHighLevelClient) {
			RestHighLevelClient client = restHighLevelClient.getIfUnique();
			if (client != null) {
				return client.getLowLevelClient();
			}
			return builder.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RestClientFallbackConfiguration {

		@Bean
		@ConditionalOnMissingBean
		RestClient elasticsearchRestClient(RestClientBuilder builder) {
			return builder.build();
		}

	}

	static class DefaultRestClientBuilderCustomizer implements RestClientBuilderCustomizer {

		private static final PropertyMapper map = PropertyMapper.get();

		private final ElasticsearchRestClientProperties properties;

		DefaultRestClientBuilderCustomizer(ElasticsearchRestClientProperties properties) {
			this.properties = properties;
		}

		@Override
		public void customize(RestClientBuilder builder) {
		}

		@Override
		public void customize(HttpAsyncClientBuilder builder) {
			map.from(this.properties::getUsername).whenHasText().to((username) -> {
				CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
				Credentials credentials = new UsernamePasswordCredentials(this.properties.getUsername(),
						this.properties.getPassword());
				credentialsProvider.setCredentials(AuthScope.ANY, credentials);
				builder.setDefaultCredentialsProvider(credentialsProvider);
			});
		}

		@Override
		public void customize(RequestConfig.Builder builder) {
			map.from(this.properties::getConnectionTimeout).whenNonNull().asInt(Duration::toMillis)
					.to(builder::setConnectTimeout);
			map.from(this.properties::getReadTimeout).whenNonNull().asInt(Duration::toMillis)
					.to(builder::setSocketTimeout);
		}

	}

}
