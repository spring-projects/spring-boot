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

package org.springframework.boot.autoconfigure.pulsar;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.ProducerAccessMode;
import org.apache.pulsar.client.api.RegexSubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.common.schema.SchemaType;
import org.apache.pulsar.reactive.client.api.ReactiveMessageConsumerBuilder;
import org.apache.pulsar.reactive.client.api.ReactiveMessageReaderBuilder;
import org.apache.pulsar.reactive.client.api.ReactiveMessageSenderBuilder;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Consumer;
import org.springframework.boot.autoconfigure.pulsar.PulsarProperties.Consumer.Subscription;
import org.springframework.pulsar.reactive.listener.ReactivePulsarContainerProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link PulsarReactivePropertiesMapper}.
 *
 * @author Chris Bono
 * @author Phillip Webb
 */
class PulsarReactivePropertiesMapperTests {

	@Test
	@SuppressWarnings("unchecked")
	void customizeMessageSenderBuilder() {
		PulsarProperties properties = new PulsarProperties();
		properties.getProducer().setName("name");
		properties.getProducer().setTopicName("topicname");
		properties.getProducer().setSendTimeout(Duration.ofSeconds(1));
		properties.getProducer().setMessageRoutingMode(MessageRoutingMode.RoundRobinPartition);
		properties.getProducer().setHashingScheme(HashingScheme.JavaStringHash);
		properties.getProducer().setBatchingEnabled(false);
		properties.getProducer().setChunkingEnabled(true);
		properties.getProducer().setCompressionType(CompressionType.SNAPPY);
		properties.getProducer().setAccessMode(ProducerAccessMode.Exclusive);
		ReactiveMessageSenderBuilder<Object> builder = mock(ReactiveMessageSenderBuilder.class);
		new PulsarReactivePropertiesMapper(properties).customizeMessageSenderBuilder(builder);
		then(builder).should().producerName("name");
		then(builder).should().topic("topicname");
		then(builder).should().sendTimeout(Duration.ofSeconds(1));
		then(builder).should().messageRoutingMode(MessageRoutingMode.RoundRobinPartition);
		then(builder).should().hashingScheme(HashingScheme.JavaStringHash);
		then(builder).should().batchingEnabled(false);
		then(builder).should().chunkingEnabled(true);
		then(builder).should().compressionType(CompressionType.SNAPPY);
		then(builder).should().accessMode(ProducerAccessMode.Exclusive);
	}

	@Test
	@SuppressWarnings("unchecked")
	void customizeMessageConsumerBuilder() {
		PulsarProperties properties = new PulsarProperties();
		List<String> topics = List.of("mytopic");
		Pattern topisPattern = Pattern.compile("my-pattern");
		properties.getConsumer().setName("name");
		properties.getConsumer().setTopics(topics);
		properties.getConsumer().setTopicsPattern(topisPattern);
		properties.getConsumer().setPriorityLevel(123);
		properties.getConsumer().setReadCompacted(true);
		Consumer.DeadLetterPolicy deadLetterPolicy = new Consumer.DeadLetterPolicy();
		deadLetterPolicy.setDeadLetterTopic("my-dlt");
		deadLetterPolicy.setMaxRedeliverCount(1);
		properties.getConsumer().setDeadLetterPolicy(deadLetterPolicy);
		properties.getConsumer().setRetryEnable(false);
		Subscription subscriptionProperties = properties.getConsumer().getSubscription();
		subscriptionProperties.setName("subname");
		subscriptionProperties.setInitialPosition(SubscriptionInitialPosition.Earliest);
		subscriptionProperties.setMode(SubscriptionMode.NonDurable);
		subscriptionProperties.setTopicsMode(RegexSubscriptionMode.NonPersistentOnly);
		subscriptionProperties.setType(SubscriptionType.Key_Shared);
		ReactiveMessageConsumerBuilder<Object> builder = mock(ReactiveMessageConsumerBuilder.class);
		new PulsarReactivePropertiesMapper(properties).customizeMessageConsumerBuilder(builder);
		then(builder).should().consumerName("name");
		then(builder).should().topics(topics);
		then(builder).should().topicsPattern(topisPattern);
		then(builder).should().priorityLevel(123);
		then(builder).should().readCompacted(true);
		then(builder).should().deadLetterPolicy(new DeadLetterPolicy(1, null, "my-dlt", null));
		then(builder).should().retryLetterTopicEnable(false);
		then(builder).should().subscriptionName("subname");
		then(builder).should().subscriptionInitialPosition(SubscriptionInitialPosition.Earliest);
		then(builder).should().subscriptionMode(SubscriptionMode.NonDurable);
		then(builder).should().topicsPatternSubscriptionMode(RegexSubscriptionMode.NonPersistentOnly);
		then(builder).should().subscriptionType(SubscriptionType.Key_Shared);
	}

	@Test
	void customizeContainerProperties() {
		PulsarProperties properties = new PulsarProperties();
		properties.getConsumer().getSubscription().setType(SubscriptionType.Shared);
		properties.getListener().setSchemaType(SchemaType.AVRO);
		ReactivePulsarContainerProperties<Object> containerProperties = new ReactivePulsarContainerProperties<>();
		new PulsarReactivePropertiesMapper(properties).customizeContainerProperties(containerProperties);
		assertThat(containerProperties.getSubscriptionType()).isEqualTo(SubscriptionType.Shared);
		assertThat(containerProperties.getSchemaType()).isEqualTo(SchemaType.AVRO);
	}

	@Test
	@SuppressWarnings("unchecked")
	void customizeMessageReaderBuilder() {
		List<String> topics = List.of("my-topic");
		PulsarProperties properties = new PulsarProperties();
		properties.getReader().setName("name");
		properties.getReader().setTopics(topics);
		properties.getReader().setSubscriptionName("subname");
		properties.getReader().setSubscriptionRolePrefix("srp");
		ReactiveMessageReaderBuilder<Object> builder = mock(ReactiveMessageReaderBuilder.class);
		new PulsarReactivePropertiesMapper(properties).customizeMessageReaderBuilder(builder);
		then(builder).should().readerName("name");
		then(builder).should().topics(topics);
		then(builder).should().subscriptionName("subname");
		then(builder).should().generatedSubscriptionNamePrefix("srp");
	}

}
