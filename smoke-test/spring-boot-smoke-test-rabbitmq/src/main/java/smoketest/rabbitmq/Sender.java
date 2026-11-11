/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

public class Sender {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	public void send(String message) {
		this.rabbitTemplate.convertAndSend("foo", message);
	}

}
