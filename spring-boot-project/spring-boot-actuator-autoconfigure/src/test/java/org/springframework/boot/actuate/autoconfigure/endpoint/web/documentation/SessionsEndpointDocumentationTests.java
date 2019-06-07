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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import org.springframework.boot.actuate.context.ShutdownEndpoint;
import org.springframework.boot.actuate.session.SessionsEndpoint;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing the {@link ShutdownEndpoint}.
 *
 * @author Andy Wilkinson
 */
@TestPropertySource(properties = "spring.jackson.serialization.write-dates-as-timestamps=false")
public class SessionsEndpointDocumentationTests extends MockMvcEndpointDocumentationTests {

	private static final Session sessionOne = createSession(Instant.now().minusSeconds(60 * 60 * 12),
			Instant.now().minusSeconds(45));

	private static final Session sessionTwo = createSession("4db5efcc-99cb-4d05-a52c-b49acfbb7ea9",
			Instant.now().minusSeconds(60 * 60 * 5), Instant.now().minusSeconds(37));

	private static final Session sessionThree = createSession(Instant.now().minusSeconds(60 * 60 * 2),
			Instant.now().minusSeconds(12));

	private static final List<FieldDescriptor> sessionFields = Arrays.asList(
			fieldWithPath("id").description("ID of the session."),
			fieldWithPath("attributeNames").description("Names of the attributes stored in the session."),
			fieldWithPath("creationTime").description("Timestamp of when the session was created."),
			fieldWithPath("lastAccessedTime").description("Timestamp of when the session was last accessed."),
			fieldWithPath("maxInactiveInterval").description(
					"Maximum permitted period of inactivity, in seconds, " + "before the session will expire."),
			fieldWithPath("expired").description("Whether the session has expired."));

	@MockBean
	private FindByIndexNameSessionRepository<Session> sessionRepository;

	@Test
	public void sessionsForUsername() throws Exception {
		Map<String, Session> sessions = new HashMap<>();
		sessions.put(sessionOne.getId(), sessionOne);
		sessions.put(sessionTwo.getId(), sessionTwo);
		sessions.put(sessionThree.getId(), sessionThree);
		given(this.sessionRepository
				.findByIndexNameAndIndexValue(FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME, "alice"))
						.willReturn(sessions);
		this.mockMvc.perform(get("/actuator/sessions").param("username", "alice")).andExpect(status().isOk())
				.andDo(document("sessions/username",
						responseFields(fieldWithPath("sessions").description("Sessions for the given username."))
								.andWithPrefix("sessions.[].", sessionFields),
						requestParameters(parameterWithName("username").description("Name of the user."))));
	}

	@Test
	public void sessionWithId() throws Exception {
		Map<String, Session> sessions = new HashMap<>();
		sessions.put(sessionOne.getId(), sessionOne);
		sessions.put(sessionTwo.getId(), sessionTwo);
		sessions.put(sessionThree.getId(), sessionThree);
		given(this.sessionRepository.findById(sessionTwo.getId())).willReturn(sessionTwo);
		this.mockMvc.perform(get("/actuator/sessions/{id}", sessionTwo.getId())).andExpect(status().isOk())
				.andDo(document("sessions/id", responseFields(sessionFields)));
	}

	@Test
	public void deleteASession() throws Exception {
		this.mockMvc.perform(delete("/actuator/sessions/{id}", sessionTwo.getId())).andExpect(status().isNoContent())
				.andDo(document("sessions/delete"));
		verify(this.sessionRepository).deleteById(sessionTwo.getId());
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

	@Configuration
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		public SessionsEndpoint endpoint(FindByIndexNameSessionRepository<?> sessionRepository) {
			return new SessionsEndpoint(sessionRepository);
		}

	}

}
