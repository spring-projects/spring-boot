/*
 * Copyright 2012-2017 the original author or authors.
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

import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Tests for {@link RestDocsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@WebMvcTest
@AutoConfigureRestDocs(uriScheme = "https", uriHost = "api.example.com", uriPort = 443)
public class RestDocsAutoConfigurationIntegrationTests {

	@Before
	public void deleteSnippets() {
		FileSystemUtils.deleteRecursively(new File("target/generated-snippets"));
	}

	@Autowired
	private MockMvc mvc;

	@Test
	public void defaultSnippetsAreWritten() throws Exception {
		this.mvc.perform(get("/")).andDo(document("default-snippets"));
		File defaultSnippetsDir = new File("target/generated-snippets/default-snippets");
		assertThat(defaultSnippetsDir).exists();
		assertThat(new File(defaultSnippetsDir, "curl-request.adoc"))
				.has(contentContaining("'https://api.example.com/'"));
		assertThat(new File(defaultSnippetsDir, "http-request.adoc"))
				.has(contentContaining("api.example.com"));
		assertThat(new File(defaultSnippetsDir, "http-response.adoc")).isFile();
	}

	private Condition<File> contentContaining(String toContain) {
		return new ContentContainingCondition(toContain);
	}

}
