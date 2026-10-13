/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for
 * {@link AutoConfigureMockRestServiceServer @AutoConfigureMockRestServiceServer} with a
 * {@link RestClient} configured with a base URL.
 *
 * @author Scott Frederick
 */
@SpringBootTest
@AutoConfigureMockRestServiceServer
class AutoConfigureMockRestServiceServerWithRestClientIntegrationTests {

	@Autowired
	private RestClient restClient;

	@Autowired
	private MockRestServiceServer server;

	@Test
	void mockServerExpectationsAreMatched() {
		this.server.expect(requestTo("/rest/test")).andRespond(withSuccess("hello", MediaType.TEXT_HTML));
		ResponseEntity<String> entity = this.restClient.get().uri("/test").retrieve().toEntity(String.class);
		assertThat(entity.getBody()).isEqualTo("hello");
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	static class RootUriConfiguration {

		@Bean
		RestClient restClient(Builder restClientBuilder) {
			return restClientBuilder.baseUrl("/rest").build();
		}

	}

}
