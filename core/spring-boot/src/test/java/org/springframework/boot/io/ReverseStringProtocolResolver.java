/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.io;

import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Test {@link ProtocolResolver} that reverses a String.
 *
 * @author Phillip Webb
 */
class ReverseStringProtocolResolver implements ProtocolResolver {

	private static final String PREFIX = "reverse:";

	@Override
	public @Nullable Resource resolve(String location, ResourceLoader resourceLoader) {
		if (!location.startsWith(PREFIX)) {
			return null;
		}
		return new ByteArrayResource(reverse(location.substring(PREFIX.length())));
	}

	private byte[] reverse(String substring) {
		return new StringBuilder(substring).reverse().toString().getBytes(StandardCharsets.UTF_8);
	}

}
