/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.amqp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.client.annotation.AmqpListener;
import org.springframework.stereotype.Component;

@Component
class Listener {

	private static final Log logger = LogFactory.getLog(Listener.class);

	@AmqpListener(addresses = "/queues/foo")
	void process(String foo) {
		logger.info(foo);
	}

}
