/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.context.reactive;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.boot.io.ApplicationResourceLoader.FilePathResolver;
import org.springframework.core.io.Resource;

/**
 * {@link FilePathResolver} for {@link FilteredReactiveWebContextResource}.
 *
 * @author Dmytro Nosan
 */
class FilteredReactiveWebContextResourceFilePathResolver implements ApplicationResourceLoader.FilePathResolver {

	@Override
	public @Nullable String resolveFilePath(String location, Resource resource) {
		return (resource instanceof FilteredReactiveWebContextResource) ? location : null;
	}

}
