/*
 * Copyright 2012-2023 the original author or authors.
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
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.sniff.Sniffer;
import org.elasticsearch.client.sniff.SnifferBuilder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails.Node;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails.Node.Protocol;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

	/**
	 * RestClientBuilderConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(RestClientBuilder.class)
	static class RestClientBuilderConfiguration {

		private final ElasticsearchProperties properties;

		/**
		 * Constructs a new RestClientBuilderConfiguration object with the specified
		 * ElasticsearchProperties.
		 * @param properties the ElasticsearchProperties object containing the
		 * configuration properties for the RestClientBuilder
		 */
		RestClientBuilderConfiguration(ElasticsearchProperties properties) {
			this.properties = properties;
		}

		/**
		 * Creates an instance of {@link PropertiesElasticsearchConnectionDetails} if no
		 * bean of type {@link ElasticsearchConnectionDetails} is present.
		 * @return the {@link PropertiesElasticsearchConnectionDetails} instance
		 */
		@Bean
		@ConditionalOnMissingBean(ElasticsearchConnectionDetails.class)
		PropertiesElasticsearchConnectionDetails elasticsearchConnectionDetails() {
			return new PropertiesElasticsearchConnectionDetails(this.properties);
		}

		/**
		 * Returns a default RestClientBuilderCustomizer bean.
		 * @param connectionDetails the Elasticsearch connection details
		 * @return the default RestClientBuilderCustomizer
		 */
		@Bean
		RestClientBuilderCustomizer defaultRestClientBuilderCustomizer(
				ElasticsearchConnectionDetails connectionDetails) {
			return new DefaultRestClientBuilderCustomizer(this.properties, connectionDetails);
		}

		/**
		 * Creates a RestClientBuilder for Elasticsearch based on the provided connection
		 * details.
		 * @param connectionDetails the Elasticsearch connection details
		 * @param builderCustomizers the customizers for the RestClientBuilder
		 * @param sslBundles the SSL bundles for configuring SSL
		 * @return the RestClientBuilder configured with the provided connection details
		 */
		@Bean
		RestClientBuilder elasticsearchRestClientBuilder(ElasticsearchConnectionDetails connectionDetails,
				ObjectProvider<RestClientBuilderCustomizer> builderCustomizers, ObjectProvider<SslBundles> sslBundles) {
			RestClientBuilder builder = RestClient.builder(connectionDetails.getNodes()
				.stream()
				.map((node) -> new HttpHost(node.hostname(), node.port(), node.protocol().getScheme()))
				.toArray(HttpHost[]::new));
			builder.setHttpClientConfigCallback((httpClientBuilder) -> {
				builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(httpClientBuilder));
				String sslBundleName = this.properties.getRestclient().getSsl().getBundle();
				if (StringUtils.hasText(sslBundleName)) {
					configureSsl(httpClientBuilder, sslBundles.getObject().getBundle(sslBundleName));
				}
				return httpClientBuilder;
			});
			builder.setRequestConfigCallback((requestConfigBuilder) -> {
				builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(requestConfigBuilder));
				return requestConfigBuilder;
			});
			String pathPrefix = connectionDetails.getPathPrefix();
			if (pathPrefix != null) {
				builder.setPathPrefix(pathPrefix);
			}
			builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
			return builder;
		}

		/**
		 * Configures SSL for the HTTP client builder.
		 * @param httpClientBuilder The HTTP client builder to configure.
		 * @param sslBundle The SSL bundle containing the SSL context and options.
		 */
		private void configureSsl(HttpAsyncClientBuilder httpClientBuilder, SslBundle sslBundle) {
			SSLContext sslcontext = sslBundle.createSslContext();
			SslOptions sslOptions = sslBundle.getOptions();
			httpClientBuilder.setSSLStrategy(new SSLIOSessionStrategy(sslcontext, sslOptions.getEnabledProtocols(),
					sslOptions.getCiphers(), (HostnameVerifier) null));
		}

	}

	/**
	 * RestClientConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(RestClient.class)
	static class RestClientConfiguration {

		/**
		 * Creates and returns an instance of RestClient using the provided
		 * RestClientBuilder.
		 * @param restClientBuilder the RestClientBuilder used to build the RestClient
		 * @return an instance of RestClient
		 */
		@Bean
		RestClient elasticsearchRestClient(RestClientBuilder restClientBuilder) {
			return restClientBuilder.build();
		}

	}

	/**
	 * RestClientSnifferConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Sniffer.class)
	@ConditionalOnSingleCandidate(RestClient.class)
	static class RestClientSnifferConfiguration {

		/**
		 * Creates a Sniffer instance for Elasticsearch based on the provided RestClient
		 * and ElasticsearchProperties. If no Sniffer instance is already present, this
		 * method will be called.
		 * @param client the RestClient instance used for communication with Elasticsearch
		 * @param properties the ElasticsearchProperties instance containing configuration
		 * properties
		 * @return a Sniffer instance configured with the provided RestClient and
		 * ElasticsearchProperties
		 */
		@Bean
		@ConditionalOnMissingBean
		Sniffer elasticsearchSniffer(RestClient client, ElasticsearchProperties properties) {
			SnifferBuilder builder = Sniffer.builder(client);
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			Duration interval = properties.getRestclient().getSniffer().getInterval();
			map.from(interval).asInt(Duration::toMillis).to(builder::setSniffIntervalMillis);
			Duration delayAfterFailure = properties.getRestclient().getSniffer().getDelayAfterFailure();
			map.from(delayAfterFailure).asInt(Duration::toMillis).to(builder::setSniffAfterFailureDelayMillis);
			return builder.build();
		}

	}

	/**
	 * DefaultRestClientBuilderCustomizer class.
	 */
	static class DefaultRestClientBuilderCustomizer implements RestClientBuilderCustomizer {

		private static final PropertyMapper map = PropertyMapper.get();

		private final ElasticsearchProperties properties;

		private final ElasticsearchConnectionDetails connectionDetails;

		/**
		 * Constructs a new DefaultRestClientBuilderCustomizer with the specified
		 * ElasticsearchProperties and ElasticsearchConnectionDetails.
		 * @param properties the ElasticsearchProperties to be used
		 * @param connectionDetails the ElasticsearchConnectionDetails to be used
		 */
		DefaultRestClientBuilderCustomizer(ElasticsearchProperties properties,
				ElasticsearchConnectionDetails connectionDetails) {
			this.properties = properties;
			this.connectionDetails = connectionDetails;
		}

		/**
		 * Customizes the RestClientBuilder.
		 * @param builder the RestClientBuilder to be customized
		 */
		@Override
		public void customize(RestClientBuilder builder) {
		}

		/**
		 * Customize the HttpAsyncClientBuilder with the provided configuration.
		 * @param builder the HttpAsyncClientBuilder to customize
		 */
		@Override
		public void customize(HttpAsyncClientBuilder builder) {
			builder.setDefaultCredentialsProvider(new ConnectionDetailsCredentialsProvider(this.connectionDetails));
			map.from(this.properties::isSocketKeepAlive)
				.to((keepAlive) -> builder
					.setDefaultIOReactorConfig(IOReactorConfig.custom().setSoKeepAlive(keepAlive).build()));
		}

		/**
		 * Customize the RequestConfig.Builder with the provided connection and socket
		 * timeouts.
		 * @param builder the RequestConfig.Builder to customize
		 */
		@Override
		public void customize(RequestConfig.Builder builder) {
			map.from(this.properties::getConnectionTimeout)
				.whenNonNull()
				.asInt(Duration::toMillis)
				.to(builder::setConnectTimeout);
			map.from(this.properties::getSocketTimeout)
				.whenNonNull()
				.asInt(Duration::toMillis)
				.to(builder::setSocketTimeout);
		}

	}

	/**
	 * ConnectionDetailsCredentialsProvider class.
	 */
	private static class ConnectionDetailsCredentialsProvider extends BasicCredentialsProvider {

		/**
		 * Sets the credentials for the Elasticsearch connection based on the provided
		 * connection details. If a username is provided in the connection details, it
		 * sets the credentials using the provided username and password. It also sets the
		 * credentials for any URIs that have user info in the connection details.
		 * @param connectionDetails the Elasticsearch connection details
		 */
		ConnectionDetailsCredentialsProvider(ElasticsearchConnectionDetails connectionDetails) {
			String username = connectionDetails.getUsername();
			if (StringUtils.hasText(username)) {
				Credentials credentials = new UsernamePasswordCredentials(username, connectionDetails.getPassword());
				setCredentials(AuthScope.ANY, credentials);
			}
			Stream<URI> uris = getUris(connectionDetails);
			uris.filter(this::hasUserInfo).forEach(this::addUserInfoCredentials);
		}

		/**
		 * Returns a stream of URIs based on the provided Elasticsearch connection
		 * details.
		 * @param connectionDetails the Elasticsearch connection details
		 * @return a stream of URIs
		 */
		private Stream<URI> getUris(ElasticsearchConnectionDetails connectionDetails) {
			return connectionDetails.getNodes().stream().map(Node::toUri);
		}

		/**
		 * Checks if the given URI has user information.
		 * @param uri the URI to check
		 * @return {@code true} if the URI has user information, {@code false} otherwise
		 */
		private boolean hasUserInfo(URI uri) {
			return uri != null && StringUtils.hasLength(uri.getUserInfo());
		}

		/**
		 * Adds user information credentials to the specified URI.
		 * @param uri The URI to add the credentials to.
		 */
		private void addUserInfoCredentials(URI uri) {
			AuthScope authScope = new AuthScope(uri.getHost(), uri.getPort());
			Credentials credentials = createUserInfoCredentials(uri.getUserInfo());
			setCredentials(authScope, credentials);
		}

		/**
		 * Creates a new instance of Credentials using the provided user information.
		 * @param userInfo the user information in the format "username:password" or just
		 * "username"
		 * @return a new instance of Credentials with the provided user information
		 */
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

	/**
	 * Adapts {@link ElasticsearchProperties} to {@link ElasticsearchConnectionDetails}.
	 */
	static class PropertiesElasticsearchConnectionDetails implements ElasticsearchConnectionDetails {

		private final ElasticsearchProperties properties;

		/**
		 * Initializes a new instance of the PropertiesElasticsearchConnectionDetails
		 * class.
		 * @param properties the ElasticsearchProperties object containing the connection
		 * details
		 */
		PropertiesElasticsearchConnectionDetails(ElasticsearchProperties properties) {
			this.properties = properties;
		}

		/**
		 * Retrieves a list of nodes based on the URIs specified in the connection
		 * details.
		 * @return a list of Node objects representing the Elasticsearch nodes
		 */
		@Override
		public List<Node> getNodes() {
			return this.properties.getUris().stream().map(this::createNode).toList();
		}

		/**
		 * Returns the username associated with the Elasticsearch connection details.
		 * @return the username associated with the Elasticsearch connection details
		 */
		@Override
		public String getUsername() {
			return this.properties.getUsername();
		}

		/**
		 * Returns the password for the Elasticsearch connection.
		 * @return the password for the Elasticsearch connection
		 */
		@Override
		public String getPassword() {
			return this.properties.getPassword();
		}

		/**
		 * Returns the path prefix for the Elasticsearch connection details.
		 * @return the path prefix for the Elasticsearch connection details
		 */
		@Override
		public String getPathPrefix() {
			return this.properties.getPathPrefix();
		}

		/**
		 * Creates a Node object using the provided URI. If the URI does not start with
		 * "http://" or "https://", it is assumed to be an HTTP URI and "http://" is
		 * prepended to it.
		 * @param uri the URI to create the Node object from
		 * @return the created Node object
		 */
		private Node createNode(String uri) {
			if (!(uri.startsWith("http://") || uri.startsWith("https://"))) {
				uri = "http://" + uri;
			}
			return createNode(URI.create(uri));
		}

		/**
		 * Creates a new Node object based on the provided URI.
		 * @param uri the URI to create the Node from
		 * @return a new Node object
		 */
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
