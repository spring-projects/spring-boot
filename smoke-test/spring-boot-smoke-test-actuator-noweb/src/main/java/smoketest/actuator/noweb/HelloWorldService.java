/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.noweb;

import org.springframework.stereotype.Component;

@Component
public class HelloWorldService {

	private final ServiceProperties configuration;

	public HelloWorldService(ServiceProperties configuration) {
		this.configuration = configuration;
	}

	public String getHelloMessage() {
		return "Hello " + this.configuration.getName();
	}

}
