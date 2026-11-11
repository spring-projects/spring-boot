/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.kafka.sending;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	private final KafkaTemplate<String, String> kafkaTemplate;

	public MyBean(KafkaTemplate<String, String> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	// @fold:on // ...
	public void someMethod() {
		this.kafkaTemplate.send("someTopic", "Hello");
	}
	// @fold:off

}
