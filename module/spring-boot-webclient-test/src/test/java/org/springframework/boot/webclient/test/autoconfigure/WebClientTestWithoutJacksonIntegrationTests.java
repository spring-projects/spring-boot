/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient.test.autoconfigure;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.annotation.Import;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebClientTest @WebClientTest} without Jackson.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("jackson-*.jar")
@WebClientTest(ExampleWebClientService.class)
@Import(MockWebServerConfiguration.class)
class WebClientTestWithoutJacksonIntegrationTests {

	@Autowired
	private MockWebServer server;

	@Autowired
	private ExampleWebClientService client;

	@Test
	void webClientTestCanBeUsedWhenJacksonIsNotOnTheClassPath() throws InterruptedException {
		ClassLoader classLoader = getClass().getClassLoader();
		assertThat(ClassUtils.isPresent("com.fasterxml.jackson.databind.Module", classLoader)).isFalse();
		assertThat(ClassUtils.isPresent("tools.jackson.databind.JacksonModule", classLoader)).isFalse();
		this.server.enqueue(new MockResponse().setBody("hello"));
		assertThat(this.client.test()).isEqualTo("hello");
		this.server.takeRequest();
	}

}
