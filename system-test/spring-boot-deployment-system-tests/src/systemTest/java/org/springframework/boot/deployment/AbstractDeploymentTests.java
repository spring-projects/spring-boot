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

package org.springframework.boot.deployment;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.TimeValue;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract class for deployment tests.
 */
abstract class AbstractDeploymentTests {

	protected static final int DEFAULT_PORT = 8080;

	@Test
	void home() {
		getDeployedApplication().test((rest) -> {
			ResponseEntity<String> response = rest.getForEntity("/", String.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isEqualTo("Hello World");
		});
	}

	@Test
	void health() {
		getDeployedApplication().test((rest) -> {
			ResponseEntity<String> response = rest.getForEntity("/actuator/health", String.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isEqualTo("{\"groups\":[\"liveness\",\"readiness\"],\"status\":\"UP\"}");
		});
	}

	@Test
	void conditionalOnWarShouldBeTrue() {
		getDeployedApplication().test((rest) -> {
			ResponseEntity<String> response = rest.getForEntity("/actuator/war", String.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isEqualTo("{\"hello\":\"world\"}");
		});
	}

	private DeployedApplication getDeployedApplication() {
		return new DeployedApplication(getContainer(), getPort());
	}

	protected int getPort() {
		return DEFAULT_PORT;
	}

	abstract WarDeploymentContainer getContainer();

	static final class DeployedApplication {

		private final WarDeploymentContainer container;

		private final int port;

		DeployedApplication(WarDeploymentContainer container, int port) {
			this.container = container;
			this.port = port;
		}

		private void test(Consumer<TestRestTemplate> consumer) {
			TestRestTemplate rest = new TestRestTemplate(new RestTemplateBuilder()
				.rootUri("http://" + this.container.getHost() + ":" + this.container.getMappedPort(this.port)
						+ "/spring-boot")
				.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(HttpClients.custom()
					.setRetryStrategy(new DefaultHttpRequestRetryStrategy(10, TimeValue.of(1, TimeUnit.SECONDS)))
					.build())));
			try {
				Awaitility.await().atMost(Duration.ofMinutes(10)).until(() -> {
					try {
						System.out.println(this.container.getLogs());
						consumer.accept(rest);
						return true;
					}
					catch (Throwable ex) {
						return false;
					}
				});
			}
			catch (ConditionTimeoutException ex) {
				System.out.println(this.container.getLogs());
				throw ex;
			}
		}

	}

	static final class WarDeploymentContainer extends GenericContainer<WarDeploymentContainer> {

		WarDeploymentContainer(String baseImage, String deploymentLocation, int port) {
			this(baseImage, deploymentLocation, port, null);
		}

		WarDeploymentContainer(String baseImage, String deploymentLocation, int port,
				Consumer<DockerfileBuilder> dockerfileCustomizer) {
			super(new ImageFromDockerfile().withFileFromFile("spring-boot.war", findWarToDeploy())
				.withDockerfileFromBuilder((builder) -> {
					builder.from(baseImage).add("spring-boot.war", deploymentLocation + "/spring-boot.war");
					if (dockerfileCustomizer != null) {
						dockerfileCustomizer.accept(builder);
					}
				}));
			withExposedPorts(port).withStartupTimeout(Duration.ofMinutes(5)).withStartupAttempts(3);
		}

		private static File findWarToDeploy() {
			File[] candidates = new File("build/libs").listFiles();
			assertThat(candidates).hasSize(1);
			return candidates[0];
		}

	}

}
