/*
 * Copyright 2012-2021 the original author or authors.
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

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.sniff.Sniffer;
import org.elasticsearch.client.sniff.SnifferBuilder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Elasticsearch rest client configurations.
 *
 * @author Stephane Nicoll
 */
class ElasticsearchRestClientConfigurations {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(RestClientBuilder.class)
	static class RestClientBuilderConfiguration {

		private final ConsolidatedProperties properties;

		@SuppressWarnings("deprecation")
		RestClientBuilderConfiguration(ElasticsearchProperties properties,
				DeprecatedElasticsearchRestClientProperties deprecatedProperties) {
			this.properties = new ConsolidatedProperties(properties, deprecatedProperties);
		}

		@Bean
		RestClientBuilderCustomizer defaultRestClientBuilderCustomizer() {
			return new DefaultRestClientBuilderCustomizer(this.properties);
		}

		@Bean
		RestClientBuilder elasticsearchRestClientBuilder(
				ObjectProvider<RestClientBuilderCustomizer> builderCustomizers) {
			HttpHost[] hosts = this.properties.getUris().stream().map(this::createHttpHost).toArray(HttpHost[]::new);
			RestClientBuilder builder = RestClient.builder(hosts);
			builder.setHttpClientConfigCallback((httpClientBuilder) -> {
				builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(httpClientBuilder));
				return httpClientBuilder;
			});
			builder.setRequestConfigCallback((requestConfigBuilder) -> {
				builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(requestConfigBuilder));
				return requestConfigBuilder;
			});
			if (this.properties.getPathPrefix() != null) {
				builder.setPathPrefix(this.properties.properties.getPathPrefix());
			}
			builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder;
		}

		private HttpHost createHttpHost(String uri) {
			try {
				return createHttpHost(URI.create(uri));
			}
			catch (IllegalArgumentException ex) {
				return HttpHost.create(uri);
			}
		}

		private HttpHost createHttpHost(URI uri) {
			if (!StringUtils.hasLength(uri.getUserInfo())) {
				return HttpHost.create(uri.toString());
			}
			try {
				return HttpHost.create(new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(),
						uri.getQuery(), uri.getFragment()).toString());
			}
			catch (URISyntaxException ex) {
				throw new IllegalStateException(ex);
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(RestHighLevelClient.class)
	@ConditionalOnMissingBean({ RestHighLevelClient.class, RestClient.class })
	static class RestHighLevelClientConfiguration {

		@Bean
		RestHighLevelClient elasticsearchRestHighLevelClient(RestClientBuilder restClientBuilder) {
			return new RestHighLevelClient(restClientBuilder);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Sniffer.class)
	@ConditionalOnSingleCandidate(RestHighLevelClient.class)
	static class RestClientSnifferConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@SuppressWarnings("deprecation")
		Sniffer elasticsearchSniffer(RestHighLevelClient client, ElasticsearchRestClientProperties properties,
				DeprecatedElasticsearchRestClientProperties deprecatedProperties) {
			SnifferBuilder builder = Sniffer.builder(client.getLowLevelClient());
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			Duration interval = deprecatedProperties.isCustomized() ? deprecatedProperties.getSniffer().getInterval()
					: properties.getSniffer().getInterval();
			map.from(interval).asInt(Duration::toMillis).to(builder::setSniffIntervalMillis);
			Duration delayAfterFailure = deprecatedProperties.isCustomized()
					? deprecatedProperties.getSniffer().getDelayAfterFailure()
					: properties.getSniffer().getDelayAfterFailure();
			map.from(delayAfterFailure).asInt(Duration::toMillis).to(builder::setSniffAfterFailureDelayMillis);
			return builder.build();
		}

	}

	static class DefaultRestClientBuilderCustomizer implements RestClientBuilderCustomizer {

		private static final PropertyMapper map = PropertyMapper.get();

		private final ConsolidatedProperties properties;

		DefaultRestClientBuilderCustomizer(ConsolidatedProperties properties) {
			this.properties = properties;
		}

		@Override
		public void customize(RestClientBuilder builder) {
		}

		@Override
		public void customize(HttpAsyncClientBuilder builder) {
			builder.setDefaultCredentialsProvider(new PropertiesCredentialsProvider(this.properties));
		}

		@Override
		public void customize(RequestConfig.Builder builder) {
			map.from(this.properties::getConnectionTimeout).whenNonNull().asInt(Duration::toMillis)
					.to(builder::setConnectTimeout);
			map.from(this.properties::getSocketTimeout).whenNonNull().asInt(Duration::toMillis)
					.to(builder::setSocketTimeout);
		}

	}

	private static class PropertiesCredentialsProvider extends BasicCredentialsProvider {

		PropertiesCredentialsProvider(ConsolidatedProperties properties) {
			if (StringUtils.hasText(properties.getUsername())) {
				Credentials credentials = new UsernamePasswordCredentials(properties.getUsername(),
						properties.getPassword());
				setCredentials(AuthScope.ANY, credentials);
			}
			properties.getUris().stream().map(this::toUri).filter(this::hasUserInfo)
					.forEach(this::addUserInfoCredentials);
		}

		private URI toUri(String uri) {
			try {
				return URI.create(uri);
			}
			catch (IllegalArgumentException ex) {
				return null;
			}
		}

		private boolean hasUserInfo(URI uri) {
			return uri != null && StringUtils.hasLength(uri.getUserInfo());
		}

		private void addUserInfoCredentials(URI uri) {
			AuthScope authScope = new AuthScope(uri.getHost(), uri.getPort());
			Credentials credentials = createUserInfoCredentials(uri.getUserInfo());
			setCredentials(authScope, credentials);
		}

		private Credentials createUserInfoCredentials(String userInfo) {
			int delimiter = userInfo.indexOf(":");
			if (delimiter == -1) {
				return new UsernamePasswordCredentials(userInfo, null);
			}
			String username = userInfo.substring(0, delimiter);
			String password = userInfo.substring(delimiter + 1);
			return new UsernamePasswordCredentials(username, password);
		}

	}

	@SuppressWarnings("deprecation")
	private static final class ConsolidatedProperties {

		private final ElasticsearchProperties properties;

		private final DeprecatedElasticsearchRestClientProperties deprecatedProperties;

		private ConsolidatedProperties(ElasticsearchProperties properties,
				DeprecatedElasticsearchRestClientProperties deprecatedProperties) {
			this.properties = properties;
			this.deprecatedProperties = deprecatedProperties;
		}

		private List<String> getUris() {
			return this.deprecatedProperties.isCustomized() ? this.deprecatedProperties.getUris()
					: this.properties.getUris();
		}

		private String getUsername() {
			return this.deprecatedProperties.isCustomized() ? this.deprecatedProperties.getUsername()
					: this.properties.getUsername();
		}

		private String getPassword() {
			return this.deprecatedProperties.isCustomized() ? this.deprecatedProperties.getPassword()
					: this.properties.getPassword();
		}

		private Duration getConnectionTimeout() {
			return this.deprecatedProperties.isCustomized() ? this.deprecatedProperties.getConnectionTimeout()
					: this.properties.getConnectionTimeout();
		}

		private Duration getSocketTimeout() {
			return this.deprecatedProperties.isCustomized() ? this.deprecatedProperties.getReadTimeout()
					: this.properties.getSocketTimeout();
		}

		private String getPathPrefix() {
			return this.deprecatedProperties.isCustomized() ? null : this.properties.getPathPrefix();
		}

	}

}
