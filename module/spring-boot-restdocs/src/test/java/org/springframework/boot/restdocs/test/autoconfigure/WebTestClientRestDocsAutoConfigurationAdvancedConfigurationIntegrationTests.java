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
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.templates.TemplateFormats;
import org.springframework.security.test.context.support.WithMockUser;
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
@WebFluxTest
@WithMockUser
@AutoConfigureRestDocs(uriScheme = "https", uriHost = "api.example.com", uriPort = 443)
class WebTestClientRestDocsAutoConfigurationAdvancedConfigurationIntegrationTests {

	@Autowired
	private WebTestClient webTestClient;

	private File generatedSnippets;

	@BeforeEach
	void deleteSnippets() {
		this.generatedSnippets = new File(new BuildOutput(getClass()).getRootLocation(), "generated-snippets");
		FileSystemUtils.deleteRecursively(this.generatedSnippets);
	}

	@Test
	void defaultSnippetsAreWritten() {
		this.webTestClient.get()
			.uri("/")
			.exchange()
			.expectStatus()
			.is2xxSuccessful()
			.expectBody()
			.consumeWith(document("default-snippets"));
		File defaultSnippetsDir = new File(this.generatedSnippets, "default-snippets");
		assertThat(defaultSnippetsDir).exists();
		assertThat(contentOf(new File(defaultSnippetsDir, "curl-request.md"))).contains("'https://api.example.com/'");
		assertThat(contentOf(new File(defaultSnippetsDir, "http-request.md"))).contains("api.example.com");
		assertThat(new File(defaultSnippetsDir, "http-response.md")).isFile();
		assertThat(new File(defaultSnippetsDir, "response-fields.md")).isFile();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class CustomizationConfiguration {

		@Bean
		RestDocumentationResultHandler restDocumentation() {
			return MockMvcRestDocumentation.document("{method-name}");
		}

		@Bean
		RestDocsWebTestClientConfigurationCustomizer templateFormatCustomizer() {
			return (configurer) -> configurer.snippets().withTemplateFormat(TemplateFormats.markdown());
		}

		@Bean
		RestDocsWebTestClientConfigurationCustomizer defaultSnippetsCustomizer() {
			return (configurer) -> configurer.snippets()
				.withAdditionalDefaults(responseFields(fieldWithPath("_links.self").description("Main URL")));
		}

	}

}
