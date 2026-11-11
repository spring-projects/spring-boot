/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.context.reactive;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Resource implementation that replaces the
 * {@link org.springframework.web.context.support.ServletContextResource} in a reactive
 * web application.
 * <p>
 * {@link #exists()} always returns {@code false} in order to avoid exposing the whole
 * classpath in a non-servlet environment.
 *
 * @author Brian Clozel
 */
class FilteredReactiveWebContextResource extends AbstractResource {

	private final String path;

	FilteredReactiveWebContextResource(String path) {
		this.path = path;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public Resource createRelative(String relativePath) throws IOException {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new FilteredReactiveWebContextResource(pathToUse);
	}

	@Override
	public String getDescription() {
		return "ReactiveWebContext resource [" + this.path + "]";
	}

	@Override
	public InputStream getInputStream() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be opened because it does not exist");
	}

}
