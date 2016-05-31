/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.web.client;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link BasicAuthorizationInterceptor}.
 *
 * @author Phillip Webb
 */
public class BasicAuthorizationInterceptorTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenUsernameIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Username must not be empty");
		new BasicAuthorizationInterceptor(null, "password");
	}

	@Test
	public void createWhenUsernameIsEmptyShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Username must not be empty");
		new BasicAuthorizationInterceptor("", "password");
	}

	@Test
	public void createWhenPasswordIsNullShouldUseEmptyPassword() throws Exception {
		BasicAuthorizationInterceptor interceptor = new BasicAuthorizationInterceptor(
				"username", "");
		assertThat(interceptor).extracting("password").containsExactly("");
	}

	@Test
	public void interceptShouldAddHeader() throws Exception {
		MockClientHttpRequest request = new MockClientHttpRequest();
		ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
		byte[] body = new byte[] {};
		new BasicAuthorizationInterceptor("spring", "boot").intercept(request, body,
				execution);
		verify(execution).execute(request, body);
		assertThat(request.getHeaders().getFirst("Authorization"))
				.isEqualTo("Basic c3ByaW5nOmJvb3Q=");
	}

}
