/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.autoconfigure;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.tomcat.autoconfigure.AutoConfigureWebServerTomcatReactiveTests.ReactiveConfiguration;
import org.springframework.boot.web.server.test.AutoConfigureWebServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigureWebServer @AutoConfigureWebServer} with Tomcat.
 *
 * @author Phillip Webb
 */
@SpringBootTest(classes = ReactiveConfiguration.class, webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = "spring.main.web-application-type=reactive")
@AutoConfigureWebServer
class AutoConfigureWebServerTomcatReactiveTests {

	@LocalServerPort
	private int port;

	@Test
	void startsTestServer() {
		RestTestClient restClient = RestTestClient.bindToServer().baseUrl("http://localhost:" + this.port).build();
		assertThat(RestTestClientResponse.from(restClient.get().exchange())).bodyText().isEqualTo("hello");
	}

	@Configuration
	static class ReactiveConfiguration {

		@Bean
		HttpHandler httpHandler() {
			return (request, response) -> {
				response.setStatusCode(HttpStatus.OK);
				DataBuffer dataBuffer = new DefaultDataBufferFactory().wrap("hello".getBytes(StandardCharsets.UTF_8));
				return response.writeWith(Mono.just(dataBuffer));
			};
		}

	}

}
