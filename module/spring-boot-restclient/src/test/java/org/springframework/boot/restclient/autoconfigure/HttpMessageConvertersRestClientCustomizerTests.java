/*
 * Copyright 2012-present the original author or authors.
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
