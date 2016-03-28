/*
 * Copyright 2012-2016 the original author or authors.
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

import java.util.Arrays;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.IntegrationTest;
import org.springframework.boot.test.context.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Gary Russell
 * @since 1.4
 */

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(KafkaPropertiesTests.Config.class)
@IntegrationTest({
	"spring.kafka.bootstrap-servers=foo:1234",
	"spring.kafka.consumer.group-id=bar",
	"spring.kafka.consumer.enable-auto-commit=true",
	"spring.kafka.producer.bootstrap-servers=bar:1234",
	"spring.kafka.producer.batch-size=20"
	})
public class KafkaPropertiesTests {

	@Autowired
	private KafkaProperties props;

	@Autowired
	private ConsumerFactory<?, ?> consumerFactory;

	@Autowired
	private ProducerFactory<?, ?> producerFactory;

	@Test
	public void testConsumerProps() {
		Map<String, Object> consumerProps = this.props.buildConsumerProperties();
		assertThat(consumerProps.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG))
			.isEqualTo(Arrays.asList(new String[] { "foo:1234" }));
		assertThat(consumerProps.get(ConsumerConfig.GROUP_ID_CONFIG)).isEqualTo("bar");
		assertThat(consumerProps.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG)).isEqualTo(Boolean.TRUE);
		// ...
	}

	@Test
	public void testProducerProps() {
		Map<String, Object> producerProps = this.props.buildProducerProperties();
		assertThat(producerProps.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))
				.isEqualTo(Arrays.asList(new String[] { "bar:1234" }));
		assertThat(producerProps.get(ProducerConfig.BATCH_SIZE_CONFIG)).isEqualTo(20);
		// ...

	}

	@Test
	public void testPropsInjected() {
		Map<String, Object> consumerProps = this.props.buildConsumerProperties();
		Map<String, Object> producerProps = this.props.buildProducerProperties();
		assertThat(new DirectFieldAccessor(this.consumerFactory).getPropertyValue("configs")).isEqualTo(consumerProps);
		assertThat(new DirectFieldAccessor(this.producerFactory).getPropertyValue("configs")).isEqualTo(producerProps);
	}

	@Configuration
	@Import(KafkaAutoConfiguration.class)
	public static class Config {

	}

}
