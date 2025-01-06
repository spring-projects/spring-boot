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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.context.ShutdownEndpoint;
import org.springframework.boot.actuate.session.SessionsEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;

/**
 * Tests for generating documentation describing the {@link ShutdownEndpoint}.
 *
 * @author Andy Wilkinson
 */
@TestPropertySource(properties = "spring.jackson.serialization.write-dates-as-timestamps=false")
class SessionsEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	private static final Session sessionOne = createSession(Instant.now().minusSeconds(60 * 60 * 12),
			Instant.now().minusSeconds(45));

	private static final Session sessionTwo = createSession("4db5efcc-99cb-4d05-a52c-b49acfbb7ea9",
			Instant.now().minusSeconds(60 * 60 * 5), Instant.now().minusSeconds(37));

	private static final Session sessionThree = createSession(Instant.now().minusSeconds(60 * 60 * 2),
			Instant.now().minusSeconds(12));

	private static final List<FieldDescriptor> sessionFields = List.of(
			fieldWithPath("id").description("ID of the session."),
			fieldWithPath("attributeNames").description("Names of the attributes stored in the session."),
			fieldWithPath("creationTime").description("Timestamp of when the session was created."),
			fieldWithPath("lastAccessedTime").description("Timestamp of when the session was last accessed."),
			fieldWithPath("maxInactiveInterval")
				.description("Maximum permitted period of inactivity, in seconds, before the session will expire."),
			fieldWithPath("expired").description("Whether the session has expired."));

	@MockitoBean
	private FindByIndexNameSessionRepository<Session> sessionRepository;

	@Test
	void sessionsForUsername() {
		Map<String, Session> sessions = new HashMap<>();
		sessions.put(sessionOne.getId(), sessionOne);
		sessions.put(sessionTwo.getId(), sessionTwo);
		sessions.put(sessionThree.getId(), sessionThree);
		given(this.sessionRepository.findByPrincipalName("alice")).willReturn(sessions);
		assertThat(this.mvc.get().uri("/actuator/sessions").param("username", "alice")).hasStatusOk()
			.apply(document("sessions/username",
					responseFields(fieldWithPath("sessions").description("Sessions for the given username."))
						.andWithPrefix("sessions.[].", sessionFields),
					queryParameters(parameterWithName("username").description("Name of the user."))));
	}

	@Test
	void sessionWithId() {
		given(this.sessionRepository.findById(sessionTwo.getId())).willReturn(sessionTwo);
		assertThat(this.mvc.get().uri("/actuator/sessions/{id}", sessionTwo.getId())).hasStatusOk()
			.apply(document("sessions/id", responseFields(sessionFields)));
	}

	@Test
	void deleteASession() {
		assertThat(this.mvc.delete().uri("/actuator/sessions/{id}", sessionTwo.getId()))
			.hasStatus(HttpStatus.NO_CONTENT)
			.apply(document("sessions/delete"));
		then(this.sessionRepository).should().deleteById(sessionTwo.getId());
	}

	private static MapSession createSession(Instant creationTime, Instant lastAccessedTime) {
		return createSession(UUID.randomUUID().toString(), creationTime, lastAccessedTime);
	}

	private static MapSession createSession(String id, Instant creationTime, Instant lastAccessedTime) {
		MapSession session = new MapSession(id);
		session.setCreationTime(creationTime);
		session.setLastAccessedTime(lastAccessedTime);
		return session;
	}

	@Configuration(proxyBeanMethods = false)
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		SessionsEndpoint endpoint(FindByIndexNameSessionRepository<?> sessionRepository) {
			return new SessionsEndpoint(sessionRepository, sessionRepository);
		}

	}

}
