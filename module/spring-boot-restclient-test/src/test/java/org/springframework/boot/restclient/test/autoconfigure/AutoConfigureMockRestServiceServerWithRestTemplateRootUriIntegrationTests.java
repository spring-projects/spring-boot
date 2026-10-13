/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restclient.test.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for
 * {@link AutoConfigureMockRestServiceServer @AutoConfigureMockRestServiceServer} with a
 * {@link RestTemplate} configured with a root URI.
 *
 * @author Andy Wilkinson
 * @deprecated since 4.2.0 for removal in 4.4.0
 */
@SpringBootTest
@AutoConfigureMockRestServiceServer
@Deprecated(since = "4.2.0", forRemoval = true)
@SuppressWarnings("removal")
class AutoConfigureMockRestServiceServerWithRestTemplateRootUriIntegrationTests {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private MockRestServiceServer server;

	@Autowired
	MeterRegistry meterRegistry;

	@Test
	void whenRestTemplateAppliesARootUriThenMockServerExpectationsAreStillMatched() {
		this.server.expect(requestTo("/test")).andRespond(withSuccess("hello", MediaType.TEXT_HTML));
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/test", String.class);
		assertThat(entity.getBody()).isEqualTo("hello");
		assertThat(this.meterRegistry.find("http.client.requests").tag("uri", "/test").timer()).isNotNull();
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	static class RootUriConfiguration {

		@Bean
		RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
			return restTemplateBuilder.baseUri("/rest").build();
		}

	}

}
