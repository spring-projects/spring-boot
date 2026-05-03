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

package smoketest.grpcclient;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import smoketest.grpcclient.SampleGrpcClientApplicationTests.MockServerInitializer;
import smoketest.grpcclient.proto.HelloReply;
import smoketest.grpcclient.proto.HelloRequest;
import smoketest.grpcclient.proto.HelloWorldGrpc;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.grpc.server.NettyGrpcServerFactory;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ContextConfiguration(initializers = MockServerInitializer.class)
@ExtendWith(OutputCaptureExtension.class)
class SampleGrpcClientApplicationTests {

	@Test
	void applicationRunsAndCallsGrpcServer(CapturedOutput output) {
		assertThat(output).contains(">>> Hello 'Spring'");
	}

	static class MockServerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			NettyGrpcServerFactory serverFactory = new NettyGrpcServerFactory("*:0", Collections.emptyList(), null,
					null, null);
			HelloWorldService helloWorldService = new HelloWorldService();
			serverFactory.addService(helloWorldService.bindService());
			GrpcServerLifecycle lifecycle = new GrpcServerLifecycle(serverFactory, Duration.ofSeconds(30),
					applicationContext);
			lifecycle.start();
			String target = "static://localhost:%s".formatted(lifecycle.getPort());
			applicationContext.getEnvironment()
				.getPropertySources()
				.addFirst(new MapPropertySource("grpc", Map.of("spring.grpc.client.channel.default.target", target)));
			applicationContext.getBeanFactory().registerSingleton("grpcServerLifecyce", lifecycle);
		}

	}

	static class HelloWorldService extends HelloWorldGrpc.HelloWorldImplBase {

		@Override
		public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
			HelloReply reply = HelloReply.newBuilder().setMessage("Hello '%s'".formatted(request.getName())).build();
			responseObserver.onNext(reply);
			responseObserver.onCompleted();
		}

		@Override
		public void streamHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
			throw new IllegalStateException();
		}

	}

}
