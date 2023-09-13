/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpMessageConvertersRestClientCustomizer}
 *
 * @author Phillip Webb
 */
class HttpMessageConvertersRestClientCustomizerTests {

	@Test
	void createWhenNullMessageConvertersArrayThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new HttpMessageConvertersRestClientCustomizer((HttpMessageConverter<?>[]) null))
			.withMessage("MessageConverters must not be null");
	}

	@Test
	void createWhenNullMessageConvertersDoesNotCustomize() {
		HttpMessageConverter<?> c0 = mock();
		assertThat(apply(new HttpMessageConvertersRestClientCustomizer((HttpMessageConverters) null), c0))
			.containsExactly(c0);
	}

	@Test
	void customizeConfiguresMessageConverters() {
		HttpMessageConverter<?> c0 = mock();
		HttpMessageConverter<?> c1 = mock();
		HttpMessageConverter<?> c2 = mock();
		assertThat(apply(new HttpMessageConvertersRestClientCustomizer(c1, c2), c0)).containsExactly(c1, c2);
	}

	@SuppressWarnings("unchecked")
	private List<HttpMessageConverter<?>> apply(HttpMessageConvertersRestClientCustomizer customizer,
			HttpMessageConverter<?>... converters) {
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>(Arrays.asList(converters));
		RestClient.Builder restClientBuilder = mock();
		ArgumentCaptor<Consumer<List<HttpMessageConverter<?>>>> captor = ArgumentCaptor.forClass(Consumer.class);
		given(restClientBuilder.messageConverters(captor.capture())).willReturn(restClientBuilder);
		customizer.customize(restClientBuilder);
		captor.getValue().accept(messageConverters);
		return messageConverters;
	}

}
