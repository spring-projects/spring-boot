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

package org.springframework.boot.autoconfigure.web.reactive.error;

import org.junit.jupiter.api.Test;

import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractErrorWebExceptionHandler}.
 *
 * @author Phillip Webb
 */
class DefaultErrorWebExceptionHandlerTests {

	@Test
	void disconnectedClientExceptionsMatchesFramework() {
		Object errorHandlers = ReflectionTestUtils.getField(AbstractErrorWebExceptionHandler.class,
				"DISCONNECTED_CLIENT_EXCEPTIONS");
		Object webHandlers = ReflectionTestUtils.getField(HttpWebHandlerAdapter.class,
				"DISCONNECTED_CLIENT_EXCEPTIONS");
		assertThat(errorHandlers).isNotNull().isEqualTo(webHandlers);
	}

}
