/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.AuditEventsEndpoint;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for generating documentation describing {@link AuditEventsEndpoint}.
 *
 * @author Andy Wilkinson
 */
public class AuditEventsEndpointDocumentationTests
		extends MockMvcEndpointDocumentationTests {

	@MockBean
	private AuditEventRepository repository;

	@Test
	public void allAuditEvents() throws Exception {
		String queryTimestamp = "2017-11-07T09:37Z";
		given(this.repository.find(any(), any(), any())).willReturn(
				Arrays.asList(new AuditEvent("alice", "logout", Collections.emptyMap())));
		this.mockMvc.perform(get("/actuator/auditevents").param("after", queryTimestamp))
				.andExpect(status().isOk())
				.andDo(document("auditevents/all", responseFields(
						fieldWithPath("events").description("An array of audit events."),
						fieldWithPath("events.[].timestamp")
								.description("The timestamp of when the event occurred."),
						fieldWithPath("events.[].principal")
								.description("The principal that triggered the event."),
						fieldWithPath("events.[].type")
								.description("The type of the event."))));
	}

	@Test
	public void filteredAuditEvents() throws Exception {
		OffsetDateTime now = OffsetDateTime.now();
		String queryTimestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now);
		given(this.repository.find("alice", now.toInstant(), "logout")).willReturn(
				Arrays.asList(new AuditEvent("alice", "logout", Collections.emptyMap())));
		this.mockMvc
				.perform(get("/actuator/auditevents").param("principal", "alice")
						.param("after", queryTimestamp).param("type", "logout"))
				.andExpect(status().isOk())
				.andDo(document("auditevents/filtered",
						requestParameters(
								parameterWithName("after").description(
										"Restricts the events to those that occurred "
												+ "after the given time. Optional."),
								parameterWithName("principal").description(
										"Restricts the events to those with the given "
												+ "principal. Optional."),
								parameterWithName("type").description(
										"Restricts the events to those with the given "
												+ "type. Optional."))));
		verify(this.repository).find("alice", now.toInstant(), "logout");
	}

	@Configuration
	@Import(BaseDocumentationConfiguration.class)
	static class TestConfiguration {

		@Bean
		public AuditEventsEndpoint auditEventsEndpoint(AuditEventRepository repository) {
			return new AuditEventsEndpoint(repository);
		}

	}

}
