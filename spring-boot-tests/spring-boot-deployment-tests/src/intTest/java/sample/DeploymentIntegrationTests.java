/*
 * Copyright 2012-2020 the original author or authors.
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

package sample;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testsupport.testcontainers.DisabledIfDockerUnavailable;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Deployment integration tests.
 */
@DisabledIfDockerUnavailable
class DeploymentIntegrationTests {

	@ParameterizedTest
	@MethodSource("deployedApplications")
	void home(DeployedApplication app) {
		app.test((rest) -> {
			ResponseEntity<String> response = rest.getForEntity("/", String.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isEqualTo("Hello World");
		});
	}

	@ParameterizedTest
	@MethodSource("deployedApplications")
	void health(DeployedApplication application) {
		application.test((rest) -> {
			ResponseEntity<String> response = rest.getForEntity("/actuator/health", String.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isEqualTo("{\"status\":\"UP\"}");
		});
	}

	@ParameterizedTest
	@MethodSource("deployedApplications")
	void conditionalOnWarShouldBeTrue(DeployedApplication application) throws Exception {
		application.test((rest) -> {
			ResponseEntity<String> response = rest.getForEntity("/actuator/war", String.class);
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
			assertThat(response.getBody()).isEqualTo("{\"hello\":\"world\"}");
		});
	}

	static List<DeployedApplication> deployedApplications() {
		return Arrays.asList(
				new DeployedApplication("openliberty/open-liberty:20.0.0.9-kernel-java8-openj9-ubi", "/config/dropins",
						9080),
				new DeployedApplication("tomcat:9.0.37-jdk8-openjdk", "/usr/local/tomcat/webapps", 8080),
				new DeployedApplication("tomee:8-jre-8.0.2-webprofile", "/usr/local/tomee/webapps", 8080),
				new DeployedApplication("jboss/wildfly:20.0.1.Final", "/opt/jboss/wildfly/standalone/deployments",
						8080));
	}

	public static final class DeployedApplication {

		private final String baseImage;

		private final String deploymentLocation;

		private final int port;

		private DeployedApplication(String baseImage, String deploymentLocation, int port) {
			this.baseImage = baseImage;
			this.deploymentLocation = deploymentLocation;
			this.port = port;
		}

		private void test(Consumer<TestRestTemplate> consumer) {
			try (WarDeploymentContainer container = new WarDeploymentContainer(this.baseImage, this.deploymentLocation,
					this.port)) {
				container.start();
				TestRestTemplate rest = new TestRestTemplate(new RestTemplateBuilder()
						.rootUri("http://" + container.getHost() + ":" + container.getMappedPort(this.port)
								+ "/spring-boot")
						.requestFactory(() -> new HttpComponentsClientHttpRequestFactory(HttpClients.custom()
								.setRetryHandler(new StandardHttpRequestRetryHandler(10, false)).build())));
				try {
					Awaitility.await().atMost(Duration.ofMinutes(10)).until(() -> {
						try {
							consumer.accept(rest);
							return true;
						}
						catch (Throwable ex) {
							return false;
						}
					});
				}
				catch (ConditionTimeoutException ex) {
					System.out.println(container.getLogs());
					throw ex;
				}
			}
		}

		@Override
		public String toString() {
			return this.baseImage;
		}

	}

	private static final class WarDeploymentContainer extends GenericContainer<WarDeploymentContainer> {

		private WarDeploymentContainer(String baseImage, String deploymentLocation, int port) {
			super(new ImageFromDockerfile().withFileFromFile("spring-boot.war", findWarToDeploy())
					.withDockerfileFromBuilder((builder) -> builder.from(baseImage)
							.add("spring-boot.war", deploymentLocation + "/spring-boot.war").build()));
			withExposedPorts(port).withStartupTimeout(Duration.ofMinutes(5)).withStartupAttempts(3);
		}

		private static File findWarToDeploy() {
			File[] candidates = new File("build/libs").listFiles();
			assertThat(candidates).hasSize(1);
			return candidates[0];
		}

	}

}
