/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.amqp.generic.receiving.custom;

import org.springframework.amqp.client.annotation.AmqpListener;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	@AmqpListener(addresses = "/queues/someQueue", containerFactory = "myFactory")
	public void processMessage(String content) {
		// ...
	}

}
