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

package org.springframework.boot.autoconfigure.web.reactive.function.client;

import java.util.List;

import org.springframework.boot.web.codec.CodecCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link WebClientCustomizer} that configures codecs for the HTTP client.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class WebClientCodecCustomizer implements WebClientCustomizer {

	private final List<CodecCustomizer> codecCustomizers;

	public WebClientCodecCustomizer(List<CodecCustomizer> codecCustomizers) {
		this.codecCustomizers = codecCustomizers;
	}

	@Override
	public void customize(WebClient.Builder webClientBuilder) {
		webClientBuilder
			.codecs((codecs) -> this.codecCustomizers.forEach((customizer) -> customizer.customize(codecs)));
	}

}
