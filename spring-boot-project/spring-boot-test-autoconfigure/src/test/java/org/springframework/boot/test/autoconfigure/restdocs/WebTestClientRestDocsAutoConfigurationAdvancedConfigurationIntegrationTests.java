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

package org.springframework.boot.test.autoconfigure.restdocs;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.context.annotation.Bean;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.templates.TemplateFormats;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;

/**
 * Integration tests for {@link RestDocsAutoConfiguration} with {@link WebTestClient}.
 *
 * @author Eddú Meléndez
 */
@RunWith(SpringRunner.class)
@WebFluxTest
@WithMockUser
@AutoConfigureRestDocs(uriScheme = "https", uriHost = "api.example.com", uriPort = 443)
public class WebTestClientRestDocsAutoConfigurationAdvancedConfigurationIntegrationTests {

	@Autowired
	private WebTestClient webTestClient;

	private File generatedSnippets;

	@Before
	public void deleteSnippets() {
		this.generatedSnippets = new File(new BuildOutput(getClass()).getRootLocation(),
				"generated-snippets");
		FileSystemUtils.deleteRecursively(this.generatedSnippets);
	}

	@Test
	public void defaultSnippetsAreWritten() throws Exception {
		this.webTestClient.get().uri("/").exchange().expectStatus().is2xxSuccessful()
				.expectBody().consumeWith(document("default-snippets"));
		File defaultSnippetsDir = new File(this.generatedSnippets, "default-snippets");
		assertThat(defaultSnippetsDir).exists();
		assertThat(contentOf(new File(defaultSnippetsDir, "curl-request.md")))
				.contains("'https://api.example.com/'");
		assertThat(contentOf(new File(defaultSnippetsDir, "http-request.md")))
				.contains("api.example.com");
		assertThat(new File(defaultSnippetsDir, "http-response.md")).isFile();
		assertThat(new File(defaultSnippetsDir, "response-fields.md")).isFile();
	}

	@TestConfiguration
	public static class CustomizationConfiguration {

		@Bean
		public RestDocumentationResultHandler restDocumentation() {
			return MockMvcRestDocumentation.document("{method-name}");
		}

		@Bean
		public RestDocsWebTestClientConfigurationCustomizer templateFormatCustomizer() {
			return (configurer) -> configurer.snippets()
					.withTemplateFormat(TemplateFormats.markdown());
		}

		@Bean
		public RestDocsWebTestClientConfigurationCustomizer defaultSnippetsCustomizer() {
			return (configurer) -> configurer.snippets().withAdditionalDefaults(
					responseFields(fieldWithPath("_links.self").description("Main URL")));
		}

	}

}
