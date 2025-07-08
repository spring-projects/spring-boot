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

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestClientBuilderConfigurer}.
 *
 * @author Moritz Halbritter
 */
@ExtendWith(MockitoExtension.class)
class RestClientBuilderConfigurerTests {

	@Mock
	private ClientHttpRequestFactoryBuilder<ClientHttpRequestFactory> clientHttpRequestFactoryBuilder;

	@Mock
	private ClientHttpRequestFactory clientHttpRequestFactory;

	@Test
	void shouldConfigureRestClientBuilder() {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.ofSslBundle(mock(SslBundle.class));
		RestClientCustomizer customizer = mock(RestClientCustomizer.class);
		RestClientCustomizer customizer1 = mock(RestClientCustomizer.class);
		RestClientBuilderConfigurer configurer = new RestClientBuilderConfigurer(this.clientHttpRequestFactoryBuilder,
				settings, List.of(customizer, customizer1));
		given(this.clientHttpRequestFactoryBuilder.build(settings)).willReturn(this.clientHttpRequestFactory);

		RestClient.Builder builder = RestClient.builder();
		configurer.configure(builder);
		assertThat(builder.build()).hasFieldOrPropertyWithValue("clientRequestFactory", this.clientHttpRequestFactory);
		then(customizer).should().customize(builder);
		then(customizer1).should().customize(builder);
	}

}
