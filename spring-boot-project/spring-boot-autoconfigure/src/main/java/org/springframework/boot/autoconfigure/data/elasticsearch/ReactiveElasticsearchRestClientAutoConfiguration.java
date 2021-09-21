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

package org.springframework.boot.autoconfigure.data.elasticsearch;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import reactor.netty.http.client.HttpClient;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients;
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients.WebClientConfigurationCallback;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Elasticsearch Reactive REST
 * clients.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
@SuppressWarnings("deprecation")
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ ReactiveRestClients.class, WebClient.class, HttpClient.class })
@EnableConfigurationProperties({ ElasticsearchProperties.class, ReactiveElasticsearchRestClientProperties.class,
		DeprecatedReactiveElasticsearchRestClientProperties.class })
public class ReactiveElasticsearchRestClientAutoConfiguration {

	private final ConsolidatedProperties properties;

	ReactiveElasticsearchRestClientAutoConfiguration(ElasticsearchProperties properties,
			ReactiveElasticsearchRestClientProperties restClientProperties,
			DeprecatedReactiveElasticsearchRestClientProperties reactiveProperties) {
		this.properties = new ConsolidatedProperties(properties, restClientProperties, reactiveProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public ClientConfiguration clientConfiguration() {
		ClientConfiguration.MaybeSecureClientConfigurationBuilder builder = ClientConfiguration.builder()
				.connectedTo(this.properties.getEndpoints().toArray(new String[0]));
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this.properties.isUseSsl()).whenTrue().toCall(builder::usingSsl);
		map.from(this.properties.getCredentials())
				.to((credentials) -> builder.withBasicAuth(credentials.getUsername(), credentials.getPassword()));
		map.from(this.properties.getConnectionTimeout()).to(builder::withConnectTimeout);
		map.from(this.properties.getSocketTimeout()).to(builder::withSocketTimeout);
		map.from(this.properties.getPathPrefix()).to(builder::withPathPrefix);
		configureExchangeStrategies(map, builder);
		return builder.build();
	}

	private void configureExchangeStrategies(PropertyMapper map,
			ClientConfiguration.TerminalClientConfigurationBuilder builder) {
		map.from(this.properties.getMaxInMemorySize()).asInt(DataSize::toBytes).to((maxInMemorySize) -> {
			builder.withClientConfigurer(WebClientConfigurationCallback.from((webClient) -> {
				ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
						.codecs((configurer) -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize)).build();
				return webClient.mutate().exchangeStrategies(exchangeStrategies).build();
			}));
		});
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactiveElasticsearchClient reactiveElasticsearchClient(ClientConfiguration clientConfiguration) {
		return ReactiveRestClients.create(clientConfiguration);
	}

	private static final class ConsolidatedProperties {

		private final ElasticsearchProperties properties;

		private final ReactiveElasticsearchRestClientProperties restClientProperties;

		private final DeprecatedReactiveElasticsearchRestClientProperties deprecatedProperties;

		private final List<URI> uris;

		private ConsolidatedProperties(ElasticsearchProperties properties,
				ReactiveElasticsearchRestClientProperties restClientProperties,
				DeprecatedReactiveElasticsearchRestClientProperties deprecatedreactiveProperties) {
			this.properties = properties;
			this.restClientProperties = restClientProperties;
			this.deprecatedProperties = deprecatedreactiveProperties;
			this.uris = properties.getUris().stream().map((s) -> s.startsWith("http") ? s : "http://" + s)
					.map(URI::create).collect(Collectors.toList());
		}

		private List<String> getEndpoints() {
			if (this.deprecatedProperties.isCustomized()) {
				return this.deprecatedProperties.getEndpoints();
			}
			return this.uris.stream().map(this::getEndpoint).collect(Collectors.toList());
		}

		private String getEndpoint(URI uri) {
			return uri.getHost() + ":" + uri.getPort();
		}

		private Credentials getCredentials() {
			if (this.deprecatedProperties.isCustomized()) {
				return Credentials.from(this.deprecatedProperties);
			}
			Credentials propertyCredentials = Credentials.from(this.properties);
			Credentials uriCredentials = Credentials.from(this.properties.getUris());
			if (uriCredentials == null) {
				return propertyCredentials;
			}
			Assert.isTrue(propertyCredentials == null || uriCredentials.equals(propertyCredentials),
					"Credentials from URI user info do not match those from spring.elasticsearch.username and "
							+ "spring.elasticsearch.password");
			return uriCredentials;
		}

		private Duration getConnectionTimeout() {
			return this.deprecatedProperties.isCustomized() ? this.deprecatedProperties.getConnectionTimeout()
					: this.properties.getConnectionTimeout();
		}

		private Duration getSocketTimeout() {
			return this.deprecatedProperties.isCustomized() ? this.deprecatedProperties.getSocketTimeout()
					: this.properties.getSocketTimeout();
		}

		private boolean isUseSsl() {
			if (this.deprecatedProperties.isCustomized()) {
				return this.deprecatedProperties.isUseSsl();
			}
			Set<String> schemes = this.uris.stream().map(URI::getScheme).collect(Collectors.toSet());
			Assert.isTrue(schemes.size() == 1, "Configured Elasticsearch URIs have varying schemes");
			return schemes.iterator().next().equals("https");
		}

		private DataSize getMaxInMemorySize() {
			return this.deprecatedProperties.isCustomized() ? this.deprecatedProperties.getMaxInMemorySize()
					: this.restClientProperties.getMaxInMemorySize();
		}

		private String getPathPrefix() {
			return this.deprecatedProperties.isCustomized() ? null : this.properties.getPathPrefix();
		}

		private static final class Credentials {

			private final String username;

			private final String password;

			private Credentials(String username, String password) {
				this.username = username;
				this.password = password;
			}

			private String getUsername() {
				return this.username;
			}

			private String getPassword() {
				return this.password;
			}

			private static Credentials from(List<String> uris) {
				Set<String> userInfos = uris.stream().map(URI::create).map(URI::getUserInfo)
						.collect(Collectors.toSet());
				Assert.isTrue(userInfos.size() == 1, "Configured Elasticsearch URIs have varying user infos");
				String userInfo = userInfos.iterator().next();
				if (userInfo == null) {
					return null;
				}
				String[] parts = userInfo.split(":");
				String username = parts[0];
				String password = (parts.length != 2) ? "" : parts[1];
				return new Credentials(username, password);
			}

			private static Credentials from(ElasticsearchProperties properties) {
				return getCredentials(properties.getUsername(), properties.getPassword());
			}

			private static Credentials from(DeprecatedReactiveElasticsearchRestClientProperties properties) {
				return getCredentials(properties.getUsername(), properties.getPassword());
			}

			private static Credentials getCredentials(String username, String password) {
				if (username == null && password == null) {
					return null;
				}
				return new Credentials(username, password);
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj) {
					return true;
				}
				if (obj == null || getClass() != obj.getClass()) {
					return false;
				}
				Credentials other = (Credentials) obj;
				return ObjectUtils.nullSafeEquals(this.username, other.username)
						&& ObjectUtils.nullSafeEquals(this.password, other.password);
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + ObjectUtils.nullSafeHashCode(this.username);
				result = prime * result + ObjectUtils.nullSafeHashCode(this.password);
				return result;
			}

		}

	}

}
