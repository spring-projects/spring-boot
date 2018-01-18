/*
 * Copyright 2012-2018 the original author or authors.
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
package sample.kafka;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for demo application.
 *
 * @author hcxin
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SampleKafkaApplicationTests {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Autowired
	private Producer producer;

	@Test
	public void sendSimpleMessage() throws Exception {
		initKafkaEmbedded();
		SampleMessage message = new SampleMessage(1, "Test message");
		producer.send(message);
		Thread.sleep(1000L);
		assertThat(this.outputCapture.toString().contains("Test message")).isTrue();
	}

	public void initKafkaEmbedded() throws Exception {
		KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true);
		embeddedKafka.setKafkaPorts(9092);
		embeddedKafka.afterPropertiesSet();
		//Need 10s, waiting for the Kafka server start.
		Thread.sleep(10000L);

	}
}
