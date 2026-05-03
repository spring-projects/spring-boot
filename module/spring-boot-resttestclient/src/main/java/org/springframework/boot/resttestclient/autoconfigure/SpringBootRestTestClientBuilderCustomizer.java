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

package org.springframework.boot.resttestclient.autoconfigure;

import java.util.Collection;

import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * {@link RestTestClientBuilderCustomizer} for a typical Spring Boot application. Usually
 * applied automatically via
 * {@link AutoConfigureRestTestClient @AutoConfigureRestTestClient}, but may also be used
 * directly.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class SpringBootRestTestClientBuilderCustomizer implements RestTestClientBuilderCustomizer {

	private final Collection<ClientHttpMessageConvertersCustomizer> messageConverterCustomizers;

	/**
	 * Create a new {@code SpringBootRestTestClientBuilderCustomizer} that will configure
	 * the builder's message converters using the given
	 * {@code messageConverterCustomizers}.
	 * @param messageConverterCustomizers the message converter customizers
	 */
	public SpringBootRestTestClientBuilderCustomizer(
			Collection<ClientHttpMessageConvertersCustomizer> messageConverterCustomizers) {
		this.messageConverterCustomizers = messageConverterCustomizers;

	}

	@Override
	public void customize(RestTestClient.Builder<?> builder) {
		if (this.messageConverterCustomizers.isEmpty()) {
			return;
		}
		builder.configureMessageConverters((configurer) -> this.messageConverterCustomizers
			.forEach((customizer) -> customizer.customize(configurer)));
	}

}
