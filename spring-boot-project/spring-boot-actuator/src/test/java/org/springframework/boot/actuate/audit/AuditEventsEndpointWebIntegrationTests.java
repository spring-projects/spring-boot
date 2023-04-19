/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.audit;

import java.time.Instant;
import java.util.Collections;

import net.minidev.json.JSONArray;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for {@link AuditEventsEndpoint} exposed by Jersey, Spring MVC, and
 * WebFlux.
 *
 * @author Vedran Pavic
 * @author Andy Wilkinson
 */
class AuditEventsEndpointWebIntegrationTests {

	@WebEndpointTest
	void allEvents(WebTestClient client) {
		client.get()
			.uri((builder) -> builder.path("/actuator/auditevents").build())
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("events.[*].principal")
			.isEqualTo(new JSONArray().appendElement("admin").appendElement("admin").appendElement("user"));
	}

	@WebEndpointTest
	void eventsAfter(WebTestClient client) {
		client.get()
			.uri((builder) -> builder.path("/actuator/auditevents")
				.queryParam("after", "2016-11-01T13:00:00%2B00:00")
				.build())
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("events")
			.isEmpty();
	}

	@WebEndpointTest
	void eventsWithPrincipal(WebTestClient client) {
		client.get()
			.uri((builder) -> builder.path("/actuator/auditevents").queryParam("principal", "user").build())
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("events.[*].principal")
			.isEqualTo(new JSONArray().appendElement("user"));
	}

	@WebEndpointTest
	void eventsWithType(WebTestClient client) {
		client.get()
			.uri((builder) -> builder.path("/actuator/auditevents").queryParam("type", "logout").build())
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("events.[*].principal")
			.isEqualTo(new JSONArray().appendElement("admin"))
			.jsonPath("events.[*].type")
			.isEqualTo(new JSONArray().appendElement("logout"));
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		AuditEventRepository auditEventsRepository() {
			AuditEventRepository repository = new InMemoryAuditEventRepository(3);
			repository.add(createEvent("2016-11-01T11:00:00Z", "admin", "login"));
			repository.add(createEvent("2016-11-01T12:00:00Z", "admin", "logout"));
			repository.add(createEvent("2016-11-01T12:00:00Z", "user", "login"));
			return repository;
		}

		@Bean
		AuditEventsEndpoint auditEventsEndpoint(AuditEventRepository auditEventRepository) {
			return new AuditEventsEndpoint(auditEventRepository);
		}

		private AuditEvent createEvent(String instant, String principal, String type) {
			return new AuditEvent(Instant.parse(instant), principal, type, Collections.emptyMap());
		}

	}

}
