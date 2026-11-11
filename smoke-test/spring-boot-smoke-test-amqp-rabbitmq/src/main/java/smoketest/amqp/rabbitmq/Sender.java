/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.amqp.rabbitmq;

import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;

public class Sender {

	@Autowired
	private RabbitAmqpTemplate rabbitAmqpTemplate;

	public void send(String payload) {
		this.rabbitAmqpTemplate.convertAndSend("foo", payload, (message) -> {
			message.getMessageProperties().setHeader("source-app", "smoke-test");
			return message;
		});
	}

}
