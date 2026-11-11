/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.restart.server;

import org.junit.jupiter.api.Test;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpRestartServerHandler}.
 *
 * @author Phillip Webb
 */
class HttpRestartServerHandlerTests {

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void serverMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new HttpRestartServerHandler(null))
			.withMessageContaining("'server' must not be null");
	}

	@Test
	void handleDelegatesToServer() throws Exception {
		HttpRestartServer server = mock(HttpRestartServer.class);
		HttpRestartServerHandler handler = new HttpRestartServerHandler(server);
		ServerHttpRequest request = mock(ServerHttpRequest.class);
		ServerHttpResponse response = mock(ServerHttpResponse.class);
		handler.handle(request, response);
		then(server).should().handle(request, response);
	}

}
