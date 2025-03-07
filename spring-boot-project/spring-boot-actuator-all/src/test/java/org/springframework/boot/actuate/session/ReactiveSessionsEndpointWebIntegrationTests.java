/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.session;

import java.util.Collections;

import net.minidev.json.JSONArray;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest;
import org.springframework.boot.actuate.endpoint.web.test.WebEndpointTest.Infrastructure;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSession;
import org.springframework.session.ReactiveFindByIndexNameSessionRepository;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.Session;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for {@link ReactiveSessionsEndpoint} exposed by WebFlux.
 *
 * @author Vedran Pavic
 * @author Moritz Halbritter
 */
class ReactiveSessionsEndpointWebIntegrationTests {

	private static final Session session = new MapSession();

	@SuppressWarnings("unchecked")
	private static final ReactiveSessionRepository<Session> sessionRepository = mock(ReactiveSessionRepository.class);

	@SuppressWarnings("unchecked")
	private static final ReactiveFindByIndexNameSessionRepository<Session> indexedSessionRepository = mock(
			ReactiveFindByIndexNameSessionRepository.class);

	@WebEndpointTest(infrastructure = Infrastructure.WEBFLUX)
	void sessionsForUsernameWithoutUsernameParam(WebTestClient client) {
		client.get()
			.uri((builder) -> builder.path("/actuator/sessions").build())
			.exchange()
			.expectStatus()
			.is4xxClientError();
	}

	@WebEndpointTest(infrastructure = Infrastructure.WEBFLUX)
	void sessionsForUsernameNoResults(WebTestClient client) {
		given(indexedSessionRepository.findByPrincipalName("user")).willReturn(Mono.just(Collections.emptyMap()));
		client.get()
			.uri((builder) -> builder.path("/actuator/sessions").queryParam("username", "user").build())
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("sessions")
			.isEmpty();
	}

	@WebEndpointTest(infrastructure = Infrastructure.WEBFLUX)
	void sessionsForUsernameFound(WebTestClient client) {
		given(indexedSessionRepository.findByPrincipalName("user"))
			.willReturn(Mono.just(Collections.singletonMap(session.getId(), session)));
		client.get()
			.uri((builder) -> builder.path("/actuator/sessions").queryParam("username", "user").build())
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("sessions.[*].id")
			.isEqualTo(new JSONArray().appendElement(session.getId()));
	}

	@WebEndpointTest(infrastructure = Infrastructure.WEBFLUX)
	void sessionForIdFound(WebTestClient client) {
		given(sessionRepository.findById(session.getId())).willReturn(Mono.just(session));
		client.get()
			.uri((builder) -> builder.path("/actuator/sessions/{id}").build(session.getId()))
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("id")
			.isEqualTo(session.getId());
	}

	@WebEndpointTest(infrastructure = Infrastructure.WEBFLUX)
	void sessionForIdNotFound(WebTestClient client) {
		given(sessionRepository.findById("not-found")).willReturn(Mono.empty());
		client.get()
			.uri((builder) -> builder.path("/actuator/sessions/not-found").build())
			.exchange()
			.expectStatus()
			.isNotFound();
	}

	@WebEndpointTest(infrastructure = Infrastructure.WEBFLUX)
	void deleteSession(WebTestClient client) {
		given(sessionRepository.deleteById(session.getId())).willReturn(Mono.empty());
		client.delete()
			.uri((builder) -> builder.path("/actuator/sessions/{id}").build(session.getId()))
			.exchange()
			.expectStatus()
			.isNoContent();
	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfiguration {

		@Bean
		ReactiveSessionsEndpoint sessionsEndpoint() {
			return new ReactiveSessionsEndpoint(sessionRepository, indexedSessionRepository);
		}

	}

}
