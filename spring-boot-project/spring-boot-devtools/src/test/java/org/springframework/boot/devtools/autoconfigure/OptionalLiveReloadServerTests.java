/*
 * Copyright 2012-2022 the original author or authors.
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
