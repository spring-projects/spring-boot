/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.converter.autoconfigure;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for HTTP message conversion.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@ConfigurationProperties("spring.http.converters")
public class HttpMessageConvertersProperties {

	/**
	 * Charset to use for String conversion.
	 */
	private Charset stringEncodingCharset = StandardCharsets.UTF_8;

	public Charset getStringEncodingCharset() {
		return this.stringEncodingCharset;
	}

	public void setStringEncodingCharset(Charset stringEncodingCharset) {
		this.stringEncodingCharset = stringEncodingCharset;
	}

}
