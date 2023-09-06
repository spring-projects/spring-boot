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

package smoketest.pulsar;

import org.apache.pulsar.client.api.MessageId;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.core.PulsarTopic;

@SpringBootApplication
public class SamplePulsarApplication {

	static final String TOPIC = "pulsar-smoke-test-topic";

	@Bean
	PulsarTopic pulsarTestTopic() {
		return PulsarTopic.builder(TOPIC).numberOfPartitions(1).build();
	}

	@Bean
	ApplicationRunner sendMessagesToPulsarTopic(PulsarTemplate<SampleMessage> template) {
		return (args) -> {
			for (int i = 0; i < 10; i++) {
				MessageId msgId = template.send(TOPIC, new SampleMessage(i, "message:" + i));
				System.out.println("*** PRODUCE: " + msgId);
			}
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(SamplePulsarApplication.class, args);
	}

}
