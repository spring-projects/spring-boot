/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.session.actuate.endpoint;

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
