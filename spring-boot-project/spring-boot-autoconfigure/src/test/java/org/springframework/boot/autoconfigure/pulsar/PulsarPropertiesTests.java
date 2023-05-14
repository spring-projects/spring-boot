/*
 * Copyright 2022-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.pulsar;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.ConsumerCryptoFailureAction;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.ProducerAccessMode;
import org.apache.pulsar.client.api.ProducerBuilder;
import org.apache.pulsar.client.api.ProducerCryptoFailureAction;
import org.apache.pulsar.client.api.ProxyProtocol;
import org.apache.pulsar.client.api.PulsarClientException.UnsupportedAuthenticationException;
import org.apache.pulsar.client.api.ReaderBuilder;
import org.apache.pulsar.client.api.RegexSubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.common.schema.SchemaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.SchemaInfo;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.TypeMapping;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link PulsarProperties}.
 *
 * @author Chris Bono
 * @author Christophe Bornet
 * @author Soby Chacko
 */
public class PulsarPropertiesTests {

	private PulsarProperties newConfigPropsFromUserProps(Map<String, String> map) {
		var targetProps = new PulsarProperties();
		var source = new MapConfigurationPropertySource(map);
		new Binder(source).bind("spring.pulsar", Bindable.ofInstance(targetProps));
		return targetProps;
	}

	@Nested
	class ClientPropertiesTests {

		@Test
		void clientProperties() {
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.client.service-url", "my-service-url");
			props.put("spring.pulsar.client.listener-name", "my-listener");
			props.put("spring.pulsar.client.operation-timeout", "1s");
			props.put("spring.pulsar.client.lookup-timeout", "2s");
			props.put("spring.pulsar.client.num-io-threads", "3");
			props.put("spring.pulsar.client.num-listener-threads", "4");
			props.put("spring.pulsar.client.num-connections-per-broker", "5");
			props.put("spring.pulsar.client.use-tcp-no-delay", "false");
			props.put("spring.pulsar.client.use-tls", "true");
			props.put("spring.pulsar.client.tls-hostname-verification-enable", "true");
			props.put("spring.pulsar.client.tls-trust-certs-file-path", "my-trust-certs-file-path");
			props.put("spring.pulsar.client.tls-certificate-file-path", "my-certificate-file-path");
			props.put("spring.pulsar.client.tls-key-file-path", "my-key-file-path");
			props.put("spring.pulsar.client.tls-allow-insecure-connection", "true");
			props.put("spring.pulsar.client.use-key-store-tls", "true");
			props.put("spring.pulsar.client.ssl-provider", "my-ssl-provider");
			props.put("spring.pulsar.client.tls-trust-store-type", "my-trust-store-type");
			props.put("spring.pulsar.client.tls-trust-store-path", "my-trust-store-path");
			props.put("spring.pulsar.client.tls-trust-store-password", "my-trust-store-password");
			props.put("spring.pulsar.client.tls-ciphers[0]", "my-tls-cipher");
			props.put("spring.pulsar.client.tls-protocols[0]", "my-tls-protocol");
			props.put("spring.pulsar.client.stats-interval", "6s");
			props.put("spring.pulsar.client.max-concurrent-lookup-request", "7");
			props.put("spring.pulsar.client.max-lookup-request", "8");
			props.put("spring.pulsar.client.max-lookup-redirects", "9");
			props.put("spring.pulsar.client.max-number-of-rejected-request-per-connection", "10");
			props.put("spring.pulsar.client.keep-alive-interval", "11s");
			props.put("spring.pulsar.client.connection-timeout", "12s");
			props.put("spring.pulsar.client.request-timeout", "13s");
			props.put("spring.pulsar.client.initial-backoff-interval", "14s");
			props.put("spring.pulsar.client.max-backoff-interval", "15s");
			props.put("spring.pulsar.client.enable-busy-wait", "true");
			props.put("spring.pulsar.client.memory-limit", "16B");
			props.put("spring.pulsar.client.proxy-service-url", "my-proxy-service-url");
			props.put("spring.pulsar.client.proxy-protocol", "sni");
			props.put("spring.pulsar.client.enable-transaction", "true");
			props.put("spring.pulsar.client.dns-lookup-bind-address", "my-dns-lookup-bind-address");
			props.put("spring.pulsar.client.dns-lookup-bind-port", "17");
			props.put("spring.pulsar.client.socks5-proxy-address", "my-socks5-proxy-address");
			props.put("spring.pulsar.client.socks5-proxy-username", "my-socks5-proxy-username");
			props.put("spring.pulsar.client.socks5-proxy-password", "my-socks5-proxy-password");
			var clientProps = newConfigPropsFromUserProps(props).getClient();

			assertThat(clientProps.getServiceUrl()).isEqualTo("my-service-url");
			assertThat(clientProps.getListenerName()).isEqualTo("my-listener");
			assertThat(clientProps.getOperationTimeout()).isEqualTo(Duration.ofMillis(1000));
			assertThat(clientProps.getLookupTimeout()).isEqualTo(Duration.ofMillis(2000));
			assertThat(clientProps.getNumIoThreads()).isEqualTo(3);
			assertThat(clientProps.getNumListenerThreads()).isEqualTo(4);
			assertThat(clientProps.getNumConnectionsPerBroker()).isEqualTo(5);
			assertThat(clientProps.getUseTcpNoDelay()).isFalse();
			assertThat(clientProps.getUseTls()).isTrue();
			assertThat(clientProps.getTlsHostnameVerificationEnable()).isTrue();
			assertThat(clientProps.getTlsTrustCertsFilePath()).isEqualTo("my-trust-certs-file-path");
			assertThat(clientProps.getTlsCertificateFilePath()).isEqualTo("my-certificate-file-path");
			assertThat(clientProps.getTlsKeyFilePath()).isEqualTo("my-key-file-path");
			assertThat(clientProps.getTlsAllowInsecureConnection()).isTrue();
			assertThat(clientProps.getUseKeyStoreTls()).isTrue();
			assertThat(clientProps.getSslProvider()).isEqualTo("my-ssl-provider");
			assertThat(clientProps.getTlsTrustStoreType()).isEqualTo("my-trust-store-type");
			assertThat(clientProps.getTlsTrustStorePath()).isEqualTo("my-trust-store-path");
			assertThat(clientProps.getTlsTrustStorePassword()).isEqualTo("my-trust-store-password");
			assertThat(clientProps.getTlsCiphers()).containsExactly("my-tls-cipher");
			assertThat(clientProps.getTlsProtocols()).containsExactly("my-tls-protocol");
			assertThat(clientProps.getStatsInterval()).isEqualTo(Duration.ofSeconds(6));
			assertThat(clientProps.getMaxConcurrentLookupRequest()).isEqualTo(7);
			assertThat(clientProps.getMaxLookupRequest()).isEqualTo(8);
			assertThat(clientProps.getMaxLookupRedirects()).isEqualTo(9);
			assertThat(clientProps.getMaxNumberOfRejectedRequestPerConnection()).isEqualTo(10);
			assertThat(clientProps.getKeepAliveInterval()).isEqualTo(Duration.ofSeconds(11));
			assertThat(clientProps.getConnectionTimeout()).isEqualTo(Duration.ofMillis(12000));
			assertThat(clientProps.getRequestTimeout()).isEqualTo(Duration.ofMillis(13_000));
			assertThat(clientProps.getInitialBackoffInterval()).isEqualTo(Duration.ofMillis(14000));
			assertThat(clientProps.getMaxBackoffInterval()).isEqualTo(Duration.ofMillis(15000));
			assertThat(clientProps.getEnableBusyWait()).isTrue();
			assertThat(clientProps.getMemoryLimit()).isEqualTo(DataSize.ofBytes(16));
			assertThat(clientProps.getProxyServiceUrl()).isEqualTo("my-proxy-service-url");
			assertThat(clientProps.getProxyProtocol()).isEqualTo(ProxyProtocol.SNI);
			assertThat(clientProps.getEnableTransaction()).isTrue();
			assertThat(clientProps.getDnsLookupBindAddress()).isEqualTo("my-dns-lookup-bind-address");
			assertThat(clientProps.getDnsLookupBindPort()).isEqualTo(17);
			assertThat(clientProps.getSocks5ProxyAddress()).isEqualTo("my-socks5-proxy-address");
			assertThat(clientProps.getSocks5ProxyUsername()).isEqualTo("my-socks5-proxy-username");
			assertThat(clientProps.getSocks5ProxyPassword()).isEqualTo("my-socks5-proxy-password");
		}

		@Test
		void authenticationUsingAuthParamsString() {
			var authPluginClassName = "org.apache.pulsar.client.impl.auth.AuthenticationToken";
			var authParamsStr = "{\"token\":\"1234\"}";
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.client.auth-plugin-class-name", authPluginClassName);
			props.put("spring.pulsar.client.auth-params", authParamsStr);
			var clientProps = newConfigPropsFromUserProps(props).getClient();
			assertThat(clientProps.getAuthPluginClassName()).isEqualTo(authPluginClassName);
			assertThat(clientProps.getAuthParams()).isEqualTo(authParamsStr);
		}

		@Test
		void authenticationUsingAuthenticationMap() {
			var authPluginClassName = "org.apache.pulsar.client.impl.auth.AuthenticationToken";
			var authToken = "1234";
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.client.auth-plugin-class-name", authPluginClassName);
			props.put("spring.pulsar.client.authentication.token", authToken);
			var configProps = newConfigPropsFromUserProps(props);
			var clientProps = configProps.getClient();
			assertThat(clientProps.getAuthPluginClassName()).isEqualTo(authPluginClassName);
			assertThat(clientProps.getAuthentication()).containsEntry("token", authToken);
		}

	}

	@Nested
	class AdminPropertiesTests {

		private final String authPluginClassName = "org.apache.pulsar.client.impl.auth.AuthenticationToken";

		private final String authParamsStr = "{\"token\":\"1234\"}";

		private final String authToken = "1234";

		@Test
		void adminProperties() {
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.administration.service-url", "my-service-url");
			props.put("spring.pulsar.administration.connection-timeout", "12s");
			props.put("spring.pulsar.administration.read-timeout", "13s");
			props.put("spring.pulsar.administration.request-timeout", "14s");
			props.put("spring.pulsar.administration.auto-cert-refresh-time", "15s");
			props.put("spring.pulsar.administration.tls-hostname-verification-enable", "true");
			props.put("spring.pulsar.administration.tls-trust-certs-file-path", "my-trust-certs-file-path");
			props.put("spring.pulsar.administration.tls-certificate-file-path", "my-certificate-file-path");
			props.put("spring.pulsar.administration.tls-key-file-path", "my-key-file-path");
			props.put("spring.pulsar.administration.tls-allow-insecure-connection", "true");
			props.put("spring.pulsar.administration.use-key-store-tls", "true");
			props.put("spring.pulsar.administration.ssl-provider", "my-ssl-provider");
			props.put("spring.pulsar.administration.tls-trust-store-type", "my-trust-store-type");
			props.put("spring.pulsar.administration.tls-trust-store-path", "my-trust-store-path");
			props.put("spring.pulsar.administration.tls-trust-store-password", "my-trust-store-password");
			props.put("spring.pulsar.administration.tls-ciphers[0]", "my-tls-cipher");
			props.put("spring.pulsar.administration.tls-protocols[0]", "my-tls-protocol");
			var adminProps = newConfigPropsFromUserProps(props).getAdministration();

			// Verify properties
			assertThat(adminProps.getServiceUrl()).isEqualTo("my-service-url");
			assertThat(adminProps.getConnectionTimeout()).isEqualTo(Duration.ofMillis(12_000));
			assertThat(adminProps.getReadTimeout()).isEqualTo(Duration.ofMillis(13_000));
			assertThat(adminProps.getRequestTimeout()).isEqualTo(Duration.ofMillis(14_000));
			assertThat(adminProps.getAutoCertRefreshTime()).isEqualTo(Duration.ofMillis(15_000));
			assertThat(adminProps.isTlsHostnameVerificationEnable()).isTrue();
			assertThat(adminProps.getTlsTrustCertsFilePath()).isEqualTo("my-trust-certs-file-path");
			assertThat(adminProps.getTlsCertificateFilePath()).isEqualTo("my-certificate-file-path");
			assertThat(adminProps.getTlsKeyFilePath()).isEqualTo("my-key-file-path");
			assertThat(adminProps.isTlsAllowInsecureConnection()).isTrue();
			assertThat(adminProps.isUseKeyStoreTls()).isTrue();
			assertThat(adminProps.getSslProvider()).isEqualTo("my-ssl-provider");
			assertThat(adminProps.getTlsTrustStoreType()).isEqualTo("my-trust-store-type");
			assertThat(adminProps.getTlsTrustStorePath()).isEqualTo("my-trust-store-path");
			assertThat(adminProps.getTlsTrustStorePassword()).isEqualTo("my-trust-store-password");
			assertThat(adminProps.getTlsCiphers()).containsExactly("my-tls-cipher");
			assertThat(adminProps.getTlsProtocols()).containsExactly("my-tls-protocol");

			// Verify customizer
			var adminBuilder = mock(PulsarAdminBuilder.class);
			var adminCustomizer = adminProps.toPulsarAdminBuilderCustomizer();
			adminCustomizer.customize(adminBuilder);

			then(adminBuilder).should().serviceHttpUrl("my-service-url");
			then(adminBuilder).should().connectionTimeout(12_000, TimeUnit.MILLISECONDS);
			then(adminBuilder).should().readTimeout(13_000, TimeUnit.MILLISECONDS);
			then(adminBuilder).should().requestTimeout(14_000, TimeUnit.MILLISECONDS);
			then(adminBuilder).should().autoCertRefreshTime(15_000, TimeUnit.MILLISECONDS);
			then(adminBuilder).should().enableTlsHostnameVerification(true);
			then(adminBuilder).should().tlsTrustCertsFilePath("my-trust-certs-file-path");
			then(adminBuilder).should().tlsCertificateFilePath("my-certificate-file-path");
			then(adminBuilder).should().tlsKeyFilePath("my-key-file-path");
			then(adminBuilder).should().allowTlsInsecureConnection(true);
			then(adminBuilder).should().useKeyStoreTls(true);
			then(adminBuilder).should().sslProvider("my-ssl-provider");
			then(adminBuilder).should().tlsTrustStoreType("my-trust-store-type");
			then(adminBuilder).should().tlsTrustStorePath("my-trust-store-path");
			then(adminBuilder).should().tlsTrustStorePassword("my-trust-store-password");
			then(adminBuilder).should().tlsCiphers(Set.of("my-tls-cipher"));
			then(adminBuilder).should().tlsProtocols(Set.of("my-tls-protocol"));

		}

		@Test
		void authenticationUsingAuthParamsString() throws UnsupportedAuthenticationException {
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.administration.auth-plugin-class-name", this.authPluginClassName);
			props.put("spring.pulsar.administration.auth-params", this.authParamsStr);
			var adminProps = newConfigPropsFromUserProps(props).getAdministration();

			// Verify properties
			assertThat(adminProps.getAuthPluginClassName()).isEqualTo(this.authPluginClassName);
			assertThat(adminProps.getAuthParams()).isEqualTo(this.authParamsStr);

			// Verify customizer
			var adminBuilder = mock(PulsarAdminBuilder.class);
			var adminCustomizer = adminProps.toPulsarAdminBuilderCustomizer();
			adminCustomizer.customize(adminBuilder);
			then(adminBuilder).should().authentication(this.authPluginClassName, this.authParamsStr);
		}

		@Test
		void authenticationUsingAuthenticationMap() throws UnsupportedAuthenticationException {
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.administration.auth-plugin-class-name", this.authPluginClassName);
			props.put("spring.pulsar.administration.authentication.token", this.authToken);
			var adminProps = newConfigPropsFromUserProps(props).getAdministration();

			// Verify properties
			assertThat(adminProps.getAuthPluginClassName()).isEqualTo(this.authPluginClassName);
			assertThat(adminProps.getAuthentication()).containsEntry("token", this.authToken);

			// Verify customizer
			var adminBuilder = mock(PulsarAdminBuilder.class);
			var adminCustomizer = adminProps.toPulsarAdminBuilderCustomizer();
			adminCustomizer.customize(adminBuilder);
			then(adminBuilder).should().authentication(this.authPluginClassName, this.authParamsStr);
		}

		@Test
		void authenticationNotAllowedUsingBothAuthParamsStringAndAuthenticationMap() {
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.administration.auth-plugin-class-name", this.authPluginClassName);
			props.put("spring.pulsar.administration.auth-params", this.authParamsStr);
			props.put("spring.pulsar.administration.authentication.token", this.authToken);
			var adminProps = newConfigPropsFromUserProps(props).getAdministration();

			// Verify properties
			assertThat(adminProps.getAuthPluginClassName()).isEqualTo(this.authPluginClassName);
			assertThat(adminProps.getAuthentication()).containsEntry("token", this.authToken);
			assertThat(adminProps.getAuthParams()).isEqualTo(this.authParamsStr);

			// Verify customizer
			var adminCustomizer = adminProps.toPulsarAdminBuilderCustomizer();
			assertThatIllegalArgumentException()
				.isThrownBy(() -> adminCustomizer.customize(mock(PulsarAdminBuilder.class)))
				.withMessageContaining(
						"Cannot set both spring.pulsar.administration.authParams and spring.pulsar.administration.authentication.*");
		}

	}

	@Nested
	class DefaultsTypeMappingsPropertiesTests {

		@Test
		void emptyByDefault() {
			assertThat(new PulsarProperties().getDefaults().getTypeMappings()).isEmpty();
		}

		@Test
		void withTopicsOnly() {
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.defaults.type-mappings[0].message-type", Foo.class.getName());
			props.put("spring.pulsar.defaults.type-mappings[0].topic-name", "foo-topic");
			props.put("spring.pulsar.defaults.type-mappings[1].message-type", String.class.getName());
			props.put("spring.pulsar.defaults.type-mappings[1].topic-name", "string-topic");
			var configProps = newConfigPropsFromUserProps(props);
			assertThat(configProps.getDefaults().getTypeMappings()).containsExactly(
					new TypeMapping(Foo.class, "foo-topic", null), new TypeMapping(String.class, "string-topic", null));
		}

		@Test
		void withSchemaOnly() {
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.defaults.type-mappings[0].message-type", Foo.class.getName());
			props.put("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type", "JSON");
			var configProps = newConfigPropsFromUserProps(props);
			assertThat(configProps.getDefaults().getTypeMappings())
				.containsExactly(new TypeMapping(Foo.class, null, new SchemaInfo(SchemaType.JSON, null)));
		}

		@Test
		void withTopicAndSchema() {
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.defaults.type-mappings[0].message-type", Foo.class.getName());
			props.put("spring.pulsar.defaults.type-mappings[0].topic-name", "foo-topic");
			props.put("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type", "JSON");
			var configProps = newConfigPropsFromUserProps(props);
			assertThat(configProps.getDefaults().getTypeMappings())
				.containsExactly(new TypeMapping(Foo.class, "foo-topic", new SchemaInfo(SchemaType.JSON, null)));
		}

		@Test
		void withKeyValueSchema() {
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.defaults.type-mappings[0].message-type", Foo.class.getName());
			props.put("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type", "KEY_VALUE");
			props.put("spring.pulsar.defaults.type-mappings[0].schema-info.message-key-type", String.class.getName());
			var configProps = newConfigPropsFromUserProps(props);
			assertThat(configProps.getDefaults().getTypeMappings())
				.containsExactly(new TypeMapping(Foo.class, null, new SchemaInfo(SchemaType.KEY_VALUE, String.class)));
		}

		@Test
		void schemaTypeRequired() {
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.defaults.type-mappings[0].message-type", Foo.class.getName());
			props.put("spring.pulsar.defaults.type-mappings[0].schema-info.message-key-type", String.class.getName());
			assertThatExceptionOfType(BindException.class).isThrownBy(() -> newConfigPropsFromUserProps(props))
				.havingRootCause()
				.withMessageContaining("schemaType must not be null");
		}

		@Test
		void schemaTypeNoneNotAllowed() {
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.defaults.type-mappings[0].message-type", Foo.class.getName());
			props.put("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type", "NONE");
			assertThatExceptionOfType(BindException.class).isThrownBy(() -> newConfigPropsFromUserProps(props))
				.havingRootCause()
				.withMessageContaining("schemaType NONE not supported");
		}

		@Test
		void messageKeyTypeOnlyAllowedForKeyValueSchemaType() {
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.defaults.type-mappings[0].message-type", Foo.class.getName());
			props.put("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type", "JSON");
			props.put("spring.pulsar.defaults.type-mappings[0].schema-info.message-key-type", String.class.getName());
			assertThatExceptionOfType(BindException.class).isThrownBy(() -> newConfigPropsFromUserProps(props))
				.havingRootCause()
				.withMessageContaining("messageKeyType can only be set when schemaType is KEY_VALUE");
		}

		record Foo(String value) {
		}

	}

	@Nested
	class ProducerPropertiesTests {

		private ProducerConfigProperties producerProps;

		@BeforeEach
		void producerTestProps() {
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.producer.topic-name", "my-topic");
			props.put("spring.pulsar.producer.producer-name", "my-producer");
			props.put("spring.pulsar.producer.send-timeout", "2s");
			props.put("spring.pulsar.producer.block-if-queue-full", "true");
			props.put("spring.pulsar.producer.max-pending-messages", "3");
			props.put("spring.pulsar.producer.max-pending-messages-across-partitions", "4");
			props.put("spring.pulsar.producer.message-routing-mode", "custompartition");
			props.put("spring.pulsar.producer.hashing-scheme", "murmur3_32hash");
			props.put("spring.pulsar.producer.crypto-failure-action", "send");
			props.put("spring.pulsar.producer.batching-max-publish-delay", "5s");
			props.put("spring.pulsar.producer.batching-partition-switch-frequency-by-publish-delay", "6");
			props.put("spring.pulsar.producer.batching-max-messages", "7");
			props.put("spring.pulsar.producer.batching-max-bytes", "8");
			props.put("spring.pulsar.producer.batching-enabled", "false");
			props.put("spring.pulsar.producer.chunking-enabled", "true");
			props.put("spring.pulsar.producer.encryption-keys[0]", "my-key");
			props.put("spring.pulsar.producer.compression-type", "lz4");
			props.put("spring.pulsar.producer.initial-sequence-id", "9");
			props.put("spring.pulsar.producer.producer-access-mode", "exclusive");
			props.put("spring.pulsar.producer.lazy-start=partitioned-producers", "true");
			props.put("spring.pulsar.producer.properties[my-prop]", "my-prop-value");
			this.producerProps = newConfigPropsFromUserProps(props).getProducer();
		}

		@Test
		void producerProperties() {
			assertThat(this.producerProps.getTopicName()).isEqualTo("my-topic");
			assertThat(this.producerProps.getProducerName()).isEqualTo("my-producer");
			assertThat(this.producerProps.getSendTimeout()).isEqualTo(Duration.ofMillis(2000));
			assertThat(this.producerProps.getBlockIfQueueFull()).isTrue();
			assertThat(this.producerProps.getMaxPendingMessages()).isEqualTo(3);
			assertThat(this.producerProps.getMaxPendingMessagesAcrossPartitions()).isEqualTo(4);
			assertThat(this.producerProps.getMessageRoutingMode()).isEqualTo(MessageRoutingMode.CustomPartition);
			assertThat(this.producerProps.getHashingScheme()).isEqualTo(HashingScheme.Murmur3_32Hash);
			assertThat(this.producerProps.getCryptoFailureAction()).isEqualTo(ProducerCryptoFailureAction.SEND);
			assertThat(this.producerProps.getBatchingMaxPublishDelay()).isEqualTo(Duration.ofMillis(5000));
			assertThat(this.producerProps.getBatchingPartitionSwitchFrequencyByPublishDelay()).isEqualTo(6);
			assertThat(this.producerProps.getBatchingMaxMessages()).isEqualTo(7);
			assertThat(this.producerProps.getBatchingMaxBytes()).isEqualTo(DataSize.ofBytes(8));
			assertThat(this.producerProps.getBatchingEnabled()).isFalse();
			assertThat(this.producerProps.getChunkingEnabled()).isTrue();
			assertThat(this.producerProps.getEncryptionKeys()).containsExactly("my-key");
			assertThat(this.producerProps.getCompressionType()).isEqualTo(CompressionType.LZ4);
			assertThat(this.producerProps.getInitialSequenceId()).isEqualTo(9);
			assertThat(this.producerProps.getProducerAccessMode()).isEqualTo(ProducerAccessMode.Exclusive);
			assertThat(this.producerProps.getLazyStartPartitionedProducers()).isTrue();
			assertThat(this.producerProps.getProperties()).containsExactly(entry("my-prop", "my-prop-value"));
		}

		@SuppressWarnings({ "unchecked", "deprecation" })
		@Test
		void toProducerCustomizer() {
			var producerBuilder = mock(ProducerBuilder.class);
			var customizer = this.producerProps.toProducerBuilderCustomizer();
			customizer.customize(producerBuilder);
			then(producerBuilder).should().topic("my-topic");
			then(producerBuilder).should().producerName("my-producer");
			then(producerBuilder).should().sendTimeout(2_000, TimeUnit.MILLISECONDS);
			then(producerBuilder).should().blockIfQueueFull(true);
			then(producerBuilder).should().maxPendingMessages(3);
			then(producerBuilder).should().maxPendingMessagesAcrossPartitions(4);
			then(producerBuilder).should().messageRoutingMode(MessageRoutingMode.CustomPartition);
			then(producerBuilder).should().hashingScheme(HashingScheme.Murmur3_32Hash);
			then(producerBuilder).should().cryptoFailureAction(ProducerCryptoFailureAction.SEND);
			then(producerBuilder).should().batchingMaxPublishDelay(5_000, TimeUnit.MILLISECONDS);
			then(producerBuilder).should().roundRobinRouterBatchingPartitionSwitchFrequency(6);
			then(producerBuilder).should().batchingMaxMessages(7);
			then(producerBuilder).should().batchingMaxBytes(8);
			then(producerBuilder).should().enableBatching(false);
			then(producerBuilder).should().enableChunking(true);
			then(producerBuilder).should().addEncryptionKey("my-key");
			then(producerBuilder).should().compressionType(CompressionType.LZ4);
			then(producerBuilder).should().initialSequenceId(9);
			then(producerBuilder).should().accessMode(ProducerAccessMode.Exclusive);
			then(producerBuilder).should().enableLazyStartPartitionedProducers(true);
			then(producerBuilder).should().properties(Map.of("my-prop", "my-prop-value"));
		}

	}

	@Nested
	class ConsumerPropertiesTests {

		private ConsumerConfigProperties consumerProps;

		@BeforeEach
		void consumerTestProps() {
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.consumer.topics[0]", "my-topic");
			props.put("spring.pulsar.consumer.topics-pattern", "my-pattern");
			props.put("spring.pulsar.consumer.subscription-name", "my-subscription");
			props.put("spring.pulsar.consumer.subscription-type", "shared");
			props.put("spring.pulsar.consumer.subscription-properties[my-sub-prop]", "my-sub-prop-value");
			props.put("spring.pulsar.consumer.subscription-mode", "nondurable");
			props.put("spring.pulsar.consumer.receiver-queue-size", "1");
			props.put("spring.pulsar.consumer.acknowledgements-group-time", "2s");
			props.put("spring.pulsar.consumer.negative-ack-redelivery-delay", "3s");
			props.put("spring.pulsar.consumer.max-total-receiver-queue-size-across-partitions", "5");
			props.put("spring.pulsar.consumer.consumer-name", "my-consumer");
			props.put("spring.pulsar.consumer.ack-timeout", "6s");
			props.put("spring.pulsar.consumer.tick-duration", "7s");
			props.put("spring.pulsar.consumer.priority-level", "8");
			props.put("spring.pulsar.consumer.crypto-failure-action", "discard");
			props.put("spring.pulsar.consumer.properties[my-prop]", "my-prop-value");
			props.put("spring.pulsar.consumer.read-compacted", "true");
			props.put("spring.pulsar.consumer.subscription-initial-position", "earliest");
			props.put("spring.pulsar.consumer.pattern-auto-discovery-period", "9");
			props.put("spring.pulsar.consumer.regex-subscription-mode", "all-topics");
			props.put("spring.pulsar.consumer.dead-letter-policy.max-redeliver-count", "4");
			props.put("spring.pulsar.consumer.dead-letter-policy.retry-letter-topic", "my-retry-topic");
			props.put("spring.pulsar.consumer.dead-letter-policy.dead-letter-topic", "my-dlt-topic");
			props.put("spring.pulsar.consumer.dead-letter-policy.initial-subscription-name", "my-initial-subscription");
			props.put("spring.pulsar.consumer.retry-enable", "true");
			props.put("spring.pulsar.consumer.auto-update-partitions", "false");
			props.put("spring.pulsar.consumer.auto-update-partitions-interval", "10s");
			props.put("spring.pulsar.consumer.replicate-subscription-state", "true");
			props.put("spring.pulsar.consumer.reset-include-head", "true");
			props.put("spring.pulsar.consumer.batch-index-ack-enabled", "true");
			props.put("spring.pulsar.consumer.ack-receipt-enabled", "true");
			props.put("spring.pulsar.consumer.pool-messages", "true");
			props.put("spring.pulsar.consumer.start-paused", "true");
			props.put("spring.pulsar.consumer.auto-ack-oldest-chunked-message-on-queue-full", "false");
			props.put("spring.pulsar.consumer.max-pending-chunked-message", "11");
			props.put("spring.pulsar.consumer.expire-time-of-incomplete-chunked-message", "12s");
			this.consumerProps = newConfigPropsFromUserProps(props).getConsumer();
		}

		@Test
		void consumerProperties() {
			assertThat(this.consumerProps.getTopics()).containsExactly("my-topic");
			assertThat(this.consumerProps.getTopicsPattern().toString()).isEqualTo("my-pattern");
			assertThat(this.consumerProps.getSubscriptionName()).isEqualTo("my-subscription");
			assertThat(this.consumerProps.getSubscriptionType()).isEqualTo(SubscriptionType.Shared);
			assertThat(this.consumerProps.getSubscriptionProperties())
				.containsExactly(entry("my-sub-prop", "my-sub-prop-value"));
			assertThat(this.consumerProps.getSubscriptionMode()).isEqualTo(SubscriptionMode.NonDurable);
			assertThat(this.consumerProps.getReceiverQueueSize()).isEqualTo(1);
			assertThat(this.consumerProps.getAcknowledgementsGroupTime()).isEqualTo(Duration.ofMillis(2_000));
			assertThat(this.consumerProps.getNegativeAckRedeliveryDelay()).isEqualTo(Duration.ofMillis(3_000));
			assertThat(this.consumerProps.getMaxTotalReceiverQueueSizeAcrossPartitions()).isEqualTo(5);
			assertThat(this.consumerProps.getConsumerName()).isEqualTo("my-consumer");
			assertThat(this.consumerProps.getAckTimeout()).isEqualTo(Duration.ofMillis(6_000));
			assertThat(this.consumerProps.getTickDuration()).isEqualTo(Duration.ofMillis(7_000));
			assertThat(this.consumerProps.getPriorityLevel()).isEqualTo(8);
			assertThat(this.consumerProps.getCryptoFailureAction()).isEqualTo(ConsumerCryptoFailureAction.DISCARD);
			assertThat(this.consumerProps.getProperties()).containsExactly(entry("my-prop", "my-prop-value"));
			assertThat(this.consumerProps.getReadCompacted()).isTrue();
			assertThat(this.consumerProps.getSubscriptionInitialPosition())
				.isEqualTo(SubscriptionInitialPosition.Earliest);
			assertThat(this.consumerProps.getPatternAutoDiscoveryPeriod()).isEqualTo(9);
			assertThat(this.consumerProps.getRegexSubscriptionMode()).isEqualTo(RegexSubscriptionMode.AllTopics);
			assertThat(this.consumerProps.getDeadLetterPolicy()).satisfies((dlp) -> {
				assertThat(dlp.getMaxRedeliverCount()).isEqualTo(4);
				assertThat(dlp.getRetryLetterTopic()).isEqualTo("my-retry-topic");
				assertThat(dlp.getDeadLetterTopic()).isEqualTo("my-dlt-topic");
				assertThat(dlp.getInitialSubscriptionName()).isEqualTo("my-initial-subscription");
			});
			assertThat(this.consumerProps.getRetryEnable()).isTrue();
			assertThat(this.consumerProps.getAutoUpdatePartitions()).isFalse();
			assertThat(this.consumerProps.getAutoUpdatePartitionsInterval()).isEqualTo(Duration.ofMillis(10_000));
			assertThat(this.consumerProps.getReplicateSubscriptionState()).isTrue();
			assertThat(this.consumerProps.getResetIncludeHead()).isTrue();
			assertThat(this.consumerProps.getBatchIndexAckEnabled()).isTrue();
			assertThat(this.consumerProps.getAckReceiptEnabled()).isTrue();
			assertThat(this.consumerProps.getPoolMessages()).isTrue();
			assertThat(this.consumerProps.getStartPaused()).isTrue();
			assertThat(this.consumerProps.getAutoAckOldestChunkedMessageOnQueueFull()).isFalse();
			assertThat(this.consumerProps.getMaxPendingChunkedMessage()).isEqualTo(11);
			assertThat(this.consumerProps.getExpireTimeOfIncompleteChunkedMessage())
				.isEqualTo(Duration.ofMillis(12_000));
		}

		@SuppressWarnings("unchecked")
		@Test
		void toConsumerCustomizer() {
			var consumerBuilder = mock(ConsumerBuilder.class);
			var customizer = this.consumerProps.toConsumerBuilderCustomizer();
			customizer.customize(consumerBuilder);
			then(consumerBuilder).should().topics(List.of("my-topic"));
			var argCaptor = ArgumentCaptor.forClass(Pattern.class);
			then(consumerBuilder).should().topicsPattern(argCaptor.capture());
			assertThat(argCaptor.getValue().pattern()).isEqualTo("my-pattern");
			then(consumerBuilder).should().subscriptionName("my-subscription");
			then(consumerBuilder).should().subscriptionType(SubscriptionType.Shared);
			then(consumerBuilder).should().subscriptionProperties(Map.of("my-sub-prop", "my-sub-prop-value"));
			then(consumerBuilder).should().subscriptionMode(SubscriptionMode.NonDurable);
			then(consumerBuilder).should().receiverQueueSize(1);
			then(consumerBuilder).should().acknowledgmentGroupTime(2_000, TimeUnit.MILLISECONDS);
			then(consumerBuilder).should().negativeAckRedeliveryDelay(3_000, TimeUnit.MILLISECONDS);
			then(consumerBuilder).should().maxTotalReceiverQueueSizeAcrossPartitions(5);
			then(consumerBuilder).should().consumerName("my-consumer");
			then(consumerBuilder).should().ackTimeout(6_000, TimeUnit.MILLISECONDS);
			then(consumerBuilder).should().ackTimeoutTickTime(7_000, TimeUnit.MILLISECONDS);
			then(consumerBuilder).should().priorityLevel(8);
			then(consumerBuilder).should().cryptoFailureAction(ConsumerCryptoFailureAction.DISCARD);
			then(consumerBuilder).should().properties(Map.of("my-prop", "my-prop-value"));
			then(consumerBuilder).should().readCompacted(true);
			then(consumerBuilder).should().subscriptionInitialPosition(SubscriptionInitialPosition.Earliest);
			then(consumerBuilder).should().patternAutoDiscoveryPeriod(9);
			then(consumerBuilder).should().subscriptionTopicsMode(RegexSubscriptionMode.AllTopics);
			then(consumerBuilder).should()
				.deadLetterPolicy(DeadLetterPolicy.builder()
					.maxRedeliverCount(4)
					.retryLetterTopic("my-retry-topic")
					.deadLetterTopic("my-dlt-topic")
					.initialSubscriptionName("my-initial-subscription")
					.build());
			then(consumerBuilder).should().enableRetry(true);
			then(consumerBuilder).should().autoUpdatePartitions(false);
			then(consumerBuilder).should().autoUpdatePartitionsInterval(10_000, TimeUnit.MILLISECONDS);
			then(consumerBuilder).should().replicateSubscriptionState(true);
			then(consumerBuilder).should().startMessageIdInclusive();
			then(consumerBuilder).should().enableBatchIndexAcknowledgment(true);
			then(consumerBuilder).should().isAckReceiptEnabled(true);
			then(consumerBuilder).should().poolMessages(true);
			then(consumerBuilder).should().startPaused(true);
			then(consumerBuilder).should().autoAckOldestChunkedMessageOnQueueFull(false);
			then(consumerBuilder).should().maxPendingChunkedMessage(11);
			then(consumerBuilder).should().expireTimeOfIncompleteChunkedMessage(12_000, TimeUnit.MILLISECONDS);
		}

		@SuppressWarnings("unchecked")
		@Test
		void toConsumerCustomizerResetDoesNotIncludeHead() {
			var consumerBuilder = mock(ConsumerBuilder.class);
			this.consumerProps.setResetIncludeHead(false);
			var customizer = this.consumerProps.toConsumerBuilderCustomizer();
			customizer.customize(consumerBuilder);
			then(consumerBuilder).should(never()).startMessageIdInclusive();
		}

	}

	@Nested
	class FunctionPropertiesTests {

		@Test
		void functionPropertiesWithDefaults() {
			var props = new HashMap<String, String>();
			var functionProps = newConfigPropsFromUserProps(props).getFunction();
			assertThat(functionProps.getFailFast()).isTrue();
			assertThat(functionProps.getPropagateFailures()).isTrue();
			assertThat(functionProps.getPropagateStopFailures()).isFalse();
		}

		@Test
		void functionPropertiesWitValues() {
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.function.fail-fast", "false");
			props.put("spring.pulsar.function.propagate-failures", "false");
			props.put("spring.pulsar.function.propagate-stop-failures", "true");
			var functionProps = newConfigPropsFromUserProps(props).getFunction();
			assertThat(functionProps.getFailFast()).isFalse();
			assertThat(functionProps.getPropagateFailures()).isFalse();
			assertThat(functionProps.getPropagateStopFailures()).isTrue();
		}

	}

	@Nested
	class ReaderPropertiesTests {

		private PulsarProperties.Reader readerProps;

		@BeforeEach
		void readerTestProps() {
			var props = new HashMap<String, String>();
			props.put("spring.pulsar.reader.topic-names", "my-topic");
			props.put("spring.pulsar.reader.receiver-queue-size", "100");
			props.put("spring.pulsar.reader.reader-name", "my-reader");
			props.put("spring.pulsar.reader.subscription-name", "my-subscription");
			props.put("spring.pulsar.reader.subscription-role-prefix", "sub-role");
			props.put("spring.pulsar.reader.read-compacted", "true");
			props.put("spring.pulsar.reader.reset-include-head", "true");
			this.readerProps = newConfigPropsFromUserProps(props).getReader();
		}

		@Test
		void readerProperties() {
			assertThat(this.readerProps.getTopicNames()).containsExactly("my-topic");
			assertThat(this.readerProps.getReceiverQueueSize()).isEqualTo(100);
			assertThat(this.readerProps.getReaderName()).isEqualTo("my-reader");
			assertThat(this.readerProps.getSubscriptionName()).isEqualTo("my-subscription");
			assertThat(this.readerProps.getSubscriptionRolePrefix()).isEqualTo("sub-role");
			assertThat(this.readerProps.getReadCompacted()).isTrue();
			assertThat(this.readerProps.getResetIncludeHead()).isTrue();
		}

		@SuppressWarnings("unchecked")
		@Test
		void toReaderCustomizer() {
			var readerBuilder = mock(ReaderBuilder.class);
			var customizer = this.readerProps.toReaderBuilderCustomizer();
			customizer.customize(readerBuilder);
			then(readerBuilder).should().topics(List.of("my-topic"));
			then(readerBuilder).should().receiverQueueSize(100);
			then(readerBuilder).should().readerName("my-reader");
			then(readerBuilder).should().subscriptionName("my-subscription");
			then(readerBuilder).should().subscriptionRolePrefix("sub-role");
			then(readerBuilder).should().readCompacted(true);
			then(readerBuilder).should().startMessageIdInclusive();
		}

		@SuppressWarnings("unchecked")
		@Test
		void toReaderCustomizerResetDoesNotIncludeHead() {
			this.readerProps.setResetIncludeHead(false);
			var readerBuilder = mock(ReaderBuilder.class);
			var customizer = this.readerProps.toReaderBuilderCustomizer();
			customizer.customize(readerBuilder);
			then(readerBuilder).should(never()).startMessageIdInclusive();
		}

	}

}
