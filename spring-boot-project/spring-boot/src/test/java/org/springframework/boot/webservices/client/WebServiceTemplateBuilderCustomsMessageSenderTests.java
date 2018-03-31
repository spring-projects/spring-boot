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

package org.springframework.boot.webservices.client;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;

/**
 * Tests for
 * {@link org.springframework.boot.webservices.client.WebServiceTemplateBuilder}.
 *
 * @author Dmytro Nosan
 */
public class WebServiceTemplateBuilderCustomsMessageSenderTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private WebServiceTemplateBuilder builder = new WebServiceTemplateBuilder();

	@Test
	public void unknownSenderReadTimeout() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("with 'readTimeout'. Please use a custom customizer.");
		this.thrown.expectMessage("There is no way to customize");

		this.builder.setReadTimeout(3000).setWebServiceMessageSender(
				() -> Mockito.mock(WebServiceMessageSender.class)).build();
	}

	@Test
	public void unknownSenderConnectionTimeout() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage(
				"with 'connectionTimeout'. Please use a custom customizer.");
		this.thrown.expectMessage("There is no way to customize");

		this.builder.setConnectionTimeout(3000).setWebServiceMessageSender(
				() -> Mockito.mock(WebServiceMessageSender.class)).build();
	}

	@Test
	public void unknownRequestFactoryReadTimeout() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("with 'readTimeout'. Please use a custom customizer.");
		this.thrown.expectMessage("There is no way to customize");

		this.builder.setReadTimeout(3000)
				.setWebServiceMessageSender(() -> new ClientHttpRequestMessageSender(
						Mockito.mock(ClientHttpRequestFactory.class)))
				.build();
	}

	@Test
	public void unknownRequestFactoryConnectionTimeout() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage(
				"with 'connectionTimeout'. Please use a custom customizer.");
		this.thrown.expectMessage("There is no way to customize");

		this.builder.setConnectionTimeout(3000)
				.setWebServiceMessageSender(() -> new ClientHttpRequestMessageSender(
						Mockito.mock(ClientHttpRequestFactory.class)))
				.build();
	}

	@Test
	public void shouldBuildWithoutTimeouts() {
		this.builder.setWebServiceMessageSender(
				() -> Mockito.mock(WebServiceMessageSender.class)).build();
	}

}
