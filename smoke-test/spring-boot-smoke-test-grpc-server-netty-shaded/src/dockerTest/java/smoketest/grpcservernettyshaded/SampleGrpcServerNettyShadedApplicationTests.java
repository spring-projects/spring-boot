/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.grpcservernettyshaded;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Spring gRPC with a shaded netty server.
 *
 * @author Phillip Webb
 */
@SpringBootTest(properties = "spring.grpc.server.port=0")
@Testcontainers(disabledWithoutDocker = true)
class SampleGrpcServerNettyShadedApplicationTests {

	@Value("${local.grpc.server.port}")
	private int port;

	@Test
	@SuppressWarnings("resource")
	void test() {
		String address = "host.testcontainers.internal:" + this.port;
		org.testcontainers.Testcontainers.exposeHostPorts(this.port);
		try (GenericContainer<?> container = new GenericContainer<>(
				DockerImageName.parse("fullstorydev/grpcurl:v1.9.3"))
			.withCommand("-d", "{\"name\": \"spring\"}", "--plaintext", address, "HelloWorld/SayHello")
			.withStartupCheckStrategy(new IndefiniteWaitOneShotStartupCheckStrategy())) {
			container.start();
			assertThat(container.getLogs()).contains("\"message\": \"Hello 'spring'\"");
		}

	}

}
