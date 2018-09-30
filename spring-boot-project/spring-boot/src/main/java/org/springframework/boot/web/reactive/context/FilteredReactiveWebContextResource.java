/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.reactive.context;

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
		throw new FileNotFoundException(
				this.getDescription() + " cannot be opened because it does not exist");
	}

}
