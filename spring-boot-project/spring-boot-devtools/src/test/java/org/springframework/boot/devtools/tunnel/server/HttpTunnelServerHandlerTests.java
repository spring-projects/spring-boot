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

package org.springframework.boot.devtools.tunnel.server;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link HttpTunnelServerHandler}.
 *
 * @author Phillip Webb
 */
public class HttpTunnelServerHandlerTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void serverMustNotBeNull() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Server must not be null");
		new HttpTunnelServerHandler(null);
	}

	@Test
	public void handleDelegatesToServer() throws Exception {
		HttpTunnelServer server = mock(HttpTunnelServer.class);
		HttpTunnelServerHandler handler = new HttpTunnelServerHandler(server);
		ServerHttpRequest request = mock(ServerHttpRequest.class);
		ServerHttpResponse response = mock(ServerHttpResponse.class);
		handler.handle(request, response);
		verify(server).handle(request, response);
	}

}
