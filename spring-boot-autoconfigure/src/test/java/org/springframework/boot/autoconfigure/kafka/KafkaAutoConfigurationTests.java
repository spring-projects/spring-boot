/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.kafka;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.junit.After;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AbstractMessageListenerContainer.AckMode;
import org.springframework.kafka.security.jaas.KafkaJaasLoginModuleInitializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link KafkaAutoConfiguration}.
 *
 * @author Gary Russell
 * @author Stephane Nicoll
 */
public class KafkaAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void consumerProperties() {
		load("spring.kafka.bootstrap-servers=foo:1234", "spring.kafka.properties.foo=bar",
				"spring.kafka.properties.baz=qux",
				"spring.kafka.properties.foo.bar.baz=qux.fiz.buz",
				"spring.kafka.ssl.key-password=p1",
				"spring.kafka.ssl.keystore-location=classpath:ksLoc",
				"spring.kafka.ssl.keystore-password=p2",
				"spring.kafka.ssl.truststore-location=classpath:tsLoc",
				"spring.kafka.ssl.truststore-password=p3",
				"spring.kafka.consumer.auto-commit-interval=123",
				"spring.kafka.consumer.max-poll-records=42",
				"spring.kafka.consumer.auto-offset-reset=earliest",
				"spring.kafka.consumer.client-id=ccid", // test override common
				"spring.kafka.consumer.enable-auto-commit=false",
				"spring.kafka.consumer.fetch-max-wait=456",
				"spring.kafka.consumer.properties.fiz.buz=fix.fox",
				"spring.kafka.consumer.fetch-min-size=789",
				"spring.kafka.consumer.group-id=bar",
				"spring.kafka.consumer.heartbeat-interval=234",
				"spring.kafka.consumer.key-deserializer = org.apache.kafka.common.serialization.LongDeserializer",
				"spring.kafka.consumer.value-deserializer = org.apache.kafka.common.serialization.IntegerDeserializer");
		DefaultKafkaConsumerFactory<?, ?> consumerFactory = this.context
				.getBean(DefaultKafkaConsumerFactory.class);
		Map<String, Object> configs = consumerFactory.getConfigurationProperties();
		// common
		assertThat(configs.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG))
				.isEqualTo(Collections.singletonList("foo:1234"));
		assertThat(configs.get(SslConfigs.SSL_KEY_PASSWORD_CONFIG)).isEqualTo("p1");
		assertThat((String) configs.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG))
				.endsWith(File.separator + "ksLoc");
		assertThat(configs.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG)).isEqualTo("p2");
		assertThat((String) configs.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG))
				.endsWith(File.separator + "tsLoc");
		assertThat(configs.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG))
				.isEqualTo("p3");
		// consumer
		assertThat(configs.get(ConsumerConfig.CLIENT_ID_CONFIG)).isEqualTo("ccid"); // override
		assertThat(configs.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG))
				.isEqualTo(Boolean.FALSE);
		assertThat(configs.get(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG))
				.isEqualTo(123);
		assertThat(configs.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG))
				.isEqualTo("earliest");
		assertThat(configs.get(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG)).isEqualTo(456);
		assertThat(configs.get(ConsumerConfig.FETCH_MIN_BYTES_CONFIG)).isEqualTo(789);
		assertThat(configs.get(ConsumerConfig.GROUP_ID_CONFIG)).isEqualTo("bar");
		assertThat(configs.get(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG))
				.isEqualTo(234);
		assertThat(configs.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG))
				.isEqualTo(LongDeserializer.class);
		assertThat(configs.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG))
				.isEqualTo(IntegerDeserializer.class);
		assertThat(configs.get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG)).isEqualTo(42);
		assertThat(configs.get("foo")).isEqualTo("bar");
		assertThat(configs.get("baz")).isEqualTo("qux");
		assertThat(configs.get("foo.bar.baz")).isEqualTo("qux.fiz.buz");
		assertThat(configs.get("fiz.buz")).isEqualTo("fix.fox");
	}

	@Test
	public void producerProperties() {
		load("spring.kafka.clientId=cid",
				"spring.kafka.properties.foo.bar.baz=qux.fiz.buz",
				"spring.kafka.producer.acks=all", "spring.kafka.producer.batch-size=20",
				"spring.kafka.producer.bootstrap-servers=bar:1234", // test override
				"spring.kafka.producer.buffer-memory=12345",
				"spring.kafka.producer.compression-type=gzip",
				"spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.LongSerializer",
				"spring.kafka.producer.retries=2",
				"spring.kafka.producer.properties.fiz.buz=fix.fox",
				"spring.kafka.producer.ssl.key-password=p4",
				"spring.kafka.producer.ssl.keystore-location=classpath:ksLocP",
				"spring.kafka.producer.ssl.keystore-password=p5",
				"spring.kafka.producer.ssl.truststore-location=classpath:tsLocP",
				"spring.kafka.producer.ssl.truststore-password=p6",
				"spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.IntegerSerializer");
		DefaultKafkaProducerFactory<?, ?> producerFactory = this.context
				.getBean(DefaultKafkaProducerFactory.class);
		Map<String, Object> configs = producerFactory.getConfigurationProperties();
		// common
		assertThat(configs.get(ProducerConfig.CLIENT_ID_CONFIG)).isEqualTo("cid");
		// producer
		assertThat(configs.get(ProducerConfig.ACKS_CONFIG)).isEqualTo("all");
		assertThat(configs.get(ProducerConfig.BATCH_SIZE_CONFIG)).isEqualTo(20);
		assertThat(configs.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))
				.isEqualTo(Collections.singletonList("bar:1234")); // override
		assertThat(configs.get(ProducerConfig.BUFFER_MEMORY_CONFIG)).isEqualTo(12345L);
		assertThat(configs.get(ProducerConfig.COMPRESSION_TYPE_CONFIG)).isEqualTo("gzip");
		assertThat(configs.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG))
				.isEqualTo(LongSerializer.class);
		assertThat(configs.get(SslConfigs.SSL_KEY_PASSWORD_CONFIG)).isEqualTo("p4");
		assertThat((String) configs.get(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG))
				.endsWith(File.separator + "ksLocP");
		assertThat(configs.get(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG)).isEqualTo("p5");
		assertThat((String) configs.get(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG))
				.endsWith(File.separator + "tsLocP");
		assertThat(configs.get(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG))
				.isEqualTo("p6");
		assertThat(configs.get(ProducerConfig.RETRIES_CONFIG)).isEqualTo(2);
		assertThat(configs.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG))
				.isEqualTo(IntegerSerializer.class);
		assertThat(this.context.getBeansOfType(KafkaJaasLoginModuleInitializer.class))
				.isEmpty();
		assertThat(configs.get("foo.bar.baz")).isEqualTo("qux.fiz.buz");
		assertThat(configs.get("fiz.buz")).isEqualTo("fix.fox");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void listenerProperties() {
		load("spring.kafka.template.default-topic=testTopic",
				"spring.kafka.listener.ack-mode=MANUAL",
				"spring.kafka.listener.ack-count=123",
				"spring.kafka.listener.ack-time=456",
				"spring.kafka.listener.concurrency=3",
				"spring.kafka.listener.poll-timeout=2000",
				"spring.kafka.listener.type=batch", "spring.kafka.jaas.enabled=true",
				"spring.kafka.jaas.login-module=foo",
				"spring.kafka.jaas.control-flag=REQUISITE",
				"spring.kafka.jaas.options.useKeyTab=true");
		DefaultKafkaProducerFactory<?, ?> producerFactory = this.context
				.getBean(DefaultKafkaProducerFactory.class);
		DefaultKafkaConsumerFactory<?, ?> consumerFactory = this.context
				.getBean(DefaultKafkaConsumerFactory.class);
		KafkaTemplate<?, ?> kafkaTemplate = this.context.getBean(KafkaTemplate.class);
		KafkaListenerContainerFactory<?> kafkaListenerContainerFactory = this.context
				.getBean(KafkaListenerContainerFactory.class);
		assertThat(new DirectFieldAccessor(kafkaTemplate)
				.getPropertyValue("producerFactory")).isEqualTo(producerFactory);
		assertThat(kafkaTemplate.getDefaultTopic()).isEqualTo("testTopic");
		DirectFieldAccessor dfa = new DirectFieldAccessor(kafkaListenerContainerFactory);
		assertThat(dfa.getPropertyValue("consumerFactory")).isEqualTo(consumerFactory);
		assertThat(dfa.getPropertyValue("containerProperties.ackMode"))
				.isEqualTo(AckMode.MANUAL);
		assertThat(dfa.getPropertyValue("containerProperties.ackCount")).isEqualTo(123);
		assertThat(dfa.getPropertyValue("containerProperties.ackTime")).isEqualTo(456L);
		assertThat(dfa.getPropertyValue("concurrency")).isEqualTo(3);
		assertThat(dfa.getPropertyValue("containerProperties.pollTimeout"))
				.isEqualTo(2000L);
		assertThat(dfa.getPropertyValue("batchListener")).isEqualTo(true);
		assertThat(this.context.getBeansOfType(KafkaJaasLoginModuleInitializer.class))
				.hasSize(1);
		KafkaJaasLoginModuleInitializer jaas = this.context
				.getBean(KafkaJaasLoginModuleInitializer.class);
		dfa = new DirectFieldAccessor(jaas);
		assertThat(dfa.getPropertyValue("loginModule")).isEqualTo("foo");
		assertThat(dfa.getPropertyValue("controlFlag"))
				.isEqualTo(AppConfigurationEntry.LoginModuleControlFlag.REQUISITE);
		assertThat(((Map<String, String>) dfa.getPropertyValue("options")))
				.containsExactly(entry("useKeyTab", "true"));
	}

	private void load(String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(KafkaAutoConfiguration.class);
		TestPropertyValues.of(environment).applyTo(ctx);
		ctx.refresh();
		this.context = ctx;
	}

}
