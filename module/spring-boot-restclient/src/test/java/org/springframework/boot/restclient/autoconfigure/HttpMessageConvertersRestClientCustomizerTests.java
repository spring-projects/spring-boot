/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.restclient.autoconfigure;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpMessageConvertersRestClientCustomizer}
 *
 * @author Brian Clozel
 */
class HttpMessageConvertersRestClientCustomizerTests {

	@Test
	void customizeConfiguresMessageConverters() {
		HttpMessageConverter<?> c0 = mock();
		HttpMessageConverter<?> c1 = mock();
		ClientHttpMessageConvertersCustomizer customizer = (clientBuilder) -> clientBuilder.addCustomConverter(c0)
			.addCustomConverter(c1);

		RestClient.Builder builder = RestClient.builder();
		new HttpMessageConvertersRestClientCustomizer(customizer).customize(builder);
		assertThat(builder.build()).extracting("messageConverters")
			.asInstanceOf(InstanceOfAssertFactories.LIST)
			.containsSubsequence(c0, c1);
	}

}
