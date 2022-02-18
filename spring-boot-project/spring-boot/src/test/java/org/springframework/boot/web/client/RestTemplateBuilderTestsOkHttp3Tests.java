/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.client;

import java.time.Duration;

import okhttp3.OkHttpClient;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.boot.testsupport.classpath.ClassPathOverrides;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link RestTemplateBuilder} with OkHttp 3.x.
 *
 * @author Andy Wilkinson
 */
@ClassPathOverrides("com.squareup.okhttp3:okhttp:3.14.9")
class RestTemplateBuilderTestsOkHttp3Tests {

	private RestTemplateBuilder builder = new RestTemplateBuilder();

	@Test
	void connectTimeoutCanBeConfiguredOnOkHttpRequestFactory() {
		ClientHttpRequestFactory requestFactory = this.builder.requestFactory(OkHttp3ClientHttpRequestFactory.class)
				.setConnectTimeout(Duration.ofMillis(1234)).build().getRequestFactory();
		assertThat(requestFactory).extracting("client", InstanceOfAssertFactories.type(OkHttpClient.class))
				.extracting(OkHttpClient::connectTimeoutMillis).isEqualTo(1234);
	}

	@Test
	void readTimeoutCanBeConfiguredOnOkHttpRequestFactory() {
		ClientHttpRequestFactory requestFactory = this.builder.requestFactory(OkHttp3ClientHttpRequestFactory.class)
				.setReadTimeout(Duration.ofMillis(1234)).build().getRequestFactory();
		assertThat(requestFactory).extracting("client", InstanceOfAssertFactories.type(OkHttpClient.class))
				.extracting(OkHttpClient::readTimeoutMillis).isEqualTo(1234);
	}

	@Test
	void bufferRequestBodyCanNotBeConfiguredOnOkHttpRequestFactory() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.builder.requestFactory(OkHttp3ClientHttpRequestFactory.class)
						.setBufferRequestBody(false).build().getRequestFactory())
				.withMessageContaining(OkHttp3ClientHttpRequestFactory.class.getName());
	}

}
