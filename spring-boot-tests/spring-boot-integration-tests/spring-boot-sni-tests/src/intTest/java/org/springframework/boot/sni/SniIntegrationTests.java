/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.sni;

import java.io.File;
import java.time.Duration;
import java.util.Map;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.util.Assert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for SSL configuration with SNI.
 *
 * @author Scott Frederick
 */
@Testcontainers(disabledWithoutDocker = true)
class SniIntegrationTests {

	private static final Map<String, String> SERVER_START_MESSAGES = Map.ofEntries(Map.entry("netty", "Netty started"),
			Map.entry("tomcat", "Tomcat initialized"), Map.entry("undertow", "starting server: Undertow"));

	public static final String PRIMARY_SERVER_NAME = "hello.example.com";

	public static final String ALT_SERVER_NAME = "hello-alt.example.com";

	private static final Integer SERVER_PORT = 8443;

	private static final Network SHARED_NETWORK = Network.newNetwork();

	@ParameterizedTest
	@CsvSource({ "reactive,netty", "reactive,tomcat", "servlet,tomcat", "reactive,undertow", "servlet,undertow" })
	void home(String webStack, String server) {
		try (ApplicationContainer serverContainer = new ServerApplicationContainer(webStack, server)) {
			serverContainer.start();
			try {
				Awaitility.await().atMost(Duration.ofSeconds(60)).until(serverContainer::isRunning);
			}
			catch (ConditionTimeoutException ex) {
				System.out.println(serverContainer.getLogs());
				throw ex;
			}
			String serverLogs = serverContainer.getLogs();
			assertThat(serverLogs).contains(SERVER_START_MESSAGES.get(server));
			try (ApplicationContainer clientContainer = new ClientApplicationContainer()) {
				clientContainer.start();
				Awaitility.await().atMost(Duration.ofSeconds(60)).until(() -> !clientContainer.isRunning());
				String clientLogs = clientContainer.getLogs();
				assertServerCalledWithName(clientLogs, PRIMARY_SERVER_NAME);
				assertServerCalledWithName(clientLogs, ALT_SERVER_NAME);
				clientContainer.stop();
			}
			serverContainer.stop();
		}
	}

	private void assertServerCalledWithName(String clientLogs, String serverName) {
		assertThat(clientLogs).contains("Calling server at 'https://" + serverName + ":8443/'")
			.contains("Hello from https://" + serverName + ":8443/");
		assertThat(clientLogs).contains("Calling server actuator at 'https://" + serverName + ":8444/actuator/health'")
			.contains("{\"status\":\"UP\"}");
	}

	static final class ClientApplicationContainer extends ApplicationContainer {

		ClientApplicationContainer() {
			super("spring-boot-sni-client-app", "", PRIMARY_SERVER_NAME, ALT_SERVER_NAME);
		}

	}

	static final class ServerApplicationContainer extends ApplicationContainer {

		ServerApplicationContainer(String webStack, String server) {
			super("spring-boot-sni-" + webStack + "-app", "-" + server);
			withNetworkAliases(PRIMARY_SERVER_NAME, ALT_SERVER_NAME);
		}

	}

	static class ApplicationContainer extends GenericContainer<ApplicationContainer> {

		protected ApplicationContainer(String appName, String fileSuffix, String... entryPointArgs) {
			super(new ImageFromDockerfile().withFileFromFile("spring-boot.jar", findJarFile(appName, fileSuffix))
				.withDockerfileFromBuilder((builder) -> builder.from("eclipse-temurin:17-jre-jammy")
					.add("spring-boot.jar", "/spring-boot.jar")
					.entryPoint(buildEntryPoint(entryPointArgs))));
			withExposedPorts(SERVER_PORT);
			withStartupTimeout(Duration.ofMinutes(2));
			withStartupAttempts(3);
			withNetwork(SHARED_NETWORK);
			withNetworkMode(SHARED_NETWORK.getId());
		}

		private static File findJarFile(String appName, String fileSuffix) {
			String path = String.format("build/%1$s/build/libs/%1$s%2$s.jar", appName, fileSuffix);
			File jar = new File(path);
			Assert.state(jar.isFile(), () -> "Could not find " + path);
			return jar;
		}

		private static String buildEntryPoint(String... args) {
			StringBuilder builder = new StringBuilder().append("java").append(" -jar").append(" /spring-boot.jar");
			for (String arg : args) {
				builder.append(" ").append(arg);
			}
			return builder.toString();
		}

	}

}
