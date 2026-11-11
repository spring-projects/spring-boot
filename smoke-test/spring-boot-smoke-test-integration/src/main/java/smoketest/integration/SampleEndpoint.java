/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.integration;

import java.io.File;
import java.io.FileInputStream;

import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.util.StreamUtils;

@MessageEndpoint
public class SampleEndpoint {

	private final HelloWorldService helloWorldService;

	public SampleEndpoint(HelloWorldService helloWorldService) {
		this.helloWorldService = helloWorldService;
	}

	@ServiceActivator
	public String hello(File input) throws Exception {
		FileInputStream in = new FileInputStream(input);
		String name = new String(StreamUtils.copyToByteArray(in));
		in.close();
		return this.helloWorldService.getHelloMessage(name);
	}

}
