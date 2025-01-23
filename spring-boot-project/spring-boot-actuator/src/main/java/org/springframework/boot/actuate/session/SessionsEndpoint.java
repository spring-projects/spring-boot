/*
 * Copyright 2012-2025 the original author or authors.
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

import java.util.Map;

import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.session.SessionsDescriptor.SessionDescriptor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.util.Assert;

/**
 * {@link Endpoint @Endpoint} to expose information about HTTP {@link Session}s on a
 * Servlet stack.
 *
 * @author Vedran Pavic
 * @since 2.0.0
 */
@Endpoint(id = "sessions")
public class SessionsEndpoint {

	private final SessionRepository<? extends Session> sessionRepository;

	private final FindByIndexNameSessionRepository<? extends Session> indexedSessionRepository;

	/**
	 * Create a new {@link SessionsEndpoint} instance.
	 * @param sessionRepository the session repository
	 * @param indexedSessionRepository the indexed session repository
	 * @since 3.3.0
	 */
	public SessionsEndpoint(SessionRepository<? extends Session> sessionRepository,
			FindByIndexNameSessionRepository<? extends Session> indexedSessionRepository) {
		Assert.notNull(sessionRepository, "'sessionRepository' must not be null");
		this.sessionRepository = sessionRepository;
		this.indexedSessionRepository = indexedSessionRepository;
	}

	@ReadOperation
	public SessionsDescriptor sessionsForUsername(String username) {
		if (this.indexedSessionRepository == null) {
			return null;
		}
		Map<String, ? extends Session> sessions = this.indexedSessionRepository.findByPrincipalName(username);
		return new SessionsDescriptor(sessions);
	}

	@ReadOperation
	public SessionDescriptor getSession(@Selector String sessionId) {
		Session session = this.sessionRepository.findById(sessionId);
		if (session == null) {
			return null;
		}
		return new SessionDescriptor(session);
	}

	@DeleteOperation
	public void deleteSession(@Selector String sessionId) {
		this.sessionRepository.deleteById(sessionId);
	}

}
