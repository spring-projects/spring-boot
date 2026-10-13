/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.io;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.io.ApplicationResourceLoader.FilePathResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * {@link FilePathResolver} for {@link ClassPathResource}.
 *
 * @author Phillip Webb
 */
class ClassPathResourceFilePathResolver implements ApplicationResourceLoader.FilePathResolver {

	@Override
	public @Nullable String resolveFilePath(String location, Resource resource) {
		return (resource instanceof ClassPathResource && !isClassPathUrl(location)) ? location : null;
	}

	private boolean isClassPathUrl(String location) {
		return location.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX);
	}

}
