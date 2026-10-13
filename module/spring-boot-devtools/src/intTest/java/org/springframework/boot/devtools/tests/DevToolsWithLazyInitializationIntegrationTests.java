/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.tests;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Integration tests for DevTools with lazy initialization enabled.
 *
 * @author Madhura Bhave
 */
class DevToolsWithLazyInitializationIntegrationTests extends AbstractDevToolsIntegrationTests {

	@ParameterizedTest(name = "{0}")
	@MethodSource("parameters")
	void addARequestMappingToAnExistingControllerWhenLazyInit(ApplicationLauncher applicationLauncher)
			throws Exception {
		launchApplication(applicationLauncher, "--spring.main.lazy-initialization=true");
		RestTestClient client = RestTestClient.bindToServer().build();
		String urlBase = "http://localhost:" + awaitServerPort();
		client.get().uri(urlBase + "/one").exchangeSuccessfully().expectBody(String.class).isEqualTo("one");
		client.get().uri(urlBase + "/two").exchange().expectStatus().isNotFound();
		controller("com.example.ControllerOne").withRequestMapping("one").withRequestMapping("two").build();
		urlBase = "http://localhost:" + awaitServerPort();
		client.get().uri(urlBase + "/one").exchangeSuccessfully().expectBody(String.class).isEqualTo("one");
		client.get().uri(urlBase + "/two").exchangeSuccessfully().expectBody(String.class).isEqualTo("two");
	}

	static Object[] parameters() {
		Directories directories = new Directories(buildOutput, temp);
		return new Object[] { new Object[] { new LocalApplicationLauncher(directories) },
				new Object[] { new ExplodedRemoteApplicationLauncher(directories) },
				new Object[] { new JarFileRemoteApplicationLauncher(directories) } };

	}

}
