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

import org.junit.Test;

import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebServiceTemplateBuilder}.
 *
 * @author Dmytro Nosan
 */
public class WebServiceTemplateBuilderSimpleClientHttpRequestFactoryTests {

	private WebServiceTemplateBuilder builder = new WebServiceTemplateBuilder();

	@Test
	public void setTimeout() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		ClientHttpRequestMessageSender sender = new ClientHttpRequestMessageSender(
				new BufferingClientHttpRequestFactory(factory));

		this.builder.setConnectionTimeout(5000).setReadTimeout(2000)
				.setWebServiceMessageSender(() -> sender).build();

		assertThat(ReflectionTestUtils.getField(factory, "connectTimeout"))
				.isEqualTo(5000);
		assertThat(ReflectionTestUtils.getField(factory, "readTimeout")).isEqualTo(2000);

	}

}
