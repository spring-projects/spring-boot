/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.context.servlet;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.boot.io.ApplicationResourceLoader.FilePathResolver;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.support.ServletContextResource;

/**
 * {@link FilePathResolver} for {@link ServletContextResource}.
 *
 * @author Phillip Webb
 */
class ServletContextResourceFilePathResolver implements ApplicationResourceLoader.FilePathResolver {

	private static final String RESOURCE_CLASS_NAME = "org.springframework.web.context.support.ServletContextResource";

	private final @Nullable Class<?> resourceClass;

	ServletContextResourceFilePathResolver() {
		ClassLoader classLoader = getClass().getClassLoader();
		this.resourceClass = ClassUtils.isPresent(RESOURCE_CLASS_NAME, classLoader)
				? ClassUtils.resolveClassName(RESOURCE_CLASS_NAME, classLoader) : null;
	}

	@Override
	public @Nullable String resolveFilePath(String location, Resource resource) {
		return (this.resourceClass != null && this.resourceClass.isInstance(resource)) ? location : null;
	}

}
