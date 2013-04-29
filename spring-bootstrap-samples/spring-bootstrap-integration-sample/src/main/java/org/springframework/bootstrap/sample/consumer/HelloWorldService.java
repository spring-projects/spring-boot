package org.springframework.bootstrap.sample.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HelloWorldService {

	@Autowired
	private ServiceProperties configuration;

	public String getHelloMessage(String name) {
		return this.configuration.getGreeting() + " " + name;
	}

}
