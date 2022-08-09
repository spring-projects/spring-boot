/*
 * Copyright 2012-2022 the original author or authors.
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

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.session.MapSession;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.Session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ReactiveSessionsEndpoint}.
 *
 * @author Vedran Pavic
 */
class ReactiveSessionsEndpointTests {

	private static final Session session = new MapSession();

	@SuppressWarnings("unchecked")
	private final ReactiveSessionRepository<Session> sessionRepository = mock(ReactiveSessionRepository.class);

	private final ReactiveSessionsEndpoint endpoint = new ReactiveSessionsEndpoint(this.sessionRepository);

	@Test
	void getSession() {
		given(this.sessionRepository.findById(session.getId())).willReturn(Mono.just(session));
		StepVerifier.create(this.endpoint.getSession(session.getId())).consumeNextWith((result) -> {
			assertThat(result.getId()).isEqualTo(session.getId());
			assertThat(result.getAttributeNames()).isEqualTo(session.getAttributeNames());
			assertThat(result.getCreationTime()).isEqualTo(session.getCreationTime());
			assertThat(result.getLastAccessedTime()).isEqualTo(session.getLastAccessedTime());
			assertThat(result.getMaxInactiveInterval()).isEqualTo(session.getMaxInactiveInterval().getSeconds());
			assertThat(result.isExpired()).isEqualTo(session.isExpired());
		}).verifyComplete();
		then(this.sessionRepository).should().findById(session.getId());
	}

	@Test
	void getSessionWithIdNotFound() {
		given(this.sessionRepository.findById("not-found")).willReturn(Mono.empty());
		StepVerifier.create(this.endpoint.getSession("not-found")).verifyComplete();
		then(this.sessionRepository).should().findById("not-found");
	}

	@Test
	void deleteSession() {
		given(this.sessionRepository.deleteById(session.getId())).willReturn(Mono.empty());
		StepVerifier.create(this.endpoint.deleteSession(session.getId())).verifyComplete();
		then(this.sessionRepository).should().deleteById(session.getId());
	}

}
