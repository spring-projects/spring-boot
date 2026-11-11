/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.codec;

import org.springframework.http.codec.CodecConfigurer;

/**
 * Callback interface that can be used to customize codecs configuration for an HTTP
 * client and/or server with a {@link CodecConfigurer}.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
@FunctionalInterface
public interface CodecCustomizer {

	/**
	 * Callback to customize a {@link CodecConfigurer} instance.
	 * @param configurer codec configurer to customize
	 */
	void customize(CodecConfigurer configurer);

}
