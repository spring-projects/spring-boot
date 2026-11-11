/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.jms.receiving.custom;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	@JmsListener(destination = "someQueue", containerFactory = "myFactory")
	public void processMessage(String content) {
		// ...
	}

}
