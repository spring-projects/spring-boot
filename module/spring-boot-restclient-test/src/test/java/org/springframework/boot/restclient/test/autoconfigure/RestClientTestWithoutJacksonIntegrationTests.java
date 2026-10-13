/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link RestClientTest @RestClientTest} without Jackson.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("jackson-*.jar")
@RestClientTest(ExampleRestClientService.class)
class RestClientTestWithoutJacksonIntegrationTests {

	@Autowired
	private MockRestServiceServer server;

	@Autowired
	private ExampleRestClientService client;

	@Test
	void restClientTestCanBeUsedWhenJacksonIsNotOnTheClassPath() {
		ClassLoader classLoader = getClass().getClassLoader();
		assertThat(ClassUtils.isPresent("com.fasterxml.jackson.databind.Module", classLoader)).isFalse();
		assertThat(ClassUtils.isPresent("tools.jackson.databind.JacksonModule", classLoader)).isFalse();
		this.server.expect(requestTo("https://example.com/test")).andRespond(withSuccess("hello", MediaType.TEXT_HTML));
		assertThat(this.client.test()).isEqualTo("hello");
	}

}
