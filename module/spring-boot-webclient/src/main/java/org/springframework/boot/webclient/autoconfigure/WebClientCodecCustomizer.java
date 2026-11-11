/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient.autoconfigure;

import java.util.List;

import org.springframework.boot.http.codec.CodecCustomizer;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link WebClientCustomizer} that configures codecs for the HTTP client.
 *
 * @author Brian Clozel
 * @since 4.0.0
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
