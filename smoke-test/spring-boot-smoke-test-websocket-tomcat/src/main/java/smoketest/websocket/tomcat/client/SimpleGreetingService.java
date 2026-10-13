/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.websocket.tomcat.client;

public class SimpleGreetingService implements GreetingService {

	@Override
	public String getGreeting() {
		return "Hello world!";
	}

}
