/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class Producer {

	private final KafkaTemplate<Object, SampleMessage> kafkaTemplate;

	Producer(KafkaTemplate<Object, SampleMessage> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	public void send(SampleMessage message) {
		this.kafkaTemplate.send("testTopic", message);
		System.out.println("Sent sample message [" + message + "]");
	}

}
