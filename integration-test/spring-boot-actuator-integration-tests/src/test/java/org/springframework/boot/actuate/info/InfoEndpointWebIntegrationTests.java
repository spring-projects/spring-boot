/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.info;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for {@link InfoEndpoint} exposed by Jersey, Spring MVC, and WebFlux.
 *
 * @author Meang Akira Tanaka
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
@TestPropertySource(properties = { "info.app.name=MyService" })
class InfoEndpointWebIntegrationTests {

	@WebEndpointTest
	void info(WebTestClient client) {
		client.get()
			.uri("/actuator/info")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("beanName1.key11")
			.isEqualTo("value11")
			.jsonPath("beanName1.key12")
			.isEqualTo("value12")
			.jsonPath("beanName2.key21")
			.isEqualTo("value21")
			.jsonPath("beanName2.key22")
			.isEqualTo("value22");
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		InfoEndpoint endpoint(ObjectProvider<InfoContributor> infoContributors) {
			return new InfoEndpoint(infoContributors.orderedStream().toList());
		}

		@Bean
		InfoContributor beanName1() {
			return (builder) -> {
				Map<String, Object> content = new LinkedHashMap<>();
				content.put("key11", "value11");
				content.put("key12", "value12");
				builder.withDetail("beanName1", content);
			};
		}

		@Bean
		InfoContributor beanName2() {
			return (builder) -> {
				Map<String, Object> content = new LinkedHashMap<>();
				content.put("key21", "value21");
				content.put("key22", "value22");
				builder.withDetail("beanName2", content);
			};
		}

	}

}
