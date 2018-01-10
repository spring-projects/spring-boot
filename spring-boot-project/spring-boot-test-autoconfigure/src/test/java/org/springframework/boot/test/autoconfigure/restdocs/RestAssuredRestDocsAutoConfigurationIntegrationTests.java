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

import io.restassured.specification.RequestSpecification;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.FileSystemUtils;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.restassured3.RestAssuredRestDocumentation.document;
import static org.springframework.restdocs.restassured3.operation.preprocess.RestAssuredPreprocessors.modifyUris;

/**
 * Integration tests for {@link RestDocsAutoConfiguration} with REST Assured.
 *
 * @author Eddú Meléndez
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestDocs
public class RestAssuredRestDocsAutoConfigurationIntegrationTests {

	@LocalServerPort
	private int port;

	@Before
	public void deleteSnippets() {
		FileSystemUtils.deleteRecursively(new File("target/generated-snippets"));
	}

	@Autowired
	private RequestSpecification documentationSpec;

	@Test
	public void defaultSnippetsAreWritten() {
		given(this.documentationSpec)
				.filter(document("default-snippets",
						preprocessRequest(modifyUris().scheme("https")
								.host("api.example.com").removePort())))
				.when().port(this.port).get("/").then().assertThat().statusCode(is(200));
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
