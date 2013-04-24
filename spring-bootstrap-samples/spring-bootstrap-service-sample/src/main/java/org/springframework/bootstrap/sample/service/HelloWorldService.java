package org.springframework.bootstrap.sample.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HelloWorldService {

	@Autowired
	private ServiceProperties configuration;

	public String getHelloMessage() {
		return "Hello " + configuration.getName();
	}

}
