/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.jms.sending;

import org.springframework.jms.core.JmsClient;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	private final JmsClient jmsClient;

	public MyBean(JmsClient jmsClient) {
		this.jmsClient = jmsClient;
	}

	// @fold:on // ...
	public void someMethod() {
		this.jmsClient.destination("myQueue").send("hello");
	}
	// @fold:off

}
