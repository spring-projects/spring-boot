/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.websocket.jetty.client;

public class SimpleGreetingService implements GreetingService {

	@Override
	public String getGreeting() {
		return "Hello world!";
	}

}
