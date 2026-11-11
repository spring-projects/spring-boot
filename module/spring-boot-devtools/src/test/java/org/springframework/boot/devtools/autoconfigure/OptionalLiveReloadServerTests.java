/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.devtools.livereload.LiveReloadServer;

import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link OptionalLiveReloadServer}.
 *
 * @author Phillip Webb
 */
@SuppressWarnings("removal")
class OptionalLiveReloadServerTests {

	@Test
	void nullServer() {
		OptionalLiveReloadServer server = new OptionalLiveReloadServer(null);
		server.startServer();
		server.triggerReload();
	}

	@Test
	void serverWontStart() throws Exception {
		LiveReloadServer delegate = mock(LiveReloadServer.class);
		OptionalLiveReloadServer server = new OptionalLiveReloadServer(delegate);
		willThrow(new RuntimeException("Error")).given(delegate).start();
		server.startServer();
		server.triggerReload();
		then(delegate).should(never()).triggerReload();
	}

}
