/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.pulsar.reading;

import org.springframework.pulsar.annotation.PulsarReader;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	@PulsarReader(topics = "someTopic", startMessageId = "earliest")
	public void processMessage(String content) {
		// ...
	}

}
