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

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.session.SessionsDescriptor.SessionDescriptor;
import org.springframework.session.ReactiveFindByIndexNameSessionRepository;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.Session;
import org.springframework.util.Assert;

/**
 * {@link Endpoint @Endpoint} to expose information about HTTP {@link Session}s on a
 * reactive stack.
 *
 * @author Vedran Pavic
 * @author Moritz Halbritter
 * @since 3.3.0
 */
@Endpoint(id = "sessions")
public class ReactiveSessionsEndpoint {

	private final ReactiveSessionRepository<? extends Session> sessionRepository;

	private final ReactiveFindByIndexNameSessionRepository<? extends Session> indexedSessionRepository;

	/**
	 * Create a new {@link ReactiveSessionsEndpoint} instance.
	 * @param sessionRepository the session repository
	 * @param indexedSessionRepository the indexed session repository
	 */
	public ReactiveSessionsEndpoint(ReactiveSessionRepository<? extends Session> sessionRepository,
			ReactiveFindByIndexNameSessionRepository<? extends Session> indexedSessionRepository) {
		Assert.notNull(sessionRepository, "'sessionRepository' must not be null");
		this.sessionRepository = sessionRepository;
		this.indexedSessionRepository = indexedSessionRepository;
	}

	@ReadOperation
	public Mono<SessionsDescriptor> sessionsForUsername(String username) {
		if (this.indexedSessionRepository == null) {
			return Mono.empty();
		}
		return this.indexedSessionRepository.findByPrincipalName(username).map(SessionsDescriptor::new);
	}

	@ReadOperation
	public Mono<SessionDescriptor> getSession(@Selector String sessionId) {
		return this.sessionRepository.findById(sessionId).map(SessionDescriptor::new);
	}

	@DeleteOperation
	public Mono<Void> deleteSession(@Selector String sessionId) {
		return this.sessionRepository.deleteById(sessionId);
	}

}
