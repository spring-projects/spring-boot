/*
 * Copyright 2012-2024 the original author or authors.
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

import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.ProducerAccessMode;
import org.apache.pulsar.client.api.RegexSubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.common.schema.SchemaType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Defaults.SchemaInfo;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Defaults.TypeMapping;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Failover;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Failover.BackupCluster;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link PulsarProperties}.
 *
 * @author Chris Bono
 * @author Christophe Bornet
 * @author Soby Chacko
 * @author Phillip Webb
 * @author Swamy Mavuri
 */
class PulsarPropertiesTests {

	private PulsarProperties bindProperties(Map<String, String> map) {
		return new Binder(new MapConfigurationPropertySource(map)).bind("spring.pulsar", PulsarProperties.class).get();
	}

	@Nested
	class ClientProperties {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.client.service-url", "my-service-url");
			map.put("spring.pulsar.client.operation-timeout", "1s");
			map.put("spring.pulsar.client.lookup-timeout", "2s");
			map.put("spring.pulsar.client.connection-timeout", "12s");
			PulsarProperties.Client properties = bindProperties(map).getClient();
			assertThat(properties.getServiceUrl()).isEqualTo("my-service-url");
			assertThat(properties.getOperationTimeout()).isEqualTo(Duration.ofMillis(1000));
			assertThat(properties.getLookupTimeout()).isEqualTo(Duration.ofMillis(2000));
			assertThat(properties.getConnectionTimeout()).isEqualTo(Duration.ofMillis(12000));
		}

		@Test
		void bindAuthentication() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.client.authentication.plugin-class-name", "com.example.MyAuth");
			map.put("spring.pulsar.client.authentication.param.token", "1234");
			PulsarProperties.Client properties = bindProperties(map).getClient();
			assertThat(properties.getAuthentication().getPluginClassName()).isEqualTo("com.example.MyAuth");
			assertThat(properties.getAuthentication().getParam()).containsEntry("token", "1234");
		}

		@Test
		void bindFailover() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.client.service-url", "my-service-url");
			map.put("spring.pulsar.client.failover.delay", "30s");
			map.put("spring.pulsar.client.failover.switch-back-delay", "15s");
			map.put("spring.pulsar.client.failover.check-interval", "1s");
			map.put("spring.pulsar.client.failover.backup-clusters[0].service-url", "backup-service-url-1");
			map.put("spring.pulsar.client.failover.backup-clusters[0].authentication.plugin-class-name",
					"com.example.MyAuth1");
			map.put("spring.pulsar.client.failover.backup-clusters[0].authentication.param.token", "1234");
			map.put("spring.pulsar.client.failover.backup-clusters[1].service-url", "backup-service-url-2");
			map.put("spring.pulsar.client.failover.backup-clusters[1].authentication.plugin-class-name",
					"com.example.MyAuth2");
			map.put("spring.pulsar.client.failover.backup-clusters[1].authentication.param.token", "5678");
			PulsarProperties.Client properties = bindProperties(map).getClient();
			Failover failoverProperties = properties.getFailover();
			List<BackupCluster> backupClusters = properties.getFailover().getBackupClusters();
			assertThat(properties.getServiceUrl()).isEqualTo("my-service-url");
			assertThat(failoverProperties.getDelay()).isEqualTo(Duration.ofMillis(30000));
			assertThat(failoverProperties.getSwitchBackDelay()).isEqualTo(Duration.ofMillis(15000));
			assertThat(failoverProperties.getCheckInterval()).isEqualTo(Duration.ofMillis(1000));
			assertThat(backupClusters.get(0).getServiceUrl()).isEqualTo("backup-service-url-1");
			assertThat(backupClusters.get(0).getAuthentication().getPluginClassName()).isEqualTo("com.example.MyAuth1");
			assertThat(backupClusters.get(0).getAuthentication().getParam()).containsEntry("token", "1234");
			assertThat(backupClusters.get(1).getServiceUrl()).isEqualTo("backup-service-url-2");
			assertThat(backupClusters.get(1).getAuthentication().getPluginClassName()).isEqualTo("com.example.MyAuth2");
			assertThat(backupClusters.get(1).getAuthentication().getParam()).containsEntry("token", "5678");
		}

	}

	@Nested
	class AdminProperties {

		private final String authPluginClassName = "org.apache.pulsar.client.impl.auth.AuthenticationToken";

		private final String authToken = "1234";

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.admin.service-url", "my-service-url");
			map.put("spring.pulsar.admin.connection-timeout", "12s");
			map.put("spring.pulsar.admin.read-timeout", "13s");
			map.put("spring.pulsar.admin.request-timeout", "14s");
			PulsarProperties.Admin properties = bindProperties(map).getAdmin();
			assertThat(properties.getServiceUrl()).isEqualTo("my-service-url");
			assertThat(properties.getConnectionTimeout()).isEqualTo(Duration.ofSeconds(12));
			assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(13));
			assertThat(properties.getRequestTimeout()).isEqualTo(Duration.ofSeconds(14));
		}

		@Test
		void bindAuthentication() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.admin.authentication.plugin-class-name", this.authPluginClassName);
			map.put("spring.pulsar.admin.authentication.param.token", this.authToken);
			PulsarProperties.Admin properties = bindProperties(map).getAdmin();
			assertThat(properties.getAuthentication().getPluginClassName()).isEqualTo(this.authPluginClassName);
			assertThat(properties.getAuthentication().getParam()).containsEntry("token", this.authToken);
		}

	}

	@Nested
	class DefaultsProperties {

		@Test
		void bindWhenNoTypeMappings() {
			assertThat(new PulsarProperties().getDefaults().getTypeMappings()).isEmpty();
		}

		@Test
		void bindWhenTypeMappingsWithTopicsOnly() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.defaults.type-mappings[0].message-type", TestMessage.class.getName());
			map.put("spring.pulsar.defaults.type-mappings[0].topic-name", "foo-topic");
			map.put("spring.pulsar.defaults.type-mappings[1].message-type", String.class.getName());
			map.put("spring.pulsar.defaults.type-mappings[1].topic-name", "string-topic");
			PulsarProperties.Defaults properties = bindProperties(map).getDefaults();
			TypeMapping expectedTopic1 = new TypeMapping(TestMessage.class, "foo-topic", null);
			TypeMapping expectedTopic2 = new TypeMapping(String.class, "string-topic", null);
			assertThat(properties.getTypeMappings()).containsExactly(expectedTopic1, expectedTopic2);
		}

		@Test
		void bindWhenTypeMappingsWithSchemaOnly() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.defaults.type-mappings[0].message-type", TestMessage.class.getName());
			map.put("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type", "JSON");
			PulsarProperties.Defaults properties = bindProperties(map).getDefaults();
			TypeMapping expected = new TypeMapping(TestMessage.class, null, new SchemaInfo(SchemaType.JSON, null));
			assertThat(properties.getTypeMappings()).containsExactly(expected);
		}

		@Test
		void bindWhenTypeMappingsWithTopicAndSchema() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.defaults.type-mappings[0].message-type", TestMessage.class.getName());
			map.put("spring.pulsar.defaults.type-mappings[0].topic-name", "foo-topic");
			map.put("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type", "JSON");
			PulsarProperties.Defaults properties = bindProperties(map).getDefaults();
			TypeMapping expected = new TypeMapping(TestMessage.class, "foo-topic",
					new SchemaInfo(SchemaType.JSON, null));
			assertThat(properties.getTypeMappings()).containsExactly(expected);
		}

		@Test
		void bindWhenTypeMappingsWithKeyValueSchema() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.defaults.type-mappings[0].message-type", TestMessage.class.getName());
			map.put("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type", "KEY_VALUE");
			map.put("spring.pulsar.defaults.type-mappings[0].schema-info.message-key-type", String.class.getName());
			PulsarProperties.Defaults properties = bindProperties(map).getDefaults();
			TypeMapping expected = new TypeMapping(TestMessage.class, null,
					new SchemaInfo(SchemaType.KEY_VALUE, String.class));
			assertThat(properties.getTypeMappings()).containsExactly(expected);
		}

		@Test
		void bindWhenNoSchemaThrowsException() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.defaults.type-mappings[0].message-type", TestMessage.class.getName());
			map.put("spring.pulsar.defaults.type-mappings[0].schema-info.message-key-type", String.class.getName());
			assertThatExceptionOfType(BindException.class).isThrownBy(() -> bindProperties(map))
				.havingRootCause()
				.withMessageContaining("schemaType must not be null");
		}

		@Test
		void bindWhenSchemaTypeNoneThrowsException() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.defaults.type-mappings[0].message-type", TestMessage.class.getName());
			map.put("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type", "NONE");
			assertThatExceptionOfType(BindException.class).isThrownBy(() -> bindProperties(map))
				.havingRootCause()
				.withMessageContaining("schemaType 'NONE' not supported");
		}

		@Test
		void bindWhenMessageKeyTypeSetOnNonKeyValueSchemaThrowsException() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.defaults.type-mappings[0].message-type", TestMessage.class.getName());
			map.put("spring.pulsar.defaults.type-mappings[0].schema-info.schema-type", "JSON");
			map.put("spring.pulsar.defaults.type-mappings[0].schema-info.message-key-type", String.class.getName());
			assertThatExceptionOfType(BindException.class).isThrownBy(() -> bindProperties(map))
				.havingRootCause()
				.withMessageContaining("messageKeyType can only be set when schemaType is KEY_VALUE");
		}

		record TestMessage(String value) {
		}

	}

	@Nested
	class FunctionProperties {

		@Test
		void defaults() {
			PulsarProperties.Function properties = new PulsarProperties.Function();
			assertThat(properties.isFailFast()).isTrue();
			assertThat(properties.isPropagateFailures()).isTrue();
			assertThat(properties.isPropagateStopFailures()).isFalse();
		}

		@Test
		void bind() {
			Map<String, String> props = new HashMap<>();
			props.put("spring.pulsar.function.fail-fast", "false");
			props.put("spring.pulsar.function.propagate-failures", "false");
			props.put("spring.pulsar.function.propagate-stop-failures", "true");
			PulsarProperties.Function properties = bindProperties(props).getFunction();
			assertThat(properties.isFailFast()).isFalse();
			assertThat(properties.isPropagateFailures()).isFalse();
			assertThat(properties.isPropagateStopFailures()).isTrue();
		}

	}

	@Nested
	class ProducerProperties {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.producer.name", "my-producer");
			map.put("spring.pulsar.producer.topic-name", "my-topic");
			map.put("spring.pulsar.producer.send-timeout", "2s");
			map.put("spring.pulsar.producer.message-routing-mode", "custompartition");
			map.put("spring.pulsar.producer.hashing-scheme", "murmur3_32hash");
			map.put("spring.pulsar.producer.batching-enabled", "false");
			map.put("spring.pulsar.producer.chunking-enabled", "true");
			map.put("spring.pulsar.producer.compression-type", "lz4");
			map.put("spring.pulsar.producer.access-mode", "exclusive");
			map.put("spring.pulsar.producer.cache.expire-after-access", "2s");
			map.put("spring.pulsar.producer.cache.maximum-size", "3");
			map.put("spring.pulsar.producer.cache.initial-capacity", "5");
			PulsarProperties.Producer properties = bindProperties(map).getProducer();
			assertThat(properties.getName()).isEqualTo("my-producer");
			assertThat(properties.getTopicName()).isEqualTo("my-topic");
			assertThat(properties.getSendTimeout()).isEqualTo(Duration.ofSeconds(2));
			assertThat(properties.getMessageRoutingMode()).isEqualTo(MessageRoutingMode.CustomPartition);
			assertThat(properties.getHashingScheme()).isEqualTo(HashingScheme.Murmur3_32Hash);
			assertThat(properties.isBatchingEnabled()).isFalse();
			assertThat(properties.isChunkingEnabled()).isTrue();
			assertThat(properties.getCompressionType()).isEqualTo(CompressionType.LZ4);
			assertThat(properties.getAccessMode()).isEqualTo(ProducerAccessMode.Exclusive);
			assertThat(properties.getCache().getExpireAfterAccess()).isEqualTo(Duration.ofSeconds(2));
			assertThat(properties.getCache().getMaximumSize()).isEqualTo(3);
			assertThat(properties.getCache().getInitialCapacity()).isEqualTo(5);
		}

	}

	@Nested
	class ConsumerPropertiesTests {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.consumer.name", "my-consumer");
			map.put("spring.pulsar.consumer.subscription.initial-position", "earliest");
			map.put("spring.pulsar.consumer.subscription.mode", "nondurable");
			map.put("spring.pulsar.consumer.subscription.name", "my-subscription");
			map.put("spring.pulsar.consumer.subscription.topics-mode", "all-topics");
			map.put("spring.pulsar.consumer.subscription.type", "shared");
			map.put("spring.pulsar.consumer.topics[0]", "my-topic");
			map.put("spring.pulsar.consumer.topics-pattern", "my-pattern");
			map.put("spring.pulsar.consumer.priority-level", "8");
			map.put("spring.pulsar.consumer.read-compacted", "true");
			map.put("spring.pulsar.consumer.dead-letter-policy.max-redeliver-count", "4");
			map.put("spring.pulsar.consumer.dead-letter-policy.retry-letter-topic", "my-retry-topic");
			map.put("spring.pulsar.consumer.dead-letter-policy.dead-letter-topic", "my-dlt-topic");
			map.put("spring.pulsar.consumer.dead-letter-policy.initial-subscription-name", "my-initial-subscription");
			map.put("spring.pulsar.consumer.retry-enable", "true");
			PulsarProperties.Consumer properties = bindProperties(map).getConsumer();
			assertThat(properties.getName()).isEqualTo("my-consumer");
			assertThat(properties.getSubscription()).satisfies((subscription) -> {
				assertThat(subscription.getName()).isEqualTo("my-subscription");
				assertThat(subscription.getType()).isEqualTo(SubscriptionType.Shared);
				assertThat(subscription.getMode()).isEqualTo(SubscriptionMode.NonDurable);
				assertThat(subscription.getInitialPosition()).isEqualTo(SubscriptionInitialPosition.Earliest);
				assertThat(subscription.getTopicsMode()).isEqualTo(RegexSubscriptionMode.AllTopics);
			});
			assertThat(properties.getTopics()).containsExactly("my-topic");
			assertThat(properties.getTopicsPattern().toString()).isEqualTo("my-pattern");
			assertThat(properties.getPriorityLevel()).isEqualTo(8);
			assertThat(properties.isReadCompacted()).isTrue();
			assertThat(properties.getDeadLetterPolicy()).satisfies((policy) -> {
				assertThat(policy.getMaxRedeliverCount()).isEqualTo(4);
				assertThat(policy.getRetryLetterTopic()).isEqualTo("my-retry-topic");
				assertThat(policy.getDeadLetterTopic()).isEqualTo("my-dlt-topic");
				assertThat(policy.getInitialSubscriptionName()).isEqualTo("my-initial-subscription");
			});
			assertThat(properties.isRetryEnable()).isTrue();
		}

	}

	@Nested
	class ListenerProperties {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.listener.schema-type", "avro");
			map.put("spring.pulsar.listener.observation-enabled", "true");
			PulsarProperties.Listener properties = bindProperties(map).getListener();
			assertThat(properties.getSchemaType()).isEqualTo(SchemaType.AVRO);
			assertThat(properties.isObservationEnabled()).isTrue();
		}

	}

	@Nested
	class ReaderProperties {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.reader.name", "my-reader");
			map.put("spring.pulsar.reader.topics", "my-topic");
			map.put("spring.pulsar.reader.subscription-name", "my-subscription");
			map.put("spring.pulsar.reader.subscription-role-prefix", "sub-role");
			map.put("spring.pulsar.reader.read-compacted", "true");
			PulsarProperties.Reader properties = bindProperties(map).getReader();
			assertThat(properties.getName()).isEqualTo("my-reader");
			assertThat(properties.getTopics()).containsExactly("my-topic");
			assertThat(properties.getSubscriptionName()).isEqualTo("my-subscription");
			assertThat(properties.getSubscriptionRolePrefix()).isEqualTo("sub-role");
			assertThat(properties.isReadCompacted()).isTrue();
		}

	}

	@Nested
	class TemplateProperties {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.template.observations-enabled", "true");
			PulsarProperties.Template properties = bindProperties(map).getTemplate();
			assertThat(properties.isObservationsEnabled()).isTrue();
		}

	}

	@Nested
	class TransactionProperties {

		@Test
		void bind() {
			Map<String, String> map = new HashMap<>();
			map.put("spring.pulsar.transaction.enabled", "true");
			PulsarProperties.Transaction properties = bindProperties(map).getTransaction();
			assertThat(properties.isEnabled()).isTrue();
		}

	}

}
