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

package org.springframework.boot.test.autoconfigure.restdocs;

import java.io.File;

import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.templates.TemplateFormats;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Integration tests for advanced configuration of {@link AutoConfigureRestDocs} with Mock
 * MVC.
 *
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = RestDocsTestController.class, secure = false)
@AutoConfigureRestDocs
public class MockMvcRestDocsAutoConfigurationAdvancedConfigurationIntegrationTests {

	@Before
	public void deleteSnippets() {
		FileSystemUtils.deleteRecursively(new File("target/generated-snippets"));
	}

	@Autowired
	private MockMvc mvc;

	@Autowired
	private RestDocumentationResultHandler documentationHandler;

	@Test
	public void snippetGeneration() throws Exception {
		this.mvc.perform(get("/")).andDo(this.documentationHandler
				.document(links(linkWithRel("self").description("Canonical location of this resource"))));
		File defaultSnippetsDir = new File("target/generated-snippets/snippet-generation");
		assertThat(defaultSnippetsDir).exists();
		assertThat(new File(defaultSnippetsDir, "curl-request.md")).has(contentContaining("'http://localhost:8080/'"));
		assertThat(new File(defaultSnippetsDir, "links.md")).isFile();
	}

	private Condition<File> contentContaining(String toContain) {
		return new ContentContainingCondition(toContain);
	}

	@TestConfiguration
	public static class CustomizationConfiguration implements RestDocsMockMvcConfigurationCustomizer {

		@Bean
		public RestDocumentationResultHandler restDocumentation() {
			return MockMvcRestDocumentation.document("{method-name}");
		}

		@Override
		public void customize(MockMvcRestDocumentationConfigurer configurer) {
			configurer.snippets().withTemplateFormat(TemplateFormats.markdown());
		}

	}

}
