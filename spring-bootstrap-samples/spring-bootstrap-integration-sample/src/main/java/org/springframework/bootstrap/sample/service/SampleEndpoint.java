package org.springframework.bootstrap.sample.service;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;

@MessageEndpoint
public class SampleEndpoint {

	@Autowired
	private HelloWorldService helloWorldService;

	@ServiceActivator
	public Map<String, String> hello(String input) {
		return Collections.singletonMap("message",
				this.helloWorldService.getHelloMessage(input));
	}

}
