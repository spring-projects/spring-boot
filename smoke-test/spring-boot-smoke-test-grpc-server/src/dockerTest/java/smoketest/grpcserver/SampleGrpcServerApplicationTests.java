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

package smoketest.grpcserver;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import smoketest.grpcserver.SampleGrpcServerApplicationTests.GrpcServerStartedEventListener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.server.lifecycle.GrpcServerStartedEvent;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the default Spring gRPC netty server.
 *
 * @author Phillip Webb
 */
@SpringBootTest(properties = "spring.grpc.server.port=0")
@Testcontainers(disabledWithoutDocker = true)
@Import(GrpcServerStartedEventListener.class)
class SampleGrpcServerApplicationTests {

	@Autowired
	private GrpcServerStartedEventListener startedEventListener;

	@Test
	@SuppressWarnings("resource")
	void test() {
		int port = this.startedEventListener.getPort();
		String address = "host.testcontainers.internal:" + port;
		org.testcontainers.Testcontainers.exposeHostPorts(port);
		try (GenericContainer<?> container = new GenericContainer<>(
				DockerImageName.parse("fullstorydev/grpcurl:v1.9.3"))
			.withNetworkAliases("")
			.withCommand("-d", "{\"name\": \"spring\"}", "--plaintext", address, "HelloWorld/SayHello")
			.withStartupCheckStrategy(new IndefiniteWaitOneShotStartupCheckStrategy())) {
			container.start();
			assertThat(container.getLogs()).contains("\"message\": \"Hello 'spring'\"");
		}

	}

	static class GrpcServerStartedEventListener implements ApplicationListener<GrpcServerStartedEvent> {

		private int port;

		@Override
		public void onApplicationEvent(GrpcServerStartedEvent event) {
			this.port = event.getPort();
		}

		int getPort() {
			return this.port;
		}

	}

}
