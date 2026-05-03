/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.resttestclient.autoconfigure;

import java.net.URI;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.boot.test.http.server.LocalTestWebServer.Scheme;
import org.springframework.boot.testsupport.classpath.resources.WithResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestRestTemplateTestAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class TestRestTemplateTestAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(TestRestTemplateTestAutoConfiguration.class));

	@Test
	void shouldFailTotRegisterTestRestTemplateWithoutWebServer() {
		this.contextRunner.run((context) -> assertThat(context).hasFailed()
			.getFailure()
			.hasMessageContaining(" No local test web server available"));
	}

	@Test
	@WithResource(name = "META-INF/spring.factories",
			content = """
					org.springframework.boot.test.http.server.LocalTestWebServer$Provider=\
					org.springframework.boot.resttestclient.autoconfigure.TestRestTemplateTestAutoConfigurationTests$TestLocalTestWebServerProvider
					""")
	void shouldDefineTestRestTemplateBoundToWebServer() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(TestRestTemplate.class)
				.hasBean("org.springframework.boot.resttestclient.TestRestTemplate");
			TestRestTemplate testRestTemplate = context.getBean(TestRestTemplate.class);
			assertThat(testRestTemplate.getRestTemplate().getUriTemplateHandler().expand("/"))
				.isEqualTo(URI.create("https://localhost:8182/"));
		});

	}

	@SuppressWarnings("unused")
	static class TestLocalTestWebServerProvider implements LocalTestWebServer.Provider {

		@Override
		public @Nullable LocalTestWebServer getLocalTestWebServer() {
			return LocalTestWebServer.of(Scheme.HTTPS, 8182);
		}

	}

}
