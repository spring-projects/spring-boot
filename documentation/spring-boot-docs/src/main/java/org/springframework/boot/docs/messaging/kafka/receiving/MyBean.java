/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.kafka.receiving;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	@KafkaListener(topics = "someTopic")
	public void processMessage(String content) {
		// ...
	}

}
