/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.amqp;

import org.springframework.amqp.client.AmqpClient;
import org.springframework.stereotype.Component;

@Component
class Sender {

	private final AmqpClient amqpClient;

	Sender(AmqpClient amqpClient) {
		this.amqpClient = amqpClient;
	}

	void send(String message) {
		this.amqpClient.to("/queues/foo").body(message).send();
	}

}
