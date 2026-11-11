/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.jta.mixingxaandnonxaconnections.primary;

import jakarta.jms.ConnectionFactory;

public class MyBean {

	public MyBean(ConnectionFactory connectionFactory) {
		// ...
	}

}
