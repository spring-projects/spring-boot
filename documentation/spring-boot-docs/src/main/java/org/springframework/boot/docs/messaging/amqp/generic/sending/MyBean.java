/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.amqp.generic.sending;

import org.springframework.amqp.client.AmqpClient;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	private final AmqpClient amqpClient;

	public MyBean(AmqpClient amqpClient) {
		this.amqpClient = amqpClient;
	}

	// @fold:on // ...
	public void sendMessage(String msg) {
		this.amqpClient.to("/queues/test").body(msg).send();
	}
	// @fold:off

}
