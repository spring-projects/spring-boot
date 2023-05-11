/*
 * Copyright 2023-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import org.apache.pulsar.client.api.MessageId;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.core.PulsarTopic;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class SamplePulsarApplication {

	public static void main(String[] args) {
		SpringApplication.run(SamplePulsarApplication.class, args);
	}

	private static final String TOPIC = "pulsar-spa-topic";

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

	@Component
	static class SampleMessageConsumer {

		private List<SampleMessage> consumed = new ArrayList<>();

		List<SampleMessage> getConsumed() {
			return this.consumed;
		}

		@PulsarListener(topics = TOPIC)
		void consumeMessagesFromPulsarTopic(SampleMessage msg) {
			System.out.println("**** CONSUME: " + msg);
			this.consumed.add(msg);
		}

	}

	record SampleMessage(Integer id, String content) {
	}

}
