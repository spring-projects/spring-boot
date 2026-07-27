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

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.TimeValue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.BodySpec;
import org.springframework.test.web.servlet.client.StatusAssertions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for DevTools.
 *
 * @author Andy Wilkinson
 */
@SuppressWarnings("removal")
class DevToolsIntegrationTests extends AbstractDevToolsIntegrationTests {

	private final RestTestClient client = RestTestClient
		.bindToServer(new HttpComponentsClientHttpRequestFactory(HttpClients.custom()
			.setRetryStrategy(new DefaultHttpRequestRetryStrategy(10, TimeValue.of(1, TimeUnit.SECONDS)))
			.build()))
		.build();

	@ParameterizedTest(name = "{0}")
	@MethodSource("parameters")
	void addARequestMappingToAnExistingController(ApplicationLauncher applicationLauncher) throws Exception {
		launchApplication(applicationLauncher, "--logging.level.org.springframework.boot=trace");
		String urlBase = "http://localhost:" + awaitServerPort();
		expectResponseBody(urlBase + "/one").isEqualTo("one");
		expectResponseStatus(urlBase + "/two").isNotFound();
		controller("com.example.ControllerOne").withRequestMapping("one").withRequestMapping("two").build();
		urlBase = "http://localhost:" + awaitServerPort();
		expectResponseBody(urlBase + "/one").isEqualTo("one");
		expectResponseBody(urlBase + "/two").isEqualTo("two");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("parameters")
	void removeARequestMappingFromAnExistingController(ApplicationLauncher applicationLauncher) throws Exception {
		launchApplication(applicationLauncher);
		String urlBase = "http://localhost:" + awaitServerPort();
		expectResponseBody(urlBase + "/one").isEqualTo("one");
		controller("com.example.ControllerOne").build();
		urlBase = "http://localhost:" + awaitServerPort();
		expectResponseStatus(urlBase + "/one").isNotFound();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("parameters")
	void createAController(ApplicationLauncher applicationLauncher) throws Exception {
		launchApplication(applicationLauncher);
		String urlBase = "http://localhost:" + awaitServerPort();
		expectResponseBody(urlBase + "/one").isEqualTo("one");
		expectResponseStatus(urlBase + "/two").isNotFound();
		controller("com.example.ControllerTwo").withRequestMapping("two").build();
		urlBase = "http://localhost:" + awaitServerPort();
		expectResponseBody(urlBase + "/one").isEqualTo("one");
		expectResponseBody(urlBase + "/two").isEqualTo("two");

	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("parameters")
	void createAControllerAndThenAddARequestMapping(ApplicationLauncher applicationLauncher) throws Exception {
		launchApplication(applicationLauncher);
		String urlBase = "http://localhost:" + awaitServerPort();
		expectResponseBody(urlBase + "/one").isEqualTo("one");
		expectResponseStatus(urlBase + "/two").isNotFound();
		controller("com.example.ControllerTwo").withRequestMapping("two").build();
		urlBase = "http://localhost:" + awaitServerPort();
		expectResponseBody(urlBase + "/one").isEqualTo("one");
		expectResponseBody(urlBase + "/two").isEqualTo("two");
		controller("com.example.ControllerTwo").withRequestMapping("two").withRequestMapping("three").build();
		urlBase = "http://localhost:" + awaitServerPort();
		expectResponseBody(urlBase + "/three").isEqualTo("three");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("parameters")
	void createAControllerAndThenAddARequestMappingToAnExistingController(ApplicationLauncher applicationLauncher)
			throws Exception {
		launchApplication(applicationLauncher);
		String urlBase = "http://localhost:" + awaitServerPort();
		expectResponseBody(urlBase + "/one").isEqualTo("one");
		expectResponseStatus(urlBase + "/two").isNotFound();
		controller("com.example.ControllerTwo").withRequestMapping("two").build();
		urlBase = "http://localhost:" + awaitServerPort();
		expectResponseBody(urlBase + "/one").isEqualTo("one");
		expectResponseBody(urlBase + "/two").isEqualTo("two");
		controller("com.example.ControllerOne").withRequestMapping("one").withRequestMapping("three").build();
		urlBase = "http://localhost:" + awaitServerPort();
		expectResponseBody(urlBase + "/one").isEqualTo("one");
		expectResponseBody(urlBase + "/two").isEqualTo("two");
		expectResponseBody(urlBase + "/three").isEqualTo("three");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("parameters")
	void deleteAController(ApplicationLauncher applicationLauncher) throws Exception {
		LaunchedApplication launchedApplication = launchApplication(applicationLauncher);
		String urlBase = "http://localhost:" + awaitServerPort();
		expectResponseBody(urlBase + "/one").isEqualTo("one");
		assertThat(new File(launchedApplication.getClassesDirectory(), "com/example/ControllerOne.class").delete())
			.isTrue();
		urlBase = "http://localhost:" + awaitServerPort();
		expectResponseStatus(urlBase + "/one").isNotFound();

	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("parameters")
	void createAControllerAndThenDeleteIt(ApplicationLauncher applicationLauncher) throws Exception {
		LaunchedApplication launchedApplication = launchApplication(applicationLauncher);
		String urlBase = "http://localhost:" + awaitServerPort();
		expectResponseBody(urlBase + "/one").isEqualTo("one");
		expectResponseStatus(urlBase + "/two").isNotFound();
		controller("com.example.ControllerTwo").withRequestMapping("two").build();
		urlBase = "http://localhost:" + awaitServerPort();
		expectResponseBody(urlBase + "/one").isEqualTo("one");
		expectResponseBody(urlBase + "/two").isEqualTo("two");
		assertThat(new File(launchedApplication.getClassesDirectory(), "com/example/ControllerTwo.class").delete())
			.isTrue();
		urlBase = "http://localhost:" + awaitServerPort();
		expectResponseStatus(urlBase + "/two").isNotFound();
	}

	static Object[] parameters() {
		Directories directories = new Directories(buildOutput, temp);
		return new Object[] { new Object[] { new LocalApplicationLauncher(directories) },
				new Object[] { new ExplodedRemoteApplicationLauncher(directories) },
				new Object[] { new JarFileRemoteApplicationLauncher(directories) } };
	}

	private BodySpec<String, ?> expectResponseBody(String url) {
		return this.client.get().uri(url).exchangeSuccessfully().expectBody(String.class);
	}

	private StatusAssertions expectResponseStatus(String url) {
		return this.client.get().uri(url).exchange().expectStatus();
	}

}
