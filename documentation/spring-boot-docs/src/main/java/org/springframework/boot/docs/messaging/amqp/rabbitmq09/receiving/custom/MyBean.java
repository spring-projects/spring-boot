/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.amqp.rabbitmq09.receiving.custom;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	@RabbitListener(queues = "someQueue", containerFactory = "myFactory")
	public void processMessage(String content) {
		// ...
	}

}
