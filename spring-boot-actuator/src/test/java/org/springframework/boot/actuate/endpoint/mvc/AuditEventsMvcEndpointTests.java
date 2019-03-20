/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.InMemoryAuditEventRepository;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link AuditEventsMvcEndpoint}.
 *
 * @author Vedran Pavic
 */
@SpringBootTest
@RunWith(SpringRunner.class)
@TestPropertySource(properties = "management.security.enabled=false")
public class AuditEventsMvcEndpointTests {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@Before
	public void setUp() {
		this.context.getBean(AuditEventsMvcEndpoint.class).setEnabled(true);
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	public void contentTypeDefaultsToActuatorV1Json() throws Exception {
		this.mvc.perform(get("/auditevents")).andExpect(status().isOk())
				.andExpect(header().string("Content-Type",
						"application/vnd.spring-boot.actuator.v1+json;charset=UTF-8"));
	}

	@Test
	public void contentTypeCanBeApplicationJson() throws Exception {
		this.mvc.perform(get("/auditevents").header(HttpHeaders.ACCEPT,
				MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk())
				.andExpect(header().string("Content-Type",
						MediaType.APPLICATION_JSON_UTF8_VALUE));
	}

	@Test
	public void invokeWhenDisabledShouldReturnNotFoundStatus() throws Exception {
		this.context.getBean(AuditEventsMvcEndpoint.class).setEnabled(false);
		this.mvc.perform(get("/auditevents").param("after", "2016-11-01T10:00:00+0000"))
				.andExpect(status().isNotFound());
	}

	@Test
	public void invokeFilterByDateAfter() throws Exception {
		this.mvc.perform(get("/auditevents").param("after", "2016-11-01T13:00:00+0000"))
				.andExpect(status().isOk())
				.andExpect(content().string("{\"events\":[]}"));
	}

	@Test
	public void invokeFilterByPrincipalAndDateAfter() throws Exception {
		this.mvc.perform(get("/auditevents")
				.param("principal", "user").param("after", "2016-11-01T10:00:00+0000"))
				.andExpect(status().isOk())
				.andExpect(content().string(
						containsString("\"principal\":\"user\",\"type\":\"login\"")))
				.andExpect(content().string(not(containsString("admin"))));
	}

	@Test
	public void invokeFilterByPrincipalAndDateAfterAndType() throws Exception {
		this.mvc.perform(get("/auditevents").param("principal", "admin")
				.param("after", "2016-11-01T10:00:00+0000").param("type", "logout"))
				.andExpect(status().isOk())
				.andExpect(content().string(
						containsString("\"principal\":\"admin\",\"type\":\"logout\"")))
				.andExpect(content().string(not(containsString("login"))));
	}

	@Import({ JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			EndpointWebMvcAutoConfiguration.class, WebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class })
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

		private AuditEvent createEvent(String instant, String principal, String type) {
			return new AuditEvent(Date.from(Instant.parse(instant)), principal, type,
					Collections.<String, Object>emptyMap());
		}

	}

}
