/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.grpc.client.stubbeans;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.docs.io.grpc.client.stubbeans.HelloWorldGrpc.HelloWorldBlockingStub;
import org.springframework.stereotype.Component;

@Component
class MyApplicationRunner implements ApplicationRunner {

	private final HelloWorldBlockingStub helloStub;

	MyApplicationRunner(HelloWorldGrpc.HelloWorldBlockingStub helloStub) {
		this.helloStub = helloStub;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		HelloRequest request = HelloRequest.newBuilder().setName("Spring").build();
		HelloReply reply = this.helloStub.sayHello(request);
		System.out.println(reply.getMessage());
	}

}
