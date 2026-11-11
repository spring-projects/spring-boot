/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.restdocs.test.autoconfigure;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.templates.TemplateFormats;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

/**
 * Integration tests for advanced configuration of
 * {@link AutoConfigureRestDocs @AutoConfigureRestDocs} with Mock MVC.
 *
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 */
@WebMvcTest(controllers = RestDocsTestController.class)
@WithMockUser
@AutoConfigureRestDocs
class MockMvcRestDocsAutoConfigurationAdvancedConfigurationIntegrationTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private RestDocumentationResultHandler documentationHandler;

	private File generatedSnippets;

	@BeforeEach
	void deleteSnippets() {
		this.generatedSnippets = new File(new BuildOutput(getClass()).getRootLocation(), "generated-snippets");
		FileSystemUtils.deleteRecursively(this.generatedSnippets);
	}

	@Test
	void snippetGeneration() {
		assertThat(this.mvc.get().uri("/")).apply(this.documentationHandler
			.document(links(linkWithRel("self").description("Canonical location of this resource"))));
		File defaultSnippetsDir = new File(this.generatedSnippets, "snippet-generation");
		assertThat(defaultSnippetsDir).exists();
		assertThat(contentOf(new File(defaultSnippetsDir, "curl-request.md"))).contains("'http://localhost:8080/'");
		assertThat(new File(defaultSnippetsDir, "links.md")).isFile();
		assertThat(new File(defaultSnippetsDir, "response-fields.md")).isFile();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class CustomizationConfiguration {

		@Bean
		RestDocumentationResultHandler restDocumentation() {
			return MockMvcRestDocumentation.document("{method-name}");
		}

		@Bean
		RestDocsMockMvcConfigurationCustomizer templateFormatCustomizer() {
			return (configurer) -> configurer.snippets().withTemplateFormat(TemplateFormats.markdown());
		}

		@Bean
		RestDocsMockMvcConfigurationCustomizer defaultSnippetsCustomizer() {
			return (configurer) -> configurer.snippets()
				.withAdditionalDefaults(responseFields(fieldWithPath("_links.self").description("Main URL")));
		}

	}

}
