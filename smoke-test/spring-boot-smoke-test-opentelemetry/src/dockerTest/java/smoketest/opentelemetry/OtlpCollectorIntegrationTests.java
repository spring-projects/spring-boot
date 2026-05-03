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

package smoketest.opentelemetry;

import java.time.Duration;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for OpenTelemetry environment variable mapping with a real OTLP
 * collector.
 *
 * @author Moritz Halbritter
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class OtlpCollectorIntegrationTests {

	private static final int OTLP_HTTP_PORT = 4318;

	private static final int OTLP_EXPOSED_PORT = 12345;

	private static final ToStringConsumer collectorOutput = new ToStringConsumer();

	@Container
	private static final OtlpCollectorContainer collector = new OtlpCollectorContainer()
		.withCopyFileToContainer(MountableFile.forClasspathResource("otel-collector-config.yaml"),
				"/etc/otel-collector-config.yaml")
		.withCopyFileToContainer(MountableFile.forClasspathResource("certs/server.crt"), "/etc/certs/server.crt")
		.withCopyFileToContainer(MountableFile.forClasspathResource("certs/server.key", 644), "/etc/certs/server.key")
		.withCommand("--config=/etc/otel-collector-config.yaml")
		.withLogConsumer(collectorOutput)
		.waitingFor(Wait.forLogMessage(".*Everything is ready.*", 1).withStartupTimeout(Duration.ofSeconds(10)));

	@LocalServerPort
	private int port;

	@Test
	void tracesAreSentToCollector() {
		RestClient restClient = RestClient.create();
		String response = restClient.get().uri("http://localhost:" + this.port + "/").retrieve().body(String.class);
		assertThat(response).contains("Greetings from Spring Boot!");
		Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			String log = getContainerLog();
			assertThat(log).contains("service.name: Str(my-service)")
				.contains("service.version: Str(1.0.0)")
				.containsAnyOf("Span", "ScopeSpans");
		});
	}

	@Test
	void metricsAreSentToCollector() {
		Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			String log = getContainerLog();
			assertThat(log).contains("service.name: Str(my-service)")
				.contains("service.version: Str(1.0.0)")
				.containsAnyOf("jvm.memory.used", "ResourceMetrics", "ScopeMetrics");
		});
	}

	private static String getContainerLog() {
		return collectorOutput.toUtf8String();
	}

	private static class OtlpCollectorContainer extends GenericContainer<OtlpCollectorContainer> {

		OtlpCollectorContainer() {
			super(DockerImageName.parse(TestImage.OTEL_COLLECTOR.toString()));
		}

		@Override
		protected void configure() {
			super.configure();
			addFixedExposedPort(OTLP_EXPOSED_PORT, OTLP_HTTP_PORT, InternetProtocol.TCP);
		}

	}

}
