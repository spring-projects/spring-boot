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

package org.springframework.boot.docs.io.grpc.testing.localserverport

import io.grpc.ManagedChannel
import io.grpc.netty.NettyChannelBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.grpc.test.autoconfigure.LocalGrpcServerPort
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["spring.grpc.server.port=0"])
class MyGrpcIntegrationTests {

	@LocalGrpcServerPort
	var port = 0

	@Test
	fun sayHello() {
		val target = "localhost:${port}"
		val channel: ManagedChannel = NettyChannelBuilder.forTarget(target).usePlaintext().build()
		try {
			val hello: HelloWorldGrpc.HelloWorldBlockingStub = HelloWorldGrpc.newBlockingStub(channel)
			val request = HelloRequest.newBuilder().setName("Spring").build()
			assertThat(hello.sayHello(request).getMessage()).isEqualTo("Hello 'Spring'")
		} finally {
			channel.shutdown()
		}
	}
}
