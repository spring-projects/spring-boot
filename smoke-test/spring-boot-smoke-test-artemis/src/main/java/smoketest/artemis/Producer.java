/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.artemis;

import jakarta.jms.Queue;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jms.core.JmsClient;
import org.springframework.stereotype.Component;

@Component
public class Producer implements CommandLineRunner {

	@Autowired
	private JmsClient jmsClient;

	@Autowired
	private Queue queue;

	@Override
	public void run(String... args) throws Exception {
		send("Sample message");
		System.out.println("Message was sent to the Queue");
	}

	public void send(String msg) {
		this.jmsClient.destination(this.queue).send(msg);
	}

}
