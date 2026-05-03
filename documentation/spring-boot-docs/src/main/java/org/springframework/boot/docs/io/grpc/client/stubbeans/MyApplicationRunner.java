/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
