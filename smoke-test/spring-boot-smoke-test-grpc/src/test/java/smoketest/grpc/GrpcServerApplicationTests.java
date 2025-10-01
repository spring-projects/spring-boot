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

package smoketest.grpc;

import io.grpc.ManagedChannel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import smoketest.grpc.proto.HelloReply;
import smoketest.grpc.proto.HelloRequest;
import smoketest.grpc.proto.SimpleGrpc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(useMainMethod = UseMainMethod.ALWAYS)
@DirtiesContext
class GrpcServerApplicationTests {

	static void main(String[] args) {
		new SpringApplicationBuilder(GrpcServerApplication.class).run();
	}

	private void assertThatResponseIsServedToChannel(ManagedChannel clientChannel) {
		SimpleGrpc.SimpleBlockingStub client = SimpleGrpc.newBlockingStub(clientChannel);
		HelloReply response = client.sayHello(HelloRequest.newBuilder().setName("Alien").build());
		assertThat(response.getMessage()).isEqualTo("Hello ==> Alien");
	}

	@Nested
	@SpringBootTest(
			properties = { "spring.grpc.server.port=0",
					"spring.grpc.client.default-channel.address=0.0.0.0:${local.grpc.port}" },
			useMainMethod = UseMainMethod.ALWAYS)
	@DirtiesContext
	class ServerUnsecured {

		@Test
		void clientChannelWithoutSsl(@Autowired GrpcChannelFactory channels) {
			assertThatResponseIsServedToChannel(channels.createChannel("test-channel"));
		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.port=0",
			"spring.grpc.client.channels.test-channel.address=static://0.0.0.0:${local.grpc.port}",
			"spring.grpc.client.channels.test-channel.negotiation-type=TLS",
			"spring.grpc.client.channels.test-channel.secure=false" })
	@ActiveProfiles("ssl")
	@DirtiesContext
	class ServerWithSsl {

		@Test
		void clientChannelWithSsl(@Autowired GrpcChannelFactory channels) {
			assertThatResponseIsServedToChannel(channels.createChannel("test-channel"));
		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.port=0", "spring.grpc.server.ssl.client-auth=REQUIRE",
			"spring.grpc.server.ssl.secure=false",
			"spring.grpc.client.channels.test-channel.address=static://0.0.0.0:${local.grpc.port}",
			"spring.grpc.client.channels.test-channel.ssl.bundle=ssltest",
			"spring.grpc.client.channels.test-channel.negotiation-type=TLS",
			"spring.grpc.client.channels.test-channel.secure=false" })
	@ActiveProfiles("ssl")
	@DirtiesContext
	class ServerWithClientAuth {

		@Test
		void clientChannelWithSsl(@Autowired GrpcChannelFactory channels) {
			assertThatResponseIsServedToChannel(channels.createChannel("test-channel"));
		}

	}

}
