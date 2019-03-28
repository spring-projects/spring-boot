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

package org.springframework.boot.cli.command.init;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InitializrService}
 *
 * @author Stephane Nicoll
 */
public class InitializrServiceTests extends AbstractHttpClientMockTests {

	private final InitializrService invoker = new InitializrService(this.http);

	@Test
	public void loadMetadata() throws Exception {
		mockSuccessfulMetadataGet(false);
		InitializrServiceMetadata metadata = this.invoker.loadMetadata("http://foo/bar");
		assertThat(metadata).isNotNull();
	}

	@Test
	public void generateSimpleProject() throws Exception {
		ProjectGenerationRequest request = new ProjectGenerationRequest();
		MockHttpProjectGenerationRequest mockHttpRequest = new MockHttpProjectGenerationRequest(
				"application/xml", "foo.zip");
		ProjectGenerationResponse entity = generateProject(request, mockHttpRequest);
		assertProjectEntity(entity, mockHttpRequest.contentType,
				mockHttpRequest.fileName);
	}

	@Test
	public void generateProjectCustomTargetFilename() throws Exception {
		ProjectGenerationRequest request = new ProjectGenerationRequest();
		request.setOutput("bar.zip");
		MockHttpProjectGenerationRequest mockHttpRequest = new MockHttpProjectGenerationRequest(
				"application/xml", null);
		ProjectGenerationResponse entity = generateProject(request, mockHttpRequest);
		assertProjectEntity(entity, mockHttpRequest.contentType, null);
	}

	@Test
	public void generateProjectNoDefaultFileName() throws Exception {
		ProjectGenerationRequest request = new ProjectGenerationRequest();
		MockHttpProjectGenerationRequest mockHttpRequest = new MockHttpProjectGenerationRequest(
				"application/xml", null);
		ProjectGenerationResponse entity = generateProject(request, mockHttpRequest);
		assertProjectEntity(entity, mockHttpRequest.contentType, null);
	}

	@Test
	public void generateProjectBadRequest() throws Exception {
		String jsonMessage = "Unknown dependency foo:bar";
		mockProjectGenerationError(400, jsonMessage);
		ProjectGenerationRequest request = new ProjectGenerationRequest();
		request.getDependencies().add("foo:bar");
		assertThatExceptionOfType(ReportableException.class)
				.isThrownBy(() -> this.invoker.generate(request))
				.withMessageContaining(jsonMessage);
	}

	@Test
	public void generateProjectBadRequestNoExtraMessage() throws Exception {
		mockProjectGenerationError(400, null);
		ProjectGenerationRequest request = new ProjectGenerationRequest();
		assertThatExceptionOfType(ReportableException.class)
				.isThrownBy(() -> this.invoker.generate(request))
				.withMessageContaining("unexpected 400 error");
	}

	@Test
	public void generateProjectNoContent() throws Exception {
		mockSuccessfulMetadataGet(false);
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		mockStatus(response, 500);
		given(this.http.execute(isA(HttpGet.class))).willReturn(response);
		ProjectGenerationRequest request = new ProjectGenerationRequest();
		assertThatExceptionOfType(ReportableException.class)
				.isThrownBy(() -> this.invoker.generate(request))
				.withMessageContaining("No content received from server");
	}

	@Test
	public void loadMetadataBadRequest() throws Exception {
		String jsonMessage = "whatever error on the server";
		mockMetadataGetError(500, jsonMessage);
		ProjectGenerationRequest request = new ProjectGenerationRequest();
		assertThatExceptionOfType(ReportableException.class)
				.isThrownBy(() -> this.invoker.generate(request))
				.withMessageContaining(jsonMessage);
	}

	@Test
	public void loadMetadataInvalidJson() throws Exception {
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		mockHttpEntity(response, "Foo-Bar-Not-JSON".getBytes(), "application/json");
		mockStatus(response, 200);
		given(this.http.execute(isA(HttpGet.class))).willReturn(response);
		ProjectGenerationRequest request = new ProjectGenerationRequest();
		assertThatExceptionOfType(ReportableException.class)
				.isThrownBy(() -> this.invoker.generate(request))
				.withMessageContaining("Invalid content received from server");
	}

	@Test
	public void loadMetadataNoContent() throws Exception {
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		mockStatus(response, 500);
		given(this.http.execute(isA(HttpGet.class))).willReturn(response);
		ProjectGenerationRequest request = new ProjectGenerationRequest();
		assertThatExceptionOfType(ReportableException.class)
				.isThrownBy(() -> this.invoker.generate(request))
				.withMessageContaining("No content received from server");
	}

	private ProjectGenerationResponse generateProject(ProjectGenerationRequest request,
			MockHttpProjectGenerationRequest mockRequest) throws Exception {
		mockSuccessfulProjectGeneration(mockRequest);
		ProjectGenerationResponse entity = this.invoker.generate(request);
		assertThat(entity.getContent()).as("wrong body content")
				.isEqualTo(mockRequest.content);
		return entity;
	}

	private static void assertProjectEntity(ProjectGenerationResponse entity,
			String mimeType, String fileName) {
		if (mimeType == null) {
			assertThat(entity.getContentType()).isNull();
		}
		else {
			assertThat(entity.getContentType().getMimeType()).isEqualTo(mimeType);
		}
		assertThat(entity.getFileName()).isEqualTo(fileName);
	}

}
