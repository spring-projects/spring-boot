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

import org.apache.http.client.HttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.junit.Test;

import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for
 * {@link org.springframework.boot.webservices.client.WebServiceTemplateBuilder}. This
 * test class check that builder will create HttpComponents by default if apache client is
 * present in the classpath.
 *
 * @author Dmytro Nosan
 */
@SuppressWarnings("deprecation")
public class WebServiceTemplateBuilderHttpComponentsMessageSenderTests {

	private WebServiceTemplateBuilder builder = new WebServiceTemplateBuilder();

	@Test
	public void build() {
		WebServiceTemplate webServiceTemplate = new WebServiceTemplateBuilder().build();

		assertThat(webServiceTemplate.getMessageSenders()).hasSize(1);

		assertThat(webServiceTemplate.getMessageSenders()[0])
				.isInstanceOf(HttpComponentsMessageSender.class);

	}

	@Test
	public void setTimeout() {
		HttpComponentsMessageSender sender = new HttpComponentsMessageSender();
		HttpClient httpClient = sender.getHttpClient();

		this.builder.setConnectionTimeout(5000).setReadTimeout(2000)
				.setWebServiceMessageSender(() -> sender).build();

		assertThat(HttpConnectionParams.getConnectionTimeout(httpClient.getParams()))
				.isEqualTo(5000);
		assertThat(HttpConnectionParams.getSoTimeout(httpClient.getParams()))
				.isEqualTo(2000);

	}

}
