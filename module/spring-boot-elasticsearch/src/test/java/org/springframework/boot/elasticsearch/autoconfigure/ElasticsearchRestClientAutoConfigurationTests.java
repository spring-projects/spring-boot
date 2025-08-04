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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import co.elastic.clients.transport.rest5_client.low_level.Node;
import co.elastic.clients.transport.rest5_client.low_level.Rest5Client;
import co.elastic.clients.transport.rest5_client.low_level.Rest5ClientBuilder;
import co.elastic.clients.transport.rest5_client.low_level.sniffer.Sniffer;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsProvider;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.function.Resolver;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.util.Timeout;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails.Node.Protocol;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchRestClientConfigurations.PropertiesElasticsearchConnectionDetails;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ElasticsearchRestClientAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Vedran Pavic
 * @author Evgeniy Cheban
 * @author Filip Hrisafov
 * @author Andy Wilkinson
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class ElasticsearchRestClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(ElasticsearchRestClientAutoConfiguration.class, SslAutoConfiguration.class));

	@Test
	void configureShouldCreateRest5ClientBuilderAndRest5Client() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(Rest5Client.class)
			.hasSingleBean(Rest5ClientBuilder.class));
	}

	@Test
	void configureWhenCustomRest5ClientShouldBackOff() {
		this.contextRunner.withUserConfiguration(CustomRest5ClientConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(Rest5ClientBuilder.class)
				.hasSingleBean(Rest5Client.class)
				.hasBean("customRest5Client"));
	}

	@Test
	void configureWhenBuilderCustomizerShouldApply() {
		this.contextRunner.withUserConfiguration(BuilderCustomizerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Rest5Client.class);
			Rest5Client restClient = context.getBean(Rest5Client.class);
			assertThat(restClient).hasFieldOrPropertyWithValue("pathPrefix", "/test");
			assertThat(restClient).extracting("client.defaultConfig.cookieSpec").isEqualTo("rfc6265-lax");
			assertThat(restClient).extracting("client.manager.pool.maxTotal").isEqualTo(100);
		});
	}

	@Test
	void configureWithNoTimeoutsApplyDefaults() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Rest5Client.class);
			Rest5Client restClient = context.getBean(Rest5Client.class);
			assertTimeouts(restClient, Duration.ofMillis(Rest5ClientBuilder.DEFAULT_CONNECT_TIMEOUT_MILLIS),
					Duration.ofMillis(Rest5ClientBuilder.DEFAULT_SOCKET_TIMEOUT_MILLIS));
		});
	}

	@Test
	void configureWithCustomTimeouts() {
		this.contextRunner
			.withPropertyValues("spring.elasticsearch.connection-timeout=15s", "spring.elasticsearch.socket-timeout=1m")
			.run((context) -> {
				assertThat(context).hasSingleBean(Rest5Client.class);
				Rest5Client restClient = context.getBean(Rest5Client.class);
				assertTimeouts(restClient, Duration.ofSeconds(15), Duration.ofMinutes(1));
			});
	}

	@SuppressWarnings("unchecked")
	private static void assertTimeouts(Rest5Client restClient, Duration connectTimeout, Duration socketTimeout) {
		assertThat(restClient).extracting("client.manager.connectionConfigResolver")
			.asInstanceOf(InstanceOfAssertFactories.type(Resolver.class))
			.extracting((resolver) -> ((Resolver<HttpRoute, ConnectionConfig>) resolver).resolve(null))
			.satisfies((connectionConfig) -> {
				assertThat(connectionConfig.getSocketTimeout()).isEqualTo(Timeout.of(socketTimeout));
				assertThat(connectionConfig.getConnectTimeout()).isEqualTo(Timeout.of(connectTimeout));
			});
	}

	@Test
	void configureUriWithNoScheme() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.uris=localhost:9876").run((context) -> {
			Rest5Client client = context.getBean(Rest5Client.class);
			assertThat(client.getNodes().stream().map(Node::getHost).map(HttpHost::toString))
				.containsExactly("http://localhost:9876");
		});
	}

	@Test
	void configureUriWithUsernameOnly() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.uris=http://user@localhost:9200").run((context) -> {
			Rest5Client client = context.getBean(Rest5Client.class);
			assertThat(client.getNodes().stream().map(Node::getHost).map(HttpHost::toString))
				.containsExactly("http://localhost:9200");
			assertThat(client)
				.extracting("client.credentialsProvider", InstanceOfAssertFactories.type(CredentialsProvider.class))
				.satisfies((credentialsProvider) -> {
					UsernamePasswordCredentials credentials = (UsernamePasswordCredentials) credentialsProvider
						.getCredentials(new AuthScope("localhost", 9200), null);
					assertThat(credentials.getUserPrincipal().getName()).isEqualTo("user");
					assertThat(credentials.getUserPassword()).isNull();
				});
		});
	}

	@Test
	void configureUriWithUsernameAndEmptyPassword() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.uris=http://user:@localhost:9200")
			.run((context) -> {
				Rest5Client client = context.getBean(Rest5Client.class);
				assertThat(client.getNodes().stream().map(Node::getHost).map(HttpHost::toString))
					.containsExactly("http://localhost:9200");
				assertThat(client)
					.extracting("client.credentialsProvider", InstanceOfAssertFactories.type(CredentialsProvider.class))
					.satisfies((credentialsProvider) -> {
						UsernamePasswordCredentials credentials = (UsernamePasswordCredentials) credentialsProvider
							.getCredentials(new AuthScope("localhost", 9200), null);
						assertThat(credentials.getUserPrincipal().getName()).isEqualTo("user");
						assertThat(credentials.getUserPassword()).isEmpty();
					});
			});
	}

	@Test
	void configureUriWithUsernameAndPasswordWhenUsernameAndPasswordPropertiesSet() {
		this.contextRunner
			.withPropertyValues("spring.elasticsearch.uris=http://user:password@localhost:9200,localhost:9201",
					"spring.elasticsearch.username=admin", "spring.elasticsearch.password=admin")
			.run((context) -> {
				Rest5Client client = context.getBean(Rest5Client.class);
				assertThat(client.getNodes().stream().map(Node::getHost).map(HttpHost::toString))
					.containsExactly("http://localhost:9200", "http://localhost:9201");
				assertThat(client)
					.extracting("client.credentialsProvider", InstanceOfAssertFactories.type(CredentialsProvider.class))
					.satisfies((credentialsProvider) -> {
						UsernamePasswordCredentials uriCredentials = (UsernamePasswordCredentials) credentialsProvider
							.getCredentials(new AuthScope("localhost", 9200), null);
						assertThat(uriCredentials.getUserPrincipal().getName()).isEqualTo("user");
						assertThat(uriCredentials.getUserPassword()).containsExactly("password".toCharArray());
						UsernamePasswordCredentials defaultCredentials = (UsernamePasswordCredentials) credentialsProvider
							.getCredentials(new AuthScope("localhost", 9201), null);
						assertThat(defaultCredentials.getUserPrincipal().getName()).isEqualTo("admin");
						assertThat(defaultCredentials.getUserPassword()).containsExactly("admin".toCharArray());
					});
			});
	}

	@Test
	void configureWithCustomPathPrefix() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.path-prefix=/some/prefix").run((context) -> {
			Rest5Client client = context.getBean(Rest5Client.class);
			assertThat(client).extracting("pathPrefix").isEqualTo("/some/prefix");
		});
	}

	@Test
	void configureWithNoSocketKeepAliveApplyDefault() {
		Rest5Client client = Rest5Client.builder(new HttpHost("http", "localhost", 9201)).build();
		assertThat(client.getHttpClient()).extracting("ioReactor.workers")
			.asInstanceOf(InstanceOfAssertFactories.ARRAY)
			.satisfies((workers) -> assertThat(workers[0]).extracting("reactorConfig.soKeepAlive").isEqualTo(false));
	}

	@Test
	void configureWithCustomSocketKeepAlive() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.socket-keep-alive=true")
			.run((context) -> assertThat(context.getBean(Rest5Client.class).getHttpClient())
				.extracting("ioReactor.workers")
				.asInstanceOf(InstanceOfAssertFactories.ARRAY)
				.satisfies(
						(workers) -> assertThat(workers[0]).extracting("reactorConfig.soKeepAlive").isEqualTo(true)));
	}

	@Test
	void configureShouldCreateSnifferUsingRest5Client() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Sniffer.class);
			assertThat(context.getBean(Sniffer.class)).hasFieldOrPropertyWithValue("restClient",
					context.getBean(Rest5Client.class));
			// Validate shutdown order as the sniffer must be shutdown before the
			// client
			assertThat(context.getBeanFactory().getDependentBeans("elasticsearchRestClient"))
				.contains("elasticsearchSniffer");
		});
	}

	@Test
	void configureWithCustomSnifferSettings() {
		this.contextRunner
			.withPropertyValues("spring.elasticsearch.restclient.sniffer.interval=180s",
					"spring.elasticsearch.restclient.sniffer.delay-after-failure=30s")
			.run((context) -> {
				assertThat(context).hasSingleBean(Sniffer.class);
				Sniffer sniffer = context.getBean(Sniffer.class);
				assertThat(sniffer).hasFieldOrPropertyWithValue("sniffIntervalMillis",
						Duration.ofMinutes(3).toMillis());
				assertThat(sniffer).hasFieldOrPropertyWithValue("sniffAfterFailureDelayMillis",
						Duration.ofSeconds(30).toMillis());
			});
	}

	@Test
	void configureWhenCustomSnifferShouldBackOff() {
		Sniffer customSniffer = mock(Sniffer.class);
		this.contextRunner.withBean(Sniffer.class, () -> customSniffer).run((context) -> {
			assertThat(context).hasSingleBean(Sniffer.class);
			Sniffer sniffer = context.getBean(Sniffer.class);
			assertThat(sniffer).isSameAs(customSniffer);
			then(customSniffer).shouldHaveNoInteractions();
		});
	}

	@Test
	void definesPropertiesBasedConnectionDetailsByDefault() {
		this.contextRunner
			.run((context) -> assertThat(context).hasSingleBean(PropertiesElasticsearchConnectionDetails.class));
	}

	@Test
	void shouldUseCustomConnectionDetailsWhenDefined() {
		this.contextRunner.withUserConfiguration(ConnectionDetailsConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Rest5Client.class)
				.hasSingleBean(ElasticsearchConnectionDetails.class)
				.doesNotHaveBean(PropertiesElasticsearchConnectionDetails.class);
			Rest5Client restClient = context.getBean(Rest5Client.class);
			assertThat(restClient).hasFieldOrPropertyWithValue("pathPrefix", "/some-path");
			assertThat(restClient.getNodes().stream().map(Node::getHost).map(HttpHost::toString))
				.containsExactly("http://elastic.example.com:9200");
			assertThat(restClient)
				.extracting("client.credentialsProvider", InstanceOfAssertFactories.type(CredentialsProvider.class))
				.satisfies((credentialsProvider) -> {
					UsernamePasswordCredentials uriCredentials = (UsernamePasswordCredentials) credentialsProvider
						.getCredentials(new AuthScope("any.elastic.example.com", 80), null);
					assertThat(uriCredentials.getUserPrincipal().getName()).isEqualTo("user-1");
					assertThat(uriCredentials.getUserPassword()).containsExactly("password-1".toCharArray());
				})
				.satisfies((credentialsProvider) -> {
					UsernamePasswordCredentials uriCredentials = (UsernamePasswordCredentials) credentialsProvider
						.getCredentials(new AuthScope("elastic.example.com", 9200), null);
					assertThat(uriCredentials.getUserPrincipal().getName()).isEqualTo("node-user-1");
					assertThat(uriCredentials.getUserPassword()).containsExactly("node-password-1".toCharArray());
				});

		});
	}

	@Test
	@WithPackageResources("test.jks")
	@SuppressWarnings("unchecked")
	void configureWithSslBundle() {
		List<String> properties = new ArrayList<>();
		properties.add("spring.elasticsearch.restclient.ssl.bundle=mybundle");
		properties.add("spring.ssl.bundle.jks.mybundle.truststore.location=classpath:test.jks");
		properties.add("spring.ssl.bundle.jks.mybundle.options.ciphers=DESede");
		properties.add("spring.ssl.bundle.jks.mybundle.options.enabled-protocols=TLSv1.3");
		this.contextRunner.withPropertyValues(properties.toArray(String[]::new)).run((context) -> {
			assertThat(context).hasSingleBean(Rest5Client.class);
			Rest5Client restClient = context.getBean(Rest5Client.class);
			assertThat(restClient).extracting("client.manager.connectionOperator.tlsStrategyLookup")
				.asInstanceOf(InstanceOfAssertFactories.type(Registry.class))
				.extracting((registry) -> registry.lookup("https"))
				.satisfies((tlsStrategy) -> {
					assertThat(tlsStrategy).extracting("supportedProtocols").isEqualTo(new String[] { "TLSv1.3" });
					assertThat(tlsStrategy).extracting("supportedCipherSuites").isEqualTo(new String[] { "DESede" });
				});
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetailsConfiguration {

		@Bean
		ElasticsearchConnectionDetails elasticsearchConnectionDetails() {
			return new ElasticsearchConnectionDetails() {

				@Override
				public List<Node> getNodes() {
					return List
						.of(new Node("elastic.example.com", 9200, Protocol.HTTP, "node-user-1", "node-password-1"));
				}

				@Override
				public String getUsername() {
					return "user-1";
				}

				@Override
				public String getPassword() {
					return "password-1";
				}

				@Override
				public String getPathPrefix() {
					return "/some-path";
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class BuilderCustomizerConfiguration {

		@Bean
		Rest5ClientBuilderCustomizer myCustomizer() {
			return new Rest5ClientBuilderCustomizer() {

				@Override
				public void customize(Rest5ClientBuilder builder) {
					builder.setPathPrefix("/test");
				}

				@Override
				public void customize(HttpAsyncClientBuilder httpClientBuilder) {
					httpClientBuilder.setConnectionManager(
							PoolingAsyncClientConnectionManagerBuilder.create().setMaxConnTotal(100).build());
				}

				@Override
				public void customize(RequestConfig.Builder builder) {
					builder.setCookieSpec("rfc6265-lax");
				}

				@Override
				public void customize(ConnectionConfig.Builder connectionConfigBuilder) {

				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRest5ClientConfiguration {

		@Bean
		Rest5Client customRest5Client(Rest5ClientBuilder builder) {
			return builder.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TwoCustomRest5ClientConfiguration {

		@Bean
		Rest5Client customRest5Client(Rest5ClientBuilder builder) {
			return builder.build();
		}

		@Bean
		Rest5Client customRest5Client1(Rest5ClientBuilder builder) {
			return builder.build();
		}

	}

}
