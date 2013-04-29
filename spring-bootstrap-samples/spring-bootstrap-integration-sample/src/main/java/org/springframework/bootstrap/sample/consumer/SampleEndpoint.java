package org.springframework.bootstrap.sample.consumer;

import java.io.File;
import java.io.FileInputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.util.StreamUtils;

@MessageEndpoint
public class SampleEndpoint {

	@Autowired
	private HelloWorldService helloWorldService;

	@ServiceActivator
	public String hello(File input) throws Exception {
		FileInputStream in = new FileInputStream(input);
		String name = new String(StreamUtils.copyToByteArray(in));
		in.close();
		return this.helloWorldService.getHelloMessage(name);
	}

}
