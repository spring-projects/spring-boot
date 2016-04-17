/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.MockEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.session.ExpiringSession;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link SessionMvcEndpoint}
 *
 * @author Eddú Meléndez
 */
public class SessionMvcEndpointTests {

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@Before
	public void setUp() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void endpointNotRegistered() throws Exception {
		loadConfig();
		assertThat(this.context.getBeanNamesForType(SessionMvcEndpoint.class)).hasSize(0);
	}

	@Test
	public void endpointRegistered() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "endpoints.session.enabled=true");
		loadConfig();
		assertThat(this.context.getBeanNamesForType(SessionMvcEndpoint.class)).hasSize(1);
	}

	@Test
	public void searchEndpointAvailable() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "endpoints.session.enabled=true");
		loadConfig();
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
		mockMvc.perform((get("/session/?username=springuser")))
				.andExpect(status().isOk());
	}

	@Test
	public void customizePath() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "endpoints.session.enabled=true",
				"endpoints.session.path=/mysession");
		loadConfig();
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
		mockMvc.perform((get("/mysession/?username=springuser")))
				.andExpect(status().isOk());
	}

	@Test
	public void deleteSession() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "endpoints.session.enabled=true");
		loadConfig();
		FindByIndexNameSessionRepository sessionRepository = this.context.getBean(FindByIndexNameSessionRepository.class);
		given(sessionRepository.getSession("123")).willReturn(mock(ExpiringSession.class));
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
		mockMvc.perform(delete("/session/123"))
				.andExpect(status().isOk());
	}

	@Test
	public void deleteSessionDoNotExist() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "endpoints.session.enabled=true");
		loadConfig();
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
		mockMvc.perform(delete("/session/12345"))
				.andExpect(status().isNotFound());
	}

	private void loadConfig() {
		this.context.register(Config.class, SessionRepositoryConfig.class,
				EndpointAutoConfiguration.class,
				WebMvcAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
	}

	@Configuration
	@EnableConfigurationProperties
	@EnableWebMvc
	@Import({ EndpointWebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class })
	public static class SessionRepositoryConfig {

		@Bean
		public FindByIndexNameSessionRepository findByIndexNameSessionRepository() {
			return mock(FindByIndexNameSessionRepository.class);
		}

	}

	@Configuration
	@EnableConfigurationProperties
	protected static class Config {

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			return new MockEmbeddedServletContainerFactory();
		}

		@Bean
		public EmbeddedServletContainerCustomizerBeanPostProcessor embeddedServletContainerCustomizerBeanPostProcessor() {
			return new EmbeddedServletContainerCustomizerBeanPostProcessor();
		}

	}

}
