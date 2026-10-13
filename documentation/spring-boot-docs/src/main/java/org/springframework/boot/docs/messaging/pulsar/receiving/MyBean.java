/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.pulsar.receiving;

import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	@PulsarListener(topics = "someTopic")
	public void processMessage(String content) {
		// ...
	}

}
