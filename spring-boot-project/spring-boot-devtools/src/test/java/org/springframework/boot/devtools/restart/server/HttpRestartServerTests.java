/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.devtools.restart.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link HttpRestartServer}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class HttpRestartServerTests {

	@Mock
	private RestartServer delegate;

	private HttpRestartServer server;

	@Captor
	private ArgumentCaptor<ClassLoaderFiles> filesCaptor;

	@BeforeEach
	void setup() {
		this.server = new HttpRestartServer(this.delegate);
	}

	@Test
	void sourceDirectoryUrlFilterMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new HttpRestartServer((SourceDirectoryUrlFilter) null))
				.withMessageContaining("SourceDirectoryUrlFilter must not be null");
	}

	@Test
	void restartServerMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new HttpRestartServer((RestartServer) null))
				.withMessageContaining("RestartServer must not be null");
	}

	@Test
	void sendClassLoaderFiles() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		ClassLoaderFiles files = new ClassLoaderFiles();
		files.addFile("name", new ClassLoaderFile(Kind.ADDED, new byte[0]));
		byte[] bytes = serialize(files);
		request.setContent(bytes);
		this.server.handle(new ServletServerHttpRequest(request), new ServletServerHttpResponse(response));
		verify(this.delegate).updateAndRestart(this.filesCaptor.capture());
		assertThat(this.filesCaptor.getValue().getFile("name")).isNotNull();
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void sendNoContent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		this.server.handle(new ServletServerHttpRequest(request), new ServletServerHttpResponse(response));
		verifyNoInteractions(this.delegate);
		assertThat(response.getStatus()).isEqualTo(500);

	}

	@Test
	void sendBadData() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setContent(new byte[] { 0, 0, 0 });
		this.server.handle(new ServletServerHttpRequest(request), new ServletServerHttpResponse(response));
		verifyNoInteractions(this.delegate);
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
