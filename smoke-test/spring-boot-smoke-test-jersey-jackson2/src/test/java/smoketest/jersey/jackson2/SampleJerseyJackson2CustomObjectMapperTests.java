/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jersey.jackson2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SampleJerseyJackson2Application} with a user-defined
 * {@link ObjectMapper}.
 *
 * @author Kristoffer Larsen Hopland
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(SampleJerseyJackson2CustomObjectMapperTests.CustomObjectMapperConfiguration.class)
class SampleJerseyJackson2CustomObjectMapperTests {

	@LocalServerPort
	private int port;

	@Test
	void resourceIsSerializedUsingCustomObjectMapper() {
		String body = RestClient.create()
			.get()
			.uri("http://localhost:" + this.port + "/name")
			.retrieve()
			.body(String.class);
		assertThat(body).isEqualTo("{\"FirstName\":\"Spring\",\"LastName\":\"Boot\"}");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class CustomObjectMapperConfiguration {

		@Bean
		ObjectMapper objectMapper() {
			return new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.UPPER_CAMEL_CASE);
		}

	}

}
