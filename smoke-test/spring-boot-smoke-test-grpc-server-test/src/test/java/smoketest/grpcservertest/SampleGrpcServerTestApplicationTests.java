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

package smoketest.grpcservertest;

import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import smoketest.grpcservertest.proto.HelloReply;
import smoketest.grpcservertest.proto.HelloRequest;
import smoketest.grpcservertest.proto.HelloWorldGrpc.HelloWorldBlockingStub;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.client.ImportGrpcClients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@AutoConfigureTestGrpcTransport
@ImportGrpcClients(types = HelloWorldBlockingStub.class)
class SampleGrpcServerTestApplicationTests {

	@Autowired
	private HelloWorldBlockingStub hello;

	@Test
	void sayHello() {
		HelloRequest request = HelloRequest.newBuilder().setName("Spring").build();
		HelloReply reply = this.hello.sayHello(request);
		assertThat(reply.getMessage()).isEqualTo("Hello 'Spring'");
	}

	@Test
	void sayHelloWhenBadName() {
		HelloRequest request = HelloRequest.newBuilder().setName("errorThing").build();
		assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> this.hello.sayHello(request))
			.satisfies((ex) -> assertThat(ex.getStatus().getCode()).isEqualTo(Code.INVALID_ARGUMENT));
	}

	@Test
	void sayHelloWhenInternalError() {
		HelloRequest request = HelloRequest.newBuilder().setName("internal").build();
		assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> this.hello.sayHello(request))
			.satisfies((ex) -> assertThat(ex.getStatus().getCode()).isEqualTo(Code.INTERNAL));
	}

}
