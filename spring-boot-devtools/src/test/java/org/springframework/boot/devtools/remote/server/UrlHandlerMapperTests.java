/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.devtools.remote.server;

import javax.servlet.http.HttpServletRequest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UrlHandlerMapper}.
 *
 * @author Rob Winch
 * @author Phillip Webb
 */
public class UrlHandlerMapperTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Handler handler = mock(Handler.class);

	@Test
	public void requestUriMustNotBeNull() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("URL must not be empty");
		new UrlHandlerMapper(null, this.handler);
	}

	@Test
	public void requestUriMustNotBeEmpty() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("URL must not be empty");
		new UrlHandlerMapper("", this.handler);
	}

	@Test
	public void requestUrlMustStartWithSlash() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("URL must start with '/'");
		new UrlHandlerMapper("tunnel", this.handler);
	}

	@Test
	public void handlesMatchedUrl() throws Exception {
		UrlHandlerMapper mapper = new UrlHandlerMapper("/tunnel", this.handler);
		HttpServletRequest servletRequest = new MockHttpServletRequest("GET", "/tunnel");
		ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
		assertThat(mapper.getHandler(request)).isEqualTo(this.handler);
	}

	@Test
	public void ignoresDifferentUrl() throws Exception {
		UrlHandlerMapper mapper = new UrlHandlerMapper("/tunnel", this.handler);
		HttpServletRequest servletRequest = new MockHttpServletRequest("GET",
				"/tunnel/other");
		ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
		assertThat(mapper.getHandler(request)).isNull();
	}

}
