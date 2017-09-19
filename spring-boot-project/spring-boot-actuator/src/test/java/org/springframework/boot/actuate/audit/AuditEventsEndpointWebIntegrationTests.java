/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Date;

import net.minidev.json.JSONArray;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointRunners;
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
@RunWith(WebEndpointRunners.class)
public class AuditEventsEndpointWebIntegrationTests {

	private static WebTestClient client;

	@Test
	public void eventsWithoutParams() throws Exception {
		client.get().uri((builder) -> builder.path("/application/auditevents").build())
				.exchange().expectStatus().isBadRequest();
	}

	@Test
	public void eventsWithDateAfter() throws Exception {
		client.get()
				.uri((builder) -> builder.path("/application/auditevents")
						.queryParam("after", "2016-11-01T13:00:00%2B00:00").build())
				.exchange().expectStatus().isOk().expectBody().jsonPath("events")
				.isEmpty();
	}

	@Test
	public void eventsWithPrincipalAndDateAfter() throws Exception {
		client.get()
				.uri((builder) -> builder.path("/application/auditevents")
						.queryParam("after", "2016-11-01T10:00:00%2B00:00")
						.queryParam("principal", "user").build())
				.exchange().expectStatus().isOk().expectBody()
				.jsonPath("events.[*].principal")
				.isEqualTo(new JSONArray().appendElement("user"));
	}

	@Test
	public void eventsWithPrincipalDateAfterAndType() throws Exception {
		client.get()
				.uri((builder) -> builder.path("/application/auditevents")
						.queryParam("after", "2016-11-01T10:00:00%2B00:00")
						.queryParam("principal", "admin").queryParam("type", "logout")
						.build())
				.exchange().expectStatus().isOk().expectBody()
				.jsonPath("events.[*].principal")
				.isEqualTo(new JSONArray().appendElement("admin"))
				.jsonPath("events.[*].type")
				.isEqualTo(new JSONArray().appendElement("logout"));
	}

	@Configuration
	protected static class TestConfiguration {

		@Bean
		public AuditEventRepository auditEventsRepository() {
			AuditEventRepository repository = new InMemoryAuditEventRepository(3);
			repository.add(createEvent("2016-11-01T11:00:00Z", "admin", "login"));
			repository.add(createEvent("2016-11-01T12:00:00Z", "admin", "logout"));
			repository.add(createEvent("2016-11-01T12:00:00Z", "user", "login"));
			return repository;
		}

		@Bean
		public AuditEventsEndpoint auditEventsEndpoint() {
			return new AuditEventsEndpoint(auditEventsRepository());
		}

		@Bean
		public AuditEventsWebEndpointExtension auditEventsWebEndpointExtension(
				AuditEventsEndpoint auditEventsEndpoint) {
			return new AuditEventsWebEndpointExtension(auditEventsEndpoint);
		}

		private AuditEvent createEvent(String instant, String principal, String type) {
			return new AuditEvent(Date.from(Instant.parse(instant)), principal, type,
					Collections.<String, Object>emptyMap());
		}

	}

}
