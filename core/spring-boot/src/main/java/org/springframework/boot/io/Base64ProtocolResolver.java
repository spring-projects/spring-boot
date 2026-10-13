/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.io;

import java.util.Base64;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * {@link ProtocolResolver} for resources containing base 64 encoded text.
 *
 * @author Scott Frederick
 */
class Base64ProtocolResolver implements ProtocolResolver {

	private static final String BASE64_PREFIX = "base64:";

	@Override
	public @Nullable Resource resolve(String location, ResourceLoader resourceLoader) {
		if (location.startsWith(BASE64_PREFIX)) {
			String value = location.substring(BASE64_PREFIX.length());
			return new ByteArrayResource(decode(value));
		}
		return null;
	}

	private static byte[] decode(String location) {
		return Base64.getDecoder().decode(location);
	}

}
