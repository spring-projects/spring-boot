/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.elasticsearch.autoconfigure;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import co.elastic.clients.transport.rest5_client.low_level.sniffer.Sniffer;
import co.elastic.clients.transport.rest5_client.low_level.sniffer.SnifferBuilder;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.reactor.ssl.SSLBufferMode;
import org.apache.hc.core5.util.Timeout;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails.Node;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails.Node.Protocol;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchProperties.Restclient.Ssl;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Elasticsearch rest client configurations.
 *
 * @author Stephane Nicoll
 * @author Filip Hrisafov
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ElasticsearchRestClientConfigurations {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(Rest5ClientBuilder.class)
	static class RestClientBuilderConfiguration {

		private final ElasticsearchProperties properties;

		RestClientBuilderConfiguration(ElasticsearchProperties properties) {
			this.properties = properties;
		}

		@Bean
		@ConditionalOnMissingBean(ElasticsearchConnectionDetails.class)
		PropertiesElasticsearchConnectionDetails elasticsearchConnectionDetails(ObjectProvider<SslBundles> sslBundles) {
			return new PropertiesElasticsearchConnectionDetails(this.properties, sslBundles.getIfAvailable());
		}

		@Bean
		Rest5ClientBuilderCustomizer defaultRestClientBuilderCustomizer(
				ElasticsearchConnectionDetails connectionDetails) {
			return new DefaultRest5ClientBuilderCustomizer(this.properties, connectionDetails);
		}

		@Bean
		Rest5ClientBuilder elasticsearchRestClientBuilder(ElasticsearchConnectionDetails connectionDetails,
				ObjectProvider<Rest5ClientBuilderCustomizer> builderCustomizers) {
			Rest5ClientBuilder builder = Rest5Client.builder(connectionDetails.getNodes()
				.stream()
				.map((node) -> new HttpHost(node.protocol().getScheme(), node.hostname(), node.port()))
				.toArray(HttpHost[]::new));
			builder.setHttpClientConfigCallback((httpClientBuilder) -> builderCustomizers.orderedStream()
				.forEach((customizer) -> customizer.customize(httpClientBuilder)));
			builder.setConnectionManagerCallback((connectionManagerBuilder) -> builderCustomizers.orderedStream()
				.forEach((customizer) -> customizer.customize(connectionManagerBuilder)));
			builder.setConnectionConfigCallback((connectionConfigBuilder) -> builderCustomizers.orderedStream()
				.forEach((customizer) -> customizer.customize(connectionConfigBuilder)));
			builder.setRequestConfigCallback((requestConfigBuilder) -> builderCustomizers.orderedStream()
				.forEach((customizer) -> customizer.customize(requestConfigBuilder)));
			String pathPrefix = connectionDetails.getPathPrefix();
			if (pathPrefix != null) {
				builder.setPathPrefix(pathPrefix);
			}
			builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(Rest5Client.class)
	static class RestClientConfiguration {

		@Bean
		Rest5Client elasticsearchRestClient(Rest5ClientBuilder restClientBuilder) {
			return restClientBuilder.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Sniffer.class)
	@ConditionalOnSingleCandidate(Rest5Client.class)
	static class RestClientSnifferConfiguration {

		@Bean
		@ConditionalOnMissingBean
		Sniffer elasticsearchSniffer(Rest5Client client, ElasticsearchProperties properties) {
			SnifferBuilder builder = Sniffer.builder(client);
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			Duration interval = properties.getRestclient().getSniffer().getInterval();
			map.from(interval).asInt(Duration::toMillis).to(builder::setSniffIntervalMillis);
			Duration delayAfterFailure = properties.getRestclient().getSniffer().getDelayAfterFailure();
			map.from(delayAfterFailure).asInt(Duration::toMillis).to(builder::setSniffAfterFailureDelayMillis);
			return builder.build();
		}

	}

	static class DefaultRest5ClientBuilderCustomizer implements Rest5ClientBuilderCustomizer, Ordered {

		private static final PropertyMapper map = PropertyMapper.get();

		private final ElasticsearchProperties properties;

		private final ElasticsearchConnectionDetails connectionDetails;

		DefaultRest5ClientBuilderCustomizer(ElasticsearchProperties properties,
				ElasticsearchConnectionDetails connectionDetails) {
			this.properties = properties;
			this.connectionDetails = connectionDetails;
		}

		@Override
		public void customize(Rest5ClientBuilder restClientBuilder) {
		}

		@Override
		public void customize(HttpAsyncClientBuilder httpClientBuilder) {
			httpClientBuilder
				.setDefaultCredentialsProvider(new ConnectionDetailsCredentialsProvider(this.connectionDetails));
			map.from(this.properties::isSocketKeepAlive)
				.to((keepAlive) -> httpClientBuilder
					.setIOReactorConfig(IOReactorConfig.custom().setSoKeepAlive(keepAlive).build()));
		}

		@Override
		public void customize(ConnectionConfig.Builder connectionConfigBuilder) {
			map.from(this.properties::getConnectionTimeout)
				.whenNonNull()
				.as(Timeout::of)
				.to(connectionConfigBuilder::setConnectTimeout);
			map.from(this.properties::getSocketTimeout)
				.whenNonNull()
				.as(Timeout::of)
				.to(connectionConfigBuilder::setSocketTimeout);
		}

		@Override
		public void customize(PoolingAsyncClientConnectionManagerBuilder connectionManagerBuilder) {
			SslBundle sslBundle = this.connectionDetails.getSslBundle();
			if (sslBundle != null) {
				SSLContext sslContext = sslBundle.createSslContext();
				SslOptions sslOptions = sslBundle.getOptions();
				DefaultClientTlsStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext,
						sslOptions.getEnabledProtocols(), sslOptions.getCiphers(), SSLBufferMode.STATIC,
						NoopHostnameVerifier.INSTANCE);
				connectionManagerBuilder.setTlsStrategy(tlsStrategy);
			}
		}

		@Override
		public int getOrder() {
			return 0;
		}

	}

	private static class ConnectionDetailsCredentialsProvider extends BasicCredentialsProvider {

		ConnectionDetailsCredentialsProvider(ElasticsearchConnectionDetails connectionDetails) {
			String username = connectionDetails.getUsername();
			if (StringUtils.hasText(username)) {
				String password = connectionDetails.getPassword();
				char[] passwordChars = StringUtils.hasText(password) ? password.toCharArray() : null;
				Credentials credentials = new UsernamePasswordCredentials(username, passwordChars);
				setCredentials(new AuthScope(null, -1), credentials);
			}
			Stream<URI> uris = getUris(connectionDetails);
			uris.filter(this::hasUserInfo).forEach(this::addUserInfoCredentials);
		}

		private Stream<URI> getUris(ElasticsearchConnectionDetails connectionDetails) {
			return connectionDetails.getNodes().stream().map(Node::toUri);
		}

		private boolean hasUserInfo(@Nullable URI uri) {
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
			return new UsernamePasswordCredentials(username, password.toCharArray());
		}

	}

	/**
	 * Adapts {@link ElasticsearchProperties} to {@link ElasticsearchConnectionDetails}.
	 */
	static class PropertiesElasticsearchConnectionDetails implements ElasticsearchConnectionDetails {

		private final ElasticsearchProperties properties;

		private final @Nullable SslBundles sslBundles;

		PropertiesElasticsearchConnectionDetails(ElasticsearchProperties properties, @Nullable SslBundles sslBundles) {
			this.properties = properties;
			this.sslBundles = sslBundles;
		}

		@Override
		public List<Node> getNodes() {
			return this.properties.getUris().stream().map(this::createNode).toList();
		}

		@Override
		public @Nullable String getUsername() {
			return this.properties.getUsername();
		}

		@Override
		public @Nullable String getPassword() {
			return this.properties.getPassword();
		}

		@Override
		public @Nullable String getPathPrefix() {
			return this.properties.getPathPrefix();
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			Ssl ssl = this.properties.getRestclient().getSsl();
			if (StringUtils.hasLength(ssl.getBundle())) {
				Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
				return this.sslBundles.getBundle(ssl.getBundle());
			}
			return null;
		}

		private Node createNode(String uri) {
			if (!(uri.startsWith("http://") || uri.startsWith("https://"))) {
				uri = "http://" + uri;
			}
			return createNode(URI.create(uri));
		}

		private Node createNode(URI uri) {
			String userInfo = uri.getUserInfo();
			Protocol protocol = Protocol.forScheme(uri.getScheme());
			if (!StringUtils.hasLength(userInfo)) {
				return new Node(uri.getHost(), uri.getPort(), protocol, null, null);
			}
			int separatorIndex = userInfo.indexOf(':');
			if (separatorIndex == -1) {
				return new Node(uri.getHost(), uri.getPort(), protocol, userInfo, null);
			}
			String[] components = userInfo.split(":");
			return new Node(uri.getHost(), uri.getPort(), protocol, components[0],
					(components.length > 1) ? components[1] : "");
		}

	}

}
