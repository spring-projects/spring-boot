/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.List;

import org.junit.Test;

import org.springframework.boot.actuate.session.SessionsEndpoint.SessionDescriptor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;
import org.springframework.session.Session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SessionsEndpoint}.
 *
 * @author Vedran Pavic
 */
public class SessionsEndpointTests {

	private static final Session session = new MapSession();

	@SuppressWarnings("unchecked")
	private final FindByIndexNameSessionRepository<Session> repository = mock(FindByIndexNameSessionRepository.class);

	private final SessionsEndpoint endpoint = new SessionsEndpoint(this.repository);

	@Test
	public void sessionsForUsername() {
		given(this.repository.findByIndexNameAndIndexValue(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
				"user")).willReturn(Collections.singletonMap(session.getId(), session));
		List<SessionDescriptor> result = this.endpoint.sessionsForUsername("user").getSessions();
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getId()).isEqualTo(session.getId());
		assertThat(result.get(0).getAttributeNames()).isEqualTo(session.getAttributeNames());
		assertThat(result.get(0).getCreationTime()).isEqualTo(session.getCreationTime());
		assertThat(result.get(0).getLastAccessedTime()).isEqualTo(session.getLastAccessedTime());
		assertThat(result.get(0).getMaxInactiveInterval()).isEqualTo(session.getMaxInactiveInterval().getSeconds());
		assertThat(result.get(0).isExpired()).isEqualTo(session.isExpired());
	}

	@Test
	public void getSession() {
		given(this.repository.findById(session.getId())).willReturn(session);
		SessionDescriptor result = this.endpoint.getSession(session.getId());
		assertThat(result.getId()).isEqualTo(session.getId());
		assertThat(result.getAttributeNames()).isEqualTo(session.getAttributeNames());
		assertThat(result.getCreationTime()).isEqualTo(session.getCreationTime());
		assertThat(result.getLastAccessedTime()).isEqualTo(session.getLastAccessedTime());
		assertThat(result.getMaxInactiveInterval()).isEqualTo(session.getMaxInactiveInterval().getSeconds());
		assertThat(result.isExpired()).isEqualTo(session.isExpired());
	}

	@Test
	public void getSessionWithIdNotFound() {
		given(this.repository.findById("not-found")).willReturn(null);
		assertThat(this.endpoint.getSession("not-found")).isNull();
	}

	@Test
	public void deleteSession() {
		this.endpoint.deleteSession(session.getId());
		verify(this.repository).deleteById(session.getId());
	}

}
