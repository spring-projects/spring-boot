/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport.container;

import java.util.UUID;

import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * A {@link RabbitMQContainer} with management plugin enabled.
 *
 * @author Stephane Nicoll
 */
public class RabbitMqManagementContainer extends RabbitMQContainer {

	public RabbitMqManagementContainer() {
		super(TestImage.RABBITMQ.toString());
	}

	/**
	 * Create a queue with the given name.
	 * @param name the name of the queue
	 * @return the AMQP target of the queue
	 */
	public String createQueue(String name) {
		try {
			execInContainer("rabbitmqadmin", "queues", "declare", "--name", name);
			return "/queues/" + name;
		}
		catch (Exception ex) {
			throw new RuntimeException("Could not create queue", ex);
		}
	}

	/**
	 * Create a queue with a random, unique, name.
	 * @return the AMQP target of the queue
	 */
	public String createRandomQueue() {
		return createQueue(UUID.randomUUID().toString());
	}

}
