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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.Map;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.sniff.Sniffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link ElasticsearchRestClientAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Vedran Pavic
 * @author Evgeniy Cheban
 * @author Filip Hrisafov
 */
class ElasticsearchRestClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ElasticsearchRestClientAutoConfiguration.class));

	@Test
	void configureShouldOnlyCreateHighLevelRestClient() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(RestClient.class)
				.hasSingleBean(RestClientBuilder.class).hasSingleBean(RestHighLevelClient.class));
	}

	@Test
	void configureWithoutRestHighLevelClientShouldOnlyCreateRestClientBuilder() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(RestHighLevelClient.class))
				.run((context) -> assertThat(context).doesNotHaveBean(RestClient.class)
						.doesNotHaveBean(RestHighLevelClient.class).hasSingleBean(RestClientBuilder.class));
	}

	@Test
	void configureWhenCustomRestClientShouldBackOff() {
		this.contextRunner.withUserConfiguration(CustomRestClientConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(RestHighLevelClient.class)
						.hasSingleBean(RestClientBuilder.class).hasSingleBean(RestClient.class)
						.hasBean("customRestClient"));
	}

	@Test
	void configureWhenCustomRestHighLevelClientShouldBackOff() {
		this.contextRunner.withUserConfiguration(CustomRestHighLevelClientConfiguration.class)
				.run((context) -> assertThat(context).hasSingleBean(RestHighLevelClient.class));
	}

	@Test
	void configureWhenDefaultRestClientShouldCreateWhenNoUniqueRestHighLevelClient() {
		this.contextRunner.withUserConfiguration(TwoCustomRestHighLevelClientConfiguration.class).run((context) -> {
			Map<String, RestHighLevelClient> restHighLevelClients = context.getBeansOfType(RestHighLevelClient.class);
			assertThat(restHighLevelClients).hasSize(2);
		});
	}

	@Test
	void configureWhenBuilderCustomizerShouldApply() {
		this.contextRunner.withUserConfiguration(BuilderCustomizerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(RestHighLevelClient.class);
			RestHighLevelClient restClient = context.getBean(RestHighLevelClient.class);
			RestClient lowLevelClient = restClient.getLowLevelClient();
			assertThat(lowLevelClient).hasFieldOrPropertyWithValue("pathPrefix", "/test");
			assertThat(lowLevelClient).extracting("client.connmgr.pool.maxTotal").isEqualTo(100);
			assertThat(lowLevelClient).extracting("client.defaultConfig.cookieSpec").isEqualTo("rfc6265-lax");
		});
	}

	@Test
	void configureWithNoTimeoutsApplyDefaults() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(RestHighLevelClient.class);
			RestHighLevelClient restClient = context.getBean(RestHighLevelClient.class);
			assertTimeouts(restClient, Duration.ofMillis(RestClientBuilder.DEFAULT_CONNECT_TIMEOUT_MILLIS),
					Duration.ofMillis(RestClientBuilder.DEFAULT_SOCKET_TIMEOUT_MILLIS));
		});
	}

	@Test
	void configureWithLegacyCustomTimeouts() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.rest.connection-timeout=15s",
				"spring.elasticsearch.rest.read-timeout=1m").run((context) -> {
					assertThat(context).hasSingleBean(RestHighLevelClient.class);
					RestHighLevelClient restClient = context.getBean(RestHighLevelClient.class);
					assertTimeouts(restClient, Duration.ofSeconds(15), Duration.ofMinutes(1));
				});
	}

	@Test
	void configureWithCustomTimeouts() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.connection-timeout=15s",
				"spring.elasticsearch.socket-timeout=1m").run((context) -> {
					assertThat(context).hasSingleBean(RestHighLevelClient.class);
					RestHighLevelClient restClient = context.getBean(RestHighLevelClient.class);
					assertTimeouts(restClient, Duration.ofSeconds(15), Duration.ofMinutes(1));
				});
	}

	private static void assertTimeouts(RestHighLevelClient restClient, Duration connectTimeout, Duration readTimeout) {
		assertThat(restClient.getLowLevelClient()).extracting("client.defaultConfig.socketTimeout")
				.isEqualTo(Math.toIntExact(readTimeout.toMillis()));
		assertThat(restClient.getLowLevelClient()).extracting("client.defaultConfig.connectTimeout")
				.isEqualTo(Math.toIntExact(connectTimeout.toMillis()));
	}

	@ParameterizedPropertyPrefixTest
	void configureUriWithNoScheme(String prefix) {
		this.contextRunner.withPropertyValues(prefix + "uris=localhost:9876").run((context) -> {
			RestClient client = context.getBean(RestHighLevelClient.class).getLowLevelClient();
			assertThat(client.getNodes().stream().map(Node::getHost).map(HttpHost::toString))
					.containsExactly("http://localhost:9876");
		});
	}

	@ParameterizedPropertyPrefixTest
	void configureUriWithUsernameOnly(String prefix) {
		this.contextRunner.withPropertyValues(prefix + "uris=http://user@localhost:9200").run((context) -> {
			RestClient client = context.getBean(RestHighLevelClient.class).getLowLevelClient();
			assertThat(client.getNodes().stream().map(Node::getHost).map(HttpHost::toString))
					.containsExactly("http://localhost:9200");
			assertThat(client).extracting("client")
					.extracting("credentialsProvider", InstanceOfAssertFactories.type(CredentialsProvider.class))
					.satisfies((credentialsProvider) -> {
						Credentials credentials = credentialsProvider.getCredentials(new AuthScope("localhost", 9200));
						assertThat(credentials.getUserPrincipal().getName()).isEqualTo("user");
						assertThat(credentials.getPassword()).isNull();
					});
		});
	}

	@ParameterizedPropertyPrefixTest
	void configureUriWithUsernameAndEmptyPassword(String prefix) {
		this.contextRunner.withPropertyValues(prefix + "uris=http://user:@localhost:9200").run((context) -> {
			RestClient client = context.getBean(RestHighLevelClient.class).getLowLevelClient();
			assertThat(client.getNodes().stream().map(Node::getHost).map(HttpHost::toString))
					.containsExactly("http://localhost:9200");
			assertThat(client).extracting("client")
					.extracting("credentialsProvider", InstanceOfAssertFactories.type(CredentialsProvider.class))
					.satisfies((credentialsProvider) -> {
						Credentials credentials = credentialsProvider.getCredentials(new AuthScope("localhost", 9200));
						assertThat(credentials.getUserPrincipal().getName()).isEqualTo("user");
						assertThat(credentials.getPassword()).isEmpty();
					});
		});
	}

	@ParameterizedPropertyPrefixTest
	void configureUriWithUsernameAndPasswordWhenUsernameAndPasswordPropertiesSet(String prefix) {
		this.contextRunner.withPropertyValues(prefix + "uris=http://user:password@localhost:9200,localhost:9201",
				prefix + "username=admin", prefix + "password=admin").run((context) -> {
					RestClient client = context.getBean(RestHighLevelClient.class).getLowLevelClient();
					assertThat(client.getNodes().stream().map(Node::getHost).map(HttpHost::toString))
							.containsExactly("http://localhost:9200", "http://localhost:9201");
					assertThat(client).extracting("client")
							.extracting("credentialsProvider",
									InstanceOfAssertFactories.type(CredentialsProvider.class))
							.satisfies((credentialsProvider) -> {
								Credentials uriCredentials = credentialsProvider
										.getCredentials(new AuthScope("localhost", 9200));
								assertThat(uriCredentials.getUserPrincipal().getName()).isEqualTo("user");
								assertThat(uriCredentials.getPassword()).isEqualTo("password");
								Credentials defaultCredentials = credentialsProvider
										.getCredentials(new AuthScope("localhost", 9201));
								assertThat(defaultCredentials.getUserPrincipal().getName()).isEqualTo("admin");
								assertThat(defaultCredentials.getPassword()).isEqualTo("admin");
							});
				});
	}

	@Test
	void configureWithCustomPathPrefix() {
		this.contextRunner.withPropertyValues("spring.elasticsearch.path-prefix=/some/prefix").run((context) -> {
			RestClient client = context.getBean(RestHighLevelClient.class).getLowLevelClient();
			assertThat(client).extracting("pathPrefix").isEqualTo("/some/prefix");
		});
	}

	@Test
	void configureWithoutSnifferLibraryShouldNotCreateSniffer() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("org.elasticsearch.client.sniff"))
				.run((context) -> assertThat(context).hasSingleBean(RestHighLevelClient.class)
						.doesNotHaveBean(Sniffer.class));
	}

	@Test
	void configureShouldCreateSnifferUsingRestHighLevelClient() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(Sniffer.class);
			assertThat(context.getBean(Sniffer.class)).hasFieldOrPropertyWithValue("restClient",
					context.getBean(RestHighLevelClient.class).getLowLevelClient());
			// Validate shutdown order as the sniffer must be shutdown before the client
			assertThat(context.getBeanFactory().getDependentBeans("elasticsearchRestHighLevelClient"))
					.contains("elasticsearchSniffer");
		});
	}

	@ParameterizedSnifferPropertyPrefixTest
	void configureWithCustomSnifferSettings(String prefix) {
		this.contextRunner.withPropertyValues(prefix + "interval=180s", prefix + "delay-after-failure=30s")
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
			verifyNoInteractions(customSniffer);
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class BuilderCustomizerConfiguration {

		@Bean
		RestClientBuilderCustomizer myCustomizer() {
			return new RestClientBuilderCustomizer() {

				@Override
				public void customize(RestClientBuilder builder) {
					builder.setPathPrefix("/test");
				}

				@Override
				public void customize(HttpAsyncClientBuilder builder) {
					builder.setMaxConnTotal(100);
				}

				@Override
				public void customize(RequestConfig.Builder builder) {
					builder.setCookieSpec("rfc6265-lax");
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRestHighLevelClientConfiguration {

		@Bean
		RestHighLevelClient customRestHighLevelClient(RestClientBuilder builder) {
			return new RestHighLevelClient(builder);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TwoCustomRestHighLevelClientConfiguration {

		@Bean
		RestHighLevelClient customRestHighLevelClient(RestClientBuilder builder) {
			return new RestHighLevelClient(builder);
		}

		@Bean
		RestHighLevelClient customRestHighLevelClient1(RestClientBuilder builder) {
			return new RestHighLevelClient(builder);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomRestClientConfiguration {

		@Bean
		RestClient customRestClient(RestClientBuilder builder) {
			return builder.build();
		}

	}

	@ParameterizedTest
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ValueSource(strings = { "spring.elasticsearch.rest.", "spring.elasticsearch." })
	static @interface ParameterizedPropertyPrefixTest {

	}

	@ParameterizedTest
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@ValueSource(strings = { "spring.elasticsearch.rest.sniffer.", "spring.elasticsearch.restclient.sniffer." })
	static @interface ParameterizedSnifferPropertyPrefixTest {

	}

}
