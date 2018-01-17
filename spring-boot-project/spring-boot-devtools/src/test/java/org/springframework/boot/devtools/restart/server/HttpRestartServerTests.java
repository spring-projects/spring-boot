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

package org.springframework.boot.devtools.restart.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link HttpRestartServer}.
 *
 * @author Phillip Webb
 */
public class HttpRestartServerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private RestartServer delegate;

	private HttpRestartServer server;

	@Captor
	private ArgumentCaptor<ClassLoaderFiles> filesCaptor;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.server = new HttpRestartServer(this.delegate);
	}

	@Test
	public void sourceFolderUrlFilterMustNotBeNull() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("SourceFolderUrlFilter must not be null");
		new HttpRestartServer((SourceFolderUrlFilter) null);
	}

	@Test
	public void restartServerMustNotBeNull() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("RestartServer must not be null");
		new HttpRestartServer((RestartServer) null);
	}

	@Test
	public void sendClassLoaderFiles() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		ClassLoaderFiles files = new ClassLoaderFiles();
		files.addFile("name", new ClassLoaderFile(Kind.ADDED, new byte[0]));
		byte[] bytes = serialize(files);
		request.setContent(bytes);
		this.server.handle(new ServletServerHttpRequest(request),
				new ServletServerHttpResponse(response));
		verify(this.delegate).updateAndRestart(this.filesCaptor.capture());
		assertThat(this.filesCaptor.getValue().getFile("name")).isNotNull();
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	public void sendNoContent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.server.handle(new ServletServerHttpRequest(request),
				new ServletServerHttpResponse(response));
		verifyZeroInteractions(this.delegate);
		assertThat(response.getStatus()).isEqualTo(500);

	}

	@Test
	public void sendBadData() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setContent(new byte[] { 0, 0, 0 });
		this.server.handle(new ServletServerHttpRequest(request),
				new ServletServerHttpResponse(response));
		verifyZeroInteractions(this.delegate);
		assertThat(response.getStatus()).isEqualTo(500);
	}

	private byte[] serialize(Object object) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(object);
		oos.close();
		return bos.toByteArray();
	}

}
