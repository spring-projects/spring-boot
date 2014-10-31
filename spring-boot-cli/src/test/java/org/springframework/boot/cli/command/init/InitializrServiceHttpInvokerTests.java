/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.cli.command.init;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link InitializrServiceHttpInvoker}
 *
 * @author Stephane Nicoll
 */
public class InitializrServiceHttpInvokerTests extends AbstractHttpClientMockTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final InitializrServiceHttpInvoker invoker = new InitializrServiceHttpInvoker(
			this.httpClient);

	@Test
	public void loadMetadata() throws IOException {
		mockSuccessfulMetadataGet();
		InitializrServiceMetadata metadata = this.invoker.loadMetadata("http://foo/bar");
		assertNotNull(metadata);
	}

	@Test
	public void generateSimpleProject() throws IOException {
		ProjectGenerationRequest request = new ProjectGenerationRequest();
		MockHttpProjectGenerationRequest mockHttpRequest = new MockHttpProjectGenerationRequest(
				"application/xml", "foo.zip");
		ProjectGenerationResponse entity = generateProject(request, mockHttpRequest);
		assertProjectEntity(entity, mockHttpRequest.contentType, mockHttpRequest.fileName);
	}

	@Test
	public void generateProjectCustomTargetFilename() throws IOException {
		ProjectGenerationRequest request = new ProjectGenerationRequest();
		request.setOutput("bar.zip");
		MockHttpProjectGenerationRequest mockHttpRequest = new MockHttpProjectGenerationRequest(
				"application/xml", null);
		ProjectGenerationResponse entity = generateProject(request, mockHttpRequest);
		assertProjectEntity(entity, mockHttpRequest.contentType, null);
	}

	@Test
	public void generateProjectNoDefaultFileName() throws IOException {
		ProjectGenerationRequest request = new ProjectGenerationRequest();
		MockHttpProjectGenerationRequest mockHttpRequest = new MockHttpProjectGenerationRequest(
				"application/xml", null);
		ProjectGenerationResponse entity = generateProject(request, mockHttpRequest);
		assertProjectEntity(entity, mockHttpRequest.contentType, null);
	}

	@Test
	public void generateProjectBadRequest() throws IOException {
		String jsonMessage = "Unknown dependency foo:bar";
		mockProjectGenerationError(400, jsonMessage);
		ProjectGenerationRequest request = new ProjectGenerationRequest();
		request.getDependencies().add("foo:bar");

		this.thrown.expect(ProjectGenerationException.class);
		this.thrown.expectMessage(jsonMessage);
		this.invoker.generate(request);
	}

	@Test
	public void generateProjectBadRequestNoExtraMessage() throws IOException {
		mockProjectGenerationError(400, null);

		ProjectGenerationRequest request = new ProjectGenerationRequest();
		this.thrown.expect(ProjectGenerationException.class);
		this.thrown.expectMessage("unexpected 400 error");
		this.invoker.generate(request);
	}

	@Test
	public void generateProjectNoContent() throws IOException {
		mockSuccessfulMetadataGet();

		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		mockStatus(response, 500);
		when(this.httpClient.execute(isA(HttpGet.class))).thenReturn(response);

		ProjectGenerationRequest request = new ProjectGenerationRequest();

		this.thrown.expect(ProjectGenerationException.class);
		this.thrown.expectMessage("No content received from server");
		this.invoker.generate(request);
	}

	@Test
	public void loadMetadataBadRequest() throws IOException {
		String jsonMessage = "whatever error on the server";
		mockMetadataGetError(500, jsonMessage);
		ProjectGenerationRequest request = new ProjectGenerationRequest();

		this.thrown.expect(ProjectGenerationException.class);
		this.thrown.expectMessage(jsonMessage);
		this.invoker.generate(request);
	}

	@Test
	public void loadMetadataInvalidJson() throws IOException {
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		mockHttpEntity(response, "Foo-Bar-Not-JSON".getBytes(), "application/json");
		mockStatus(response, 200);
		when(this.httpClient.execute(isA(HttpGet.class))).thenReturn(response);

		ProjectGenerationRequest request = new ProjectGenerationRequest();
		this.thrown.expect(ProjectGenerationException.class);
		this.thrown.expectMessage("Invalid content received from server");
		this.invoker.generate(request);
	}

	@Test
	public void loadMetadataNoContent() throws IOException {
		CloseableHttpResponse response = mock(CloseableHttpResponse.class);
		mockStatus(response, 500);
		when(this.httpClient.execute(isA(HttpGet.class))).thenReturn(response);

		ProjectGenerationRequest request = new ProjectGenerationRequest();

		this.thrown.expect(ProjectGenerationException.class);
		this.thrown.expectMessage("No content received from server");
		this.invoker.generate(request);
	}

	private ProjectGenerationResponse generateProject(ProjectGenerationRequest request,
			MockHttpProjectGenerationRequest mockRequest) throws IOException {
		mockSuccessfulProjectGeneration(mockRequest);
		ProjectGenerationResponse entity = this.invoker.generate(request);
		assertArrayEquals("wrong body content", mockRequest.content, entity.getContent());
		return entity;
	}

	private static void assertProjectEntity(ProjectGenerationResponse entity,
			String mimeType, String fileName) {
		if (mimeType == null) {
			assertNull("No content type expected", entity.getContentType());
		}
		else {
			assertEquals("wrong mime type", mimeType, entity.getContentType()
					.getMimeType());
		}
		assertEquals("wrong filename", fileName, entity.getFileName());
	}

}
