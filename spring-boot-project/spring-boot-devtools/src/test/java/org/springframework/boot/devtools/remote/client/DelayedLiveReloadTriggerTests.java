/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.devtools.remote.client;

import java.io.IOException;
import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.devtools.autoconfigure.OptionalLiveReloadServer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DelayedLiveReloadTrigger}.
 *
 * @author Phillip Webb
 */
public class DelayedLiveReloadTriggerTests {

	private static final String URL = "http://localhost:8080";

	@Mock
	private OptionalLiveReloadServer liveReloadServer;

	@Mock
	private ClientHttpRequestFactory requestFactory;

	@Mock
	private ClientHttpRequest errorRequest;

	@Mock
	private ClientHttpRequest okRequest;

	@Mock
	private ClientHttpResponse errorResponse;

	@Mock
	private ClientHttpResponse okResponse;

	private DelayedLiveReloadTrigger trigger;

	@Before
	public void setup() throws IOException {
		MockitoAnnotations.initMocks(this);
		given(this.errorRequest.execute()).willReturn(this.errorResponse);
		given(this.okRequest.execute()).willReturn(this.okResponse);
		given(this.errorResponse.getStatusCode())
				.willReturn(HttpStatus.INTERNAL_SERVER_ERROR);
		given(this.okResponse.getStatusCode()).willReturn(HttpStatus.OK);
		this.trigger = new DelayedLiveReloadTrigger(this.liveReloadServer,
				this.requestFactory, URL);
	}

	@Test
	public void liveReloadServerMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new DelayedLiveReloadTrigger(null, this.requestFactory, URL))
				.withMessageContaining("LiveReloadServer must not be null");
	}

	@Test
	public void requestFactoryMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(
				() -> new DelayedLiveReloadTrigger(this.liveReloadServer, null, URL))
				.withMessageContaining("RequestFactory must not be null");
	}

	@Test
	public void urlMustNotBeNull() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DelayedLiveReloadTrigger(this.liveReloadServer,
						this.requestFactory, null))
				.withMessageContaining("URL must not be empty");
	}

	@Test
	public void urlMustNotBeEmpty() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new DelayedLiveReloadTrigger(this.liveReloadServer,
						this.requestFactory, ""))
				.withMessageContaining("URL must not be empty");
	}

	@Test
	public void triggerReloadOnStatus() throws Exception {
		given(this.requestFactory.createRequest(new URI(URL), HttpMethod.GET))
				.willThrow(new IOException())
				.willReturn(this.errorRequest, this.okRequest);
		long startTime = System.currentTimeMillis();
		this.trigger.setTimings(10, 200, 30000);
		this.trigger.run();
		assertThat(System.currentTimeMillis() - startTime).isGreaterThan(300L);
		verify(this.liveReloadServer).triggerReload();
	}

	@Test
	public void timeout() throws Exception {
		given(this.requestFactory.createRequest(new URI(URL), HttpMethod.GET))
				.willThrow(new IOException());
		this.trigger.setTimings(10, 0, 10);
		this.trigger.run();
		verify(this.liveReloadServer, never()).triggerReload();
	}

}
