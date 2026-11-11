/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.integration;

import org.springframework.stereotype.Service;

@Service
public class HelloWorldService {

	private final ServiceProperties configuration;

	public HelloWorldService(ServiceProperties configuration) {
		this.configuration = configuration;
	}

	public String getHelloMessage(String name) {
		return this.configuration.getGreeting() + " " + name;
	}

}
